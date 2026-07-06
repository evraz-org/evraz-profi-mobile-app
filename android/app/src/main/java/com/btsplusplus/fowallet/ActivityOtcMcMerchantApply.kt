package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.toList
import com.btsplusplus.fowallet.databinding.ActivityOtcMcMerchantApplyBinding
import org.json.JSONArray

class ActivityOtcMcMerchantApply : BtsppActivity() {

    private lateinit var _binding: ActivityOtcMcMerchantApplyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityOtcMcMerchantApplyBinding.inflate(layoutInflater)
        // 设置自动布局
        setAutoLayoutContentView(_binding.root)
        // 设置全屏
        setFullScreen()

        _binding.tvAccountNameFromOtcMcMerchantApply.text = "susu01"
        _binding.layoutSelectBakAccountFromOtcMcMerchantApply.setOnClickListener { onSelectBakAccount() }
        _binding.tvApplySubmitFromOtcMcMerchantApply.setOnClickListener { onApplySubmit() }
        _binding.layoutBackFromOtcMcMerchantApply.setOnClickListener { finish() }
    }

    private fun onApplySubmit() {

    }

    private fun onSelectBakAccount() {

        val bak_acconts = JSONArray().apply {
            put("susu02")
            put("susu03")
        }
        ViewSelector.show(this, "请选择备用账号", bak_acconts.toList<String>().toTypedArray()) { index: Int, _: String ->
            _binding.tvBakAccountNameFromOtcMcMerchantApply.text = bak_acconts.getString(index)
        }

    }
}
