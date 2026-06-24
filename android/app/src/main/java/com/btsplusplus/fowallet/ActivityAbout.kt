package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import bitshares.Utils
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityAboutBinding

class ActivityAbout : BtsppActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLayoutContentView(R.layout.activity_about)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  draw version
        val ver = Utils.appVersionName()
        val appname = R.string.kAppName.xmlstring(this)

        binding = ActivityAboutBinding.bind(findViewById<View>(android.R.id.content).rootView)

        binding.labelTxtIconVersion.text = "$appname v$ver"
        binding.labelTxtVersion.text = "$appname v$ver"

        //  back
        binding.layoutBackFromAbout.setOnClickListener { finish() }
    }
}
