package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivityVestingBalanceBinding

class ActivityVestingBalance : BtsppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityVestingBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFullScreen()

        binding.layoutBackFromPageOfUnfreezeAmount.setOnClickListener {
            finish()
        }
    }
}