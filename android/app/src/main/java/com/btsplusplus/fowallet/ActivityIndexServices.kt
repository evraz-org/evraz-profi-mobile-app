package com.btsplusplus.fowallet

import android.Manifest
import android.os.Bundle
import android.view.View
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityIndexServicesBinding
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONObject

class ActivityIndexServices : BtsppActivity() {

    private lateinit var _binding: ActivityIndexServicesBinding

    /**
     * 重载 - 返回键按下
     */
    override fun onBackPressed() {
        goHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityIndexServicesBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root, navigationBarColor = R.color.theme01_tabBarColor)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        // 设置底部导航栏样式
        setBottomNavigationStyle(_binding.bottomNav, 4)

        //  设置模块可见性
        if (ChainObjectManager.sharedChainObjectManager().getMainSmartAssetList().length() > 0) {
            _binding.layoutSmartCoin.visibility = View.VISIBLE
        } else {
            _binding.layoutSmartCoin.visibility = View.GONE
        }

        //  入口可见性判断
        //  1 - 编译时宏判断
        //  2 - 根据语言判断
        //  3 - 根据服务器配置判断
        if (BuildConfig.kAppModuleEnableOTC && resources.getString(R.string.enableOtcEntry).toInt() != 0) {
            var hidden_layout = 0
            val cfg = OtcManager.sharedOtcManager().server_config
            if (cfg != null && cfg.getJSONObject("user").getJSONObject("entry").getInt("type") != OtcManager.EOtcEntryType.eoet_gone.value) {
                _binding.layoutOtcUser.visibility = View.VISIBLE
                _binding.layoutOtcUser.setOnClickListener { onOtcUsrEntryClicked() }
            } else {
                _binding.layoutOtcUser.visibility = View.GONE
                hidden_layout += 1
            }
            if (cfg != null && cfg.getJSONObject("merchant").getJSONObject("entry").getInt("type") != OtcManager.EOtcEntryType.eoet_gone.value) {
                _binding.layoutOtcMerchant.visibility = View.VISIBLE
                _binding.layoutOtcMerchant.setOnClickListener { onOtcMerchantEntryClicked() }
            } else {
                _binding.layoutOtcMerchant.visibility = View.GONE
                hidden_layout += 1
            }
            //  直接整个OTC组不可见
            if (hidden_layout >= 2) {
                _binding.layoutGroupOtc.visibility = View.GONE
            }
        } else {
            //  直接整个OTC组不可见
            _binding.layoutGroupOtc.visibility = View.GONE
        }

        //  设置图标颜色
        val iconcolor = resources.getColor(R.color.theme01_textColorNormal)
        _binding.imgIconQrscan.setColorFilter(iconcolor)
        _binding.imgIconAccountSearch.setColorFilter(iconcolor)
        _binding.imgIconSmartCoin.setColorFilter(iconcolor)
        _binding.imgIconVoting.setColorFilter(iconcolor)
        _binding.imgIconOtcUser.setColorFilter(iconcolor)
        _binding.imgIconOtcMerchant.setColorFilter(iconcolor)
        _binding.imgIconAdvfunction.setColorFilter(iconcolor)
        _binding.imgIconExplorer.setColorFilter(iconcolor)
        _binding.imgIconGame.setColorFilter(iconcolor)

        _binding.layoutAccountQueryFromServices.setOnClickListener {
            TempManager.sharedTempManager().set_query_account_callback { last_activity, it ->
                last_activity.goTo(ActivityIndexServices::class.java, true, back = true)
                viewUserAssets(it.getString("name"))
            }
            goTo(ActivityAccountQueryBase::class.java, true)
        }

        if (ChainObjectManager.sharedChainObjectManager().getMainSmartAssetList().length() > 0) {
            _binding.layoutSmartCoin.setOnClickListener {
                goTo(ActivityAssetInfos::class.java, true)
            }
        }

        _binding.layoutVotingFromServices.setOnClickListener {
            guardWalletExist { goTo(ActivityVoting::class.java, true) }
        }

