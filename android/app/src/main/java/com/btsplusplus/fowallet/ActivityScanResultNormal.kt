package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivityScanResultNormalBinding

class ActivityScanResultNormal : BtsppActivity() {

    private lateinit var binding: ActivityScanResultNormalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置自动布局
        setAutoLayoutContentView(R.layout.activity_scan_result_normal)
        binding = ActivityScanResultNormalBinding.bind(findViewById<View>(android.R.id.content).rootView)
        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  设置参数
        binding.tvScanStrFromScanResultNormal.text = btspp_args_as_JSONObject().getString("result")
        binding.layoutBackFromScanResultNormal.setOnClickListener { finish() }
    }
}
