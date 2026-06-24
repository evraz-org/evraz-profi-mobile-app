package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.OtcManager
import com.btsplusplus.fowallet.databinding.ActivityOtcUserAuthInfosBinding

class ActivityOtcUserAuthInfos : BtsppActivity() {

    private lateinit var binding: ActivityOtcUserAuthInfosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  设置自动布局
        setAutoLayoutContentView(R.layout.activity_otc_user_auth_infos)
        binding = ActivityOtcUserAuthInfosBinding.bind(findViewById<View>(android.R.id.content).rootView)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val auth_info = btspp_args_as_JSONObject().getJSONObject("auth_info")

        //  UI - 姓名
        var realName = auth_info.optString("realName", null)
        if (realName != null && realName.length >= 2) {
            realName = "*${realName.substring(1)}"
        }
        binding.tvRealnameFromOtcUserAuthinfo.text = realName ?: ""

        //  UI - 身份证号
        var idstr = auth_info.optString("idcardNo", null)
        if (idstr != null && idstr.length == 18) {
            idstr = "${idstr.substring(0, 6)}********${idstr.substring(14)}"
        }
        binding.tvIdcordnoFromOtcUserAuthinfo.text = idstr ?: ""

        //  UI - 联系方式
        binding.tvContactPhoneFromOtcUserAuthinfo.text = auth_info.optString("phone")

        //  UI - 状态
        if (auth_info.getInt("status") == OtcManager.EOtcUserStatus.eous_freeze.value) {
            binding.tvStatusFromOtcUserAuthinfo.text = resources.getString(R.string.kOtcAuthInfoCellLabelValueStatusFreeze)
            binding.tvStatusFromOtcUserAuthinfo.setTextColor(resources.getColor(R.color.theme01_sellColor))
        } else {
            binding.tvStatusFromOtcUserAuthinfo.text = resources.getString(R.string.kOtcAuthInfoCellLabelValueStatusOK)
            binding.tvStatusFromOtcUserAuthinfo.setTextColor(resources.getColor(R.color.theme01_buyColor))
        }

        //  事件 - 返回
        binding.layoutBackFromOtcUserAuthInfo.setOnClickListener { finish() }
    }
}
