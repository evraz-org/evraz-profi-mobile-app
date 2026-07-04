package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View

class SignUpInfoActivity : BtsppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLayoutContentView(R.layout.activity_sign_up_info)

        findViewById<View>(R.id.layout_back).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.sign_up_next).setOnClickListener {
            goTo(ActivityRegisterEntry::class.java, true)
        }
    }
}