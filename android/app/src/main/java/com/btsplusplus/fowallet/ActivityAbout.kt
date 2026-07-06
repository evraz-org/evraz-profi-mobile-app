package com.btsplusplus.fowallet

import android.annotation.SuppressLint
import android.os.Bundle
import bitshares.Utils
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityAboutBinding

class ActivityAbout : BtsppActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setAutoLayoutContentView(binding.root)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()


        //  draw version
        val ver = Utils.appVersionName()
        val appname = R.string.kAppName.xmlstring(this)
        binding.labelTxtIconVersion.text = "$appname v$ver"
        binding.labelTxtVersion.text = "$appname v$ver"

        //  back
        binding.layoutBackFromAbout.setOnClickListener { finish() }
    }
}
