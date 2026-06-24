package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import bitshares.Promise
import com.btsplusplus.fowallet.databinding.ActivityBlindAccountImportBinding
import com.btsplusplus.fowallet.utils.VcUtils

class ActivityBlindAccountImport : BtsppActivity() {

    private lateinit var binding: ActivityBlindAccountImportBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置自动布局
        setAutoLayoutContentView(R.layout.activity_blind_account_import)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        binding = ActivityBlindAccountImportBinding.bind(findViewById<View>(android.R.id.content).rootView)

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val result_promise = args.opt("result_promise") as? Promise

        // 提交事件
        binding.btnImportSubmit.setOnClickListener { onSubmit(result_promise) }

        // 返回事件
        binding.layoutBackFromBlindAccountImport.setOnClickListener { finish() }
    }

    private fun onSubmit(result_promise: Promise?) {
        val alias_name = binding.tvAliasName.text.toString().trim()
        val brain_key = binding.tvBrainKey.text.toString().trim()

        VcUtils.processImportBlindAccount(this, alias_name, brain_key) { blind_account ->
            //  导入成功
            result_promise?.resolve(blind_account)
            finish()
        }
    }
}
