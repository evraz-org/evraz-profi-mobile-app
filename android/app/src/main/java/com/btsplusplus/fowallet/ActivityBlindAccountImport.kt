package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.Promise
import com.btsplusplus.fowallet.databinding.ActivityBlindAccountImportBinding
import com.btsplusplus.fowallet.utils.VcUtils

class ActivityBlindAccountImport : BtsppActivity() {

    private lateinit var _binbing: ActivityBlindAccountImportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binbing = ActivityBlindAccountImportBinding.inflate(layoutInflater)

        // 设置自动布局
        setAutoLayoutContentView(_binbing.root)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val result_promise = args.opt("result_promise") as? Promise

        // 提交事件
        _binbing.btnImportSubmit.setOnClickListener { onSubmit(result_promise) }

        // 返回事件
        _binbing.layoutBackFromBlindAccountImport.setOnClickListener { finish() }
    }

    private fun onSubmit(result_promise: Promise?) {
        val alias_name = _binbing.tvAliasName.text.toString().trim()
        val brain_key = _binbing.tvBrainKey.text.toString().trim()

        VcUtils.processImportBlindAccount(this, alias_name, brain_key) { blind_account ->
            //  导入成功
            result_promise?.resolve(blind_account)
            finish()
        }
    }
}
