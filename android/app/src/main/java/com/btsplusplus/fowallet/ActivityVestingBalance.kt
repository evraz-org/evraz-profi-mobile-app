package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivityVestingBalanceBinding

//  TODO: pending

class ActivityVestingBalance : BtsppActivity() {

    private lateinit var binding: ActivityVestingBalanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVestingBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setFullScreen()

        binding.layoutBackFromPageOfUnfreezeAmount.setOnClickListener {
            finish()
        }
    }
}
