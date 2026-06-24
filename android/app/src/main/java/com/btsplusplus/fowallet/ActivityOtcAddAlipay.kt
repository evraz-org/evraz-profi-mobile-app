package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import bitshares.OtcManager
import bitshares.Promise
import com.btsplusplus.fowallet.databinding.ActivityOtcAddAlipayBinding
import org.json.JSONObject

class ActivityOtcAddAlipay : BtsppActivity() {

    private lateinit var binding: ActivityOtcAddAlipayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  设置自动布局
        setAutoLayoutContentView(R.layout.activity_otc_add_alipay)
        binding = ActivityOtcAddAlipayBinding.bind(findViewById<View>(android.R.id.content).rootView)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val auth_info = args.getJSONObject("auth_info")
        val result_promise = args.opt("result_promise") as? Promise

        //  初始化值
        val name = auth_info.optString("realName")
        if (name.isNotEmpty()) {
            binding.tfRealname.setText(name)
            binding.tfRealname.isEnabled = false
        }

        // 返回
        binding.layoutBackFromOtcAddApipay.setOnClickListener { finish() }

        //  提交
        binding.tvSubmitFromOtcAddAlipay.setOnClickListener { onSubmit(result_promise) }

    }

    private fun onSubmit(result_promise: Promise?) {
        val str_realname = binding.tfRealname.text.toString()
        val str_account = binding.tfAccount.text.toString()

        if (str_realname == "") {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputRealname))
            return
        }

        if (str_account == "") {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputValidAccount))
            return
        }

        guardWalletUnlocked(true) { unlocked ->
            if (unlocked) {
                val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this)
                mask.show()
                val otc = OtcManager.sharedOtcManager()
                val args = JSONObject().apply {
                    put("account", str_account)
                    put("btsAccount", otc.getCurrentBtsAccount())
                    put("qrCode", "")           //  for alipay & wechat pay TODO:3.0 暂时不支持二维码
                    put("realName", str_realname)
                    put("remark", "")           //  for bank card
                    put("reservePhone", "")     //  for bank card
                    put("type", OtcManager.EOtcPaymentMethodType.eopmt_alipay.value)
                }
                otc.addPaymentMethods(args).then {
                    mask.dismiss()
                    showToast(resources.getString(R.string.kOtcRmSubmitTipsOK))
                    //  返回上一个界面并刷新
                    result_promise?.resolve(true)
                    finish()
                    return@then null
                }.catch { err ->
                    mask.dismiss()
                    otc.showOtcError(this, err)
                }
            }
        }
    }
}
