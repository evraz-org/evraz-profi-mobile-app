package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivitySettingThemeBinding

//  TODO: pending

class ActivitySettingTheme : BtsppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySettingThemeBinding.inflate(layoutInflater)
        setAutoLayoutContentView(binding.root)

        setFullScreen()

        binding.layoutBackFromSettingTheme.setOnClickListener { finish() }
    }
}
