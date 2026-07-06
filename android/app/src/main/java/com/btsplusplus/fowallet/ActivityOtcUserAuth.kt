package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.AsyncTaskManager
import bitshares.OtcManager
import bitshares.isTrue
import com.btsplusplus.fowallet.databinding.ActivityOtcUserAuthBinding
import org.json.JSONObject

class ActivityOtcUserAuth : BtsppActivity() {

    private var _smsTimerId = 0
    private lateinit var _binding: ActivityOtcUserAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityOtcUserAuthBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  事件
        _binding.layoutBackFromOtcUserAuth.setOnClickListener { finish() }
        _binding.btnGetsmscode.setOnClickListener { sendPhoneAuthCode() }
        _binding.btnSubmit.setOnClickListener { onSubmit() }
    }

    override fun onDestroy() {
        //  移除定时器
        AsyncTaskManager.sharedAsyncTaskManager().removeSecondsTimer(_smsTimerId)
        super.onDestroy()
    }

    private fun sendPhoneAuthCode() {
        //  倒计时中
        if (AsyncTaskManager.sharedAsyncTaskManager().isExistSecondsTimer(_smsTimerId)) {
            return
        }

        val str_phone = _binding.tfPhone.text.toString()
        if (!OtcManager.checkIsValidPhoneNumber(str_phone)) {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputPhoneNo))
            return
        }

        //  TODO:2.9 配置，短信重发时间间隔。单位：秒。
        val max_countdown_secs = 60L
        val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this)
        mask.show()
        val otc = OtcManager.sharedOtcManager()
        otc.sendSmsCode(otc.getCurrentBtsAccount(), str_phone, OtcManager.EOtcSmsType.eost_id_verify).then {
            mask.dismiss()
            //  提示
            showToast(resources.getString(R.string.kOtcAuthInfoTailerTipsGetSmscodeOK))
            //  重发倒计时
            _binding.btnGetsmscode.isClickable = false
            _binding.btnGetsmscode.text = String.format(resources.getString(R.string.kOtcAuthInfoTailerBtnGetSmscodeWaitNsec), max_countdown_secs.toString())
            _smsTimerId = AsyncTaskManager.sharedAsyncTaskManager().scheduledSecondsTimer(max_countdown_secs) { left_ts ->
                if (left_ts > 0) {
                    _binding.btnGetsmscode.text = String.format(resources.getString(R.string.kOtcAuthInfoTailerBtnGetSmscodeWaitNsec), left_ts.toString())
                } else {
                    _binding.btnGetsmscode.isClickable = true
                    _binding.btnGetsmscode.text = resources.getString(R.string.kOtcAuthInfoTailerBtnGetSmscode)
                }
            }
            return@then null
        }.catch { err ->
            mask.dismiss()
            otc.showOtcError(this, err)
        }
    }

    private fun onSubmit() {
        //  是否开启新用户认证功能判断
        val otc = OtcManager.sharedOtcManager()
        assert(otc.server_config != null)
        val auth_config = otc.server_config!!.optJSONObject("order")
        if (auth_config == null || !auth_config.isTrue("enable")) {
            var msg = auth_config?.optString("msg", null)
            if (msg == null || msg.isEmpty()) {
                msg = resources.getString(R.string.kOtcEntryDisableDefaultMsg)
            }
            showToast(msg!!)
            return
        }

        val str_realname = _binding.tfRealname.text.toString()
        val str_idcard_no = _binding.tfIdcardNo.text.toString()
        val str_phone = _binding.tfPhone.text.toString()
        val str_smscode = _binding.tfSmscode.text.toString()

        if (str_realname.isEmpty()) {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputRealname))
            return
        }

        if (!OtcManager.checkIsValidChineseCardNo(str_idcard_no)) {
            showToast(resources.getString(R.string.kOtcAuthInfoSubmitTipsInputIdNo))
            return
        }

        if (!OtcManager.checkIsValidPhoneNumber(str_phone)) {
            showToast(resources.getString(R.string.kOtcRmSubmitTipsInputPhoneNo))
            return
        }

        if (str_smscode.isEmpty()) {
            showToast(resources.getString(R.string.kOtcAuthInfoSubmitTipsInputSmscode))
            return
        }

        //  认证
        val args = JSONObject().apply {
            put("btsAccount", otc.getCurrentBtsAccount())
            put("idcardNo", str_idcard_no)
            put("phoneNum", str_phone)
            put("realName", str_realname)
            put("smscode", str_smscode)
        }

        guardWalletUnlocked(true) { unlocked ->
            if (unlocked) {
                val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this)
                mask.show()
                otc.idVerify(args).then {
                    mask.dismiss()
                    //  提示 & 返回
                    showToast(resources.getString(R.string.kOtcAuthInfoSubmitTipsOK))
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
