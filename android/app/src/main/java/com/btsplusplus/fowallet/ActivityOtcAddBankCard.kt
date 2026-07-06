package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.OtcManager
import bitshares.Promise
import org.json.JSONObject
import com.btsplusplus.fowallet.databinding.ActivityOtcAddBankCardBinding

class ActivityOtcAddBankCard : BtsppActivity() {

    private lateinit var _binding: ActivityOtcAddBankCardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityOtcAddBankCardBinding.inflate(layoutInflater)
        //  设置自动布局
        setAutoLayoutContentView(_binding.root)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val auth_info = args.getJSONObject("auth_info")
        val result_promise = args.opt("result_promise") as? Promise

        //  初始化值
        val name = auth_info.optString("realName")
        if (name.isNotEmpty()) {
            _binding.tfRealname.setText(name)
            _binding.tfRealname.isEnabled = false
        }

        //  事件
        _binding.layoutBackFromOtcAddBankcard.setOnClickListener { finish() }
        _binding.tvSubmitFromOtcAddBankcard.setOnClickListener { onSubmit(result_promise) }
    }

    private fun onSubmit(result_promise: Promise?) {
        val str_realname = _binding.tfRealname.text.toString()
        val str_bankno = _binding.tfBankcardNo.text.toString()
        val str_phoneno = _binding.tfReservePhone.text.toString()

        if (str_realname == "") {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputRealname))
            return
        }
        if (str_bankno == "") {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputBankcardNo))
            return
        }

        if (!OtcManager.checkIsValidPhoneNumber(str_phoneno)) {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputPhoneNo))
            return
        }

        guardWalletUnlocked(true) { unlocked ->
            if (unlocked) {
                val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this)
                mask.show()
                val otc = OtcManager.sharedOtcManager()
                val args = JSONObject().apply {
                    put("account", str_bankno)
                    put("btsAccount", otc.getCurrentBtsAccount())
                    put("qrCode", "")           //  for alipay & wechat pay
                    put("realName", str_realname)
                    put("remark", "")           //  for bank card
                    put("reservePhone", str_phoneno)    //  for bank card
                    put("type", OtcManager.EOtcPaymentMethodType.eopmt_bankcard.value)
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
