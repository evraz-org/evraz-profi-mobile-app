package com.btsplusplus.fowallet

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import bitshares.toList
import com.btsplusplus.fowallet.databinding.ActivityOtcMcMerchantApplyBinding
import org.json.JSONArray

class ActivityOtcMcMerchantApply : BtsppActivity() {

    private lateinit var binding: ActivityOtcMcMerchantApplyBinding

    lateinit var edit_text_nickname: EditText
    lateinit var tv_bak_account: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置自动布局
        setAutoLayoutContentView(R.layout.activity_otc_mc_merchant_apply)
        binding = ActivityOtcMcMerchantApplyBinding.bind(findViewById<View>(android.R.id.content).rootView)
        // 设置全屏
        setFullScreen()

        binding.tvBakAccountNameFromOtcMcMerchantApply.let { tv_bak_account = it }
        edit_text_nickname = binding.etInputNicknameFromOtcMcMerchantApply

        binding.tvAccountNameFromOtcMcMerchantApply.text = "susu01"
        binding.layoutSelectBakAccountFromOtcMcMerchantApply.setOnClickListener { onSelectBakAccount() }
        binding.tvApplySubmitFromOtcMcMerchantApply.setOnClickListener { onApplySubmit() }
        binding.layoutBackFromOtcMcMerchantApply.setOnClickListener { finish() }
    }

    private fun onApplySubmit() {

    }

    private fun onSelectBakAccount() {

        val bak_acconts = JSONArray().apply {
            put("susu02")
            put("susu03")
        }
        ViewSelector.show(this, "请选择备用账号", bak_acconts.toList<String>().toTypedArray()) { index: Int, _: String ->
            tv_bak_account.text = bak_acconts.getString(index)
        }

    }
}