        _binding.layoutSaoyisaoFromServices.setOnClickListener {
            this.guardPermissions(Manifest.permission.CAMERA).then {
                when (it as Int) {
                    EBtsppPermissionResult.GRANTED.value -> {
                        goTo(ActivityQrScan::class.java, true, args = JSONObject())
                    }
                    EBtsppPermissionResult.SHOW_RATIONALE.value -> {
                        showToast(resources.getString(R.string.kVcScanPermissionUserRejected))
                    }
                    EBtsppPermissionResult.DONT_ASK_AGAIN.value -> {
                        showToast(resources.getString(R.string.kVcScanPermissionGotoSetting))
                    }
                }
                return@then null
            }
        }

        _binding.layoutAdvancedFeatureOfService.setOnClickListener {
            goTo(ActivityAdvancedFeature::class.java, true)
        }

        _binding.layoutBtsExplorer.setOnClickListener {
            //  TODO:插件配置url
            openURL("https://bts.ai?lang=${resources.getString(R.string.btsaiLangKey)}")
        }
    }

    /**
     *  (private) 进入场外交易界面
     */
    private fun _gotoOtcUserEntry() {
        guardWalletExist {
            //  TODO:2.9 默認參數
            OtcManager.sharedOtcManager().gotoOtc(this, "CNY", OtcManager.EOtcAdType.eoadt_user_buy)
        }
    }

    private fun onOtcUsrEntryClicked() {
        val cfg = OtcManager.sharedOtcManager().server_config!!
        val entry = cfg.getJSONObject("user").getJSONObject("entry")
        if (entry.getInt("type") == OtcManager.EOtcEntryType.eoet_enabled.value) {
            val otcUserAgreementKeyName = "kOtcUserAgreementApprovedVer"
            val approvedVer = AppCacheManager.sharedAppCacheManager().getPref(otcUserAgreementKeyName) as? String
            if (approvedVer != null && approvedVer.isNotEmpty()) {
                //  已同意 TODO:3.0 暂时不处理协议更新。
                _gotoOtcUserEntry()
            } else {
                //  未同意 弹出协议对话框
                val agreement_url = cfg.getJSONObject("urls").getString("agreement")
                val message = resources.getString(R.string.kOtcEntryUserAgreementAskMessage)
                val link = JSONObject().apply {
                    put("text", resources.getString(R.string.kOtcEntryUserAgreementLinkName))
                    put("url", String.format("%s?v=%s", agreement_url, Utils.now_ts().toString()))
                }
                UtilsAlert.showMessageConfirm(this, resources.getString(R.string.kOtcEntryUserAgreementAskTitle), message, btn_ok = resources.getString(R.string.kOtcEntryUserAgreementBtnOK), link = link).then {
                    if (it != null && it as Boolean) {
                        //  记录：同意协议
                        AppCacheManager.sharedAppCacheManager().setPref(otcUserAgreementKeyName, agreement_url).saveCacheToFile()
                        //  继续处理
                        _gotoOtcUserEntry()
                    }
                    return@then null
                }
            }
        } else {
            var msg = entry.optString("msg", null)
            if (msg == null || msg.isEmpty()) {
                msg = resources.getString(R.string.kOtcEntryDisableDefaultMsg)
            }
            showToast(msg)
        }
    }

    private fun onOtcMerchantEntryClicked() {
        val cfg = OtcManager.sharedOtcManager().server_config!!
        val entry = cfg.getJSONObject("merchant").getJSONObject("entry")
        if (entry.getInt("type") == OtcManager.EOtcEntryType.eoet_enabled.value) {
            guardWalletExist { OtcManager.sharedOtcManager().gotoOtcMerchantHome(this) }
        } else {
            var msg = entry.optString("msg", null)
            if (msg == null || msg.isEmpty()) {
                msg = resources.getString(R.string.kOtcEntryDisableDefaultMsg)
            }
            showToast(msg)
        }
    }
}