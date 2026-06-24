package com.btsplusplus.fowallet

import android.Manifest
import android.os.Bundle
import android.view.View
import bitshares.*
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import com.btsplusplus.fowallet.databinding.ActivityIndexServicesBinding
import org.json.JSONArray
import org.json.JSONObject

class ActivityIndexServices : BtsppActivity() {

    private lateinit var binding: ActivityIndexServicesBinding

    /**
     * 重载 - 返回键按下
     */
    override fun onBackPressed() {
        goHome()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLayoutContentView(R.layout.activity_index_services, navigationBarColor = R.color.theme01_tabBarColor)
        binding = ActivityIndexServicesBinding.bind(findViewById<View>(android.R.id.content).rootView)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        // 设置底部导航栏样式
        setBottomNavigationStyle(2)

        //  设置模块可见性
        if (ChainObjectManager.sharedChainObjectManager().getMainSmartAssetList().length() > 0) {
            binding.layoutSmartCoin.visibility = View.VISIBLE
        } else {
            binding.layoutSmartCoin.visibility = View.GONE
        }

        //  入口可见性判断
        //  1 - 编译时宏判断
        //  2 - 根据语言判断
        //  3 - 根据服务器配置判断
        if (BuildConfig.kAppModuleEnableOTC && resources.getString(R.string.enableOtcEntry).toInt() != 0) {
            var hidden_layout = 0
            val cfg = OtcManager.sharedOtcManager().server_config
            if (cfg != null && cfg.getJSONObject("user").getJSONObject("entry").getInt("type") != OtcManager.EOtcEntryType.eoet_gone.value) {
                binding.layoutOtcUser.visibility = View.VISIBLE
                binding.layoutOtcUser.setOnClickListener { onOtcUsrEntryClicked() }
            } else {
                binding.layoutOtcUser.visibility = View.GONE
                hidden_layout += 1
            }
            if (cfg != null && cfg.getJSONObject("merchant").getJSONObject("entry").getInt("type") != OtcManager.EOtcEntryType.eoet_gone.value) {
                binding.layoutOtcMerchant.visibility = View.VISIBLE
                binding.layoutOtcMerchant.setOnClickListener { onOtcMerchantEntryClicked() }
            } else {
                binding.layoutOtcMerchant.visibility = View.GONE
                hidden_layout += 1
            }
            //  直接整个OTC组不可见
            if (hidden_layout >= 2) {
                binding.layoutGroupOtc.visibility = View.GONE
            }
        } else {
            //  直接整个OTC组不可见
            binding.layoutGroupOtc.visibility = View.GONE
        }

        if (BuildConfig.kAppModuleEnableGateway) {
            binding.layoutRechargeAndWithdrawOfService.visibility = View.VISIBLE
        } else {
            binding.layoutRechargeAndWithdrawOfService.visibility = View.GONE
        }

        //  设置图标颜色
        val iconcolor = resources.getColor(R.color.theme01_textColorNormal)
        binding.imgIconTransfer.setColorFilter(iconcolor)
        binding.imgIconQrscan.setColorFilter(iconcolor)
        binding.imgIconAccountSearch.setColorFilter(iconcolor)
        binding.imgIconSmartCoin.setColorFilter(iconcolor)
        binding.imgIconVoting.setColorFilter(iconcolor)
        binding.imgIconDepositWithdraw.setColorFilter(iconcolor)
        binding.imgIconOtcUser.setColorFilter(iconcolor)
        binding.imgIconOtcMerchant.setColorFilter(iconcolor)
        binding.imgIconAdvfunction.setColorFilter(iconcolor)
        binding.imgIconExplorer.setColorFilter(iconcolor)
        binding.imgIconGame.setColorFilter(iconcolor)

        binding.layoutAccountQueryFromServices.setOnClickListener {
            TempManager.sharedTempManager().set_query_account_callback { last_activity, it ->
                last_activity.goTo(ActivityIndexServices::class.java, true, back = true)
                viewUserAssets(it.getString("name"))
            }
            goTo(ActivityAccountQueryBase::class.java, true)
        }

        if (ChainObjectManager.sharedChainObjectManager().getMainSmartAssetList().length() > 0) {
            binding.layoutSmartCoin.setOnClickListener {
                goTo(ActivityAssetInfos::class.java, true)
            }
        }

        binding.layoutTransferFromServices.setOnClickListener {
            guardWalletExist {
                val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
                mask.show()
                val p1 = get_full_account_data_and_asset_hash(WalletManager.sharedWalletManager().getWalletAccountName()!!)
                val p2 = ChainObjectManager.sharedChainObjectManager().queryFeeAssetListDynamicInfo()
                Promise.all(p1, p2).then {
                    mask.dismiss()
                    val data_array = it as JSONArray
                    val full_userdata = data_array.getJSONObject(0)
                    goTo(ActivityTransfer::class.java, true, args = jsonObjectfromKVS("full_account_data", full_userdata))
                    return@then null
                }.catch {
                    mask.dismiss()
                    showToast(resources.getString(R.string.tip_network_error))
                }
            }
        }

        binding.layoutVotingFromServices.setOnClickListener {
            guardWalletExist { goTo(ActivityVoting::class.java, true) }
        }

        binding.layoutSaoyisaoFromServices.setOnClickListener {
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

        if (BuildConfig.kAppModuleEnableGateway) {
            binding.layoutRechargeAndWithdrawOfService.setOnClickListener {
                guardWalletExist { goTo(ActivityDepositAndWithdraw::class.java, true) }
            }
        }

        binding.layoutAdvancedFeatureOfService.setOnClickListener {
            goTo(ActivityAdvancedFeature::class.java, true)
        }

        binding.layoutBtsExplorer.setOnClickListener {
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
