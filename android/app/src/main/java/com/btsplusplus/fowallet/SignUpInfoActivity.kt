package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivitySignUpInfoBinding

class SignUpInfoActivity : BtsppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySignUpInfoBinding.inflate(layoutInflater)
        setAutoLayoutContentView(binding.root)
        setFullScreen()

        binding.layoutBack.setOnClickListener {
            finish()
        }

        binding.signUpNext.setOnClickListener {
            goTo(CreateAccountActivity::class.java, true)
        }
    }
}