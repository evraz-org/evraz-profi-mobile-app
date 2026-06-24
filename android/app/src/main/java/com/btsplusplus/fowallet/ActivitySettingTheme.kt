package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivitySettingThemeBinding

//  TODO: pending

class ActivitySettingTheme : BtsppActivity() {

    private lateinit var binding: ActivitySettingThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLayoutContentView(R.layout.activity_setting_theme)
        binding = ActivitySettingThemeBinding.bind(findViewById<View>(android.R.id.content).rootView)

        setFullScreen()

        binding.layoutBackFromSettingTheme.setOnClickListener { finish() }
    }
}
