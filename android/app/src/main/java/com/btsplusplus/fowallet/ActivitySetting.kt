package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivitySettingBinding
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONObject

class ActivitySetting : BtsppActivity() {

    private lateinit var _result_promise: Promise
    private lateinit var _binding: ActivitySettingBinding

    override fun onResume() {
        super.onResume()
        //  初始化默认值
        _refreshUI()
    }

    override fun onBackClicked(result: Any?) {
        _result_promise.resolve(true)
        super.onBackClicked(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivitySettingBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)

        //  获取参数 / get params
        val args = btspp_args_as_JSONObject()
        _result_promise = args.get("result_promise") as Promise

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  事件 - 返回
        _binding.layoutBackFromSetting.setOnClickListener { onBackClicked(false) }

        //  事件 - 多语言
        _binding.layoutLanguageFromSetting.setOnClickListener {
            val saveCurrLangCode = LangManager.sharedLangManager().currLangCode
            val result_promise = Promise()
            goTo(ActivitySettingLanguage::class.java, true, args = jsonObjectfromKVS("result_promise", result_promise))
            result_promise.then {
                if (LangManager.sharedLangManager().currLangCode != saveCurrLangCode) {
                    //  reset arguments
                    recreate()
                }
            }
        }

        //  事件 - 计价单位
        _binding.layoutCurrencyFromSetting.setOnClickListener { goTo(ActivitySettingCurrency::class.java, true) }

        //  事件 - 横板交易界面
        _binding.btnEnableHorUi.setOnCheckedChangeListener { _, isChecked ->
            SettingManager.sharedSettingManager().setUseConfigBoolean(kSettingKey_EnableHorTradeUI, isChecked)
        }

        //  事件 - API节点
        _binding.layoutApinode.setOnClickListener { goTo(ActivitySelectApiNode::class.java, true) }

        //  事件 - 版本
        _binding.layoutVersion.setOnClickListener { onVersionCellClicked() }

        //  事件 - 关于
        _binding.layoutAbout.setOnClickListener { goTo(ActivityAbout::class.java, true) }
    }

    /**
     *  事件 - 当前版本点击
     */
    private fun onVersionCellClicked() {
        val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
        ActivityLaunch.checkAppUpdate().then {
            mask.dismiss()
            val pVersionConfig = it as? JSONObject
            if (VcUtils.processCheckAppVersionResponsed(this, pVersionConfig, null)) {
                //  ...
            } else {
                showToast(resources.getString(R.string.kSettingVersionTipsNewest))
            }
            return@then null
        }
    }

    private fun _refreshUI() {
        _refresh_language()
        _refresh_currency()
        _binding.btnEnableHorUi.isChecked = SettingManager.sharedSettingManager().isEnableHorTradeUI()
        _refresh_apinode()
        _refresh_version()
    }

    /**
     * 显示当前语言
     */
    private fun _refresh_language() {
        _binding.labelTxtLanguage.text = LangManager.sharedLangManager().getCurrentLanguageName(this)
    }

    /**
     * 显示当前计价方式
     */
    private fun _refresh_currency() {
        val assetSymbol = SettingManager.sharedSettingManager().getEstimateAssetSymbol()
        val currency = ChainObjectManager.sharedChainObjectManager().getEstimateUnitBySymbol(assetSymbol)
        _binding.labelTxtCurrency.text = resources.getString(resources.getIdentifier(currency.getString("namekey"), "string", this.packageName))
    }

    /**
     * 显示当前API节点
     */
    private fun _refresh_apinode() {
        val user_config = SettingManager.sharedSettingManager().getUseConfig(kSettingKey_ApiNode) as? JSONObject
        val current_node = user_config?.optJSONObject(kSettingKey_ApiNode_Current)
        if (current_node != null) {
            val namekey = current_node.optString("namekey", "")
            if (namekey.isNotEmpty()) {
                _binding.labelTxtApinode.text = resources.getString(resources.getIdentifier(namekey, "string", packageName))
            } else {
                _binding.labelTxtApinode.text = current_node.optString("location", null) ?: current_node.optString("name")
            }
        } else {
            _binding.labelTxtApinode.text = resources.getString(R.string.kSettingApiCellValueRandom)
        }
    }

    /**
     * 显示当前版本
     */
    private fun _refresh_version() {
        _binding.labelTxtVersion.text = String.format("v%s", Utils.appVersionName())
    }
}
