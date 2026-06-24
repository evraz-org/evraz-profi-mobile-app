package com.btsplusplus.fowallet

import android.os.Bundle
import com.btsplusplus.fowallet.databinding.ActivityScanResultPaySuccessBinding

class ActivityScanResultPaySuccess : BtsppActivity() {

    private lateinit var binding: ActivityScanResultPaySuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  设置自动布局
        setAutoLayoutContentView(R.layout.activity_scan_result_pay_success)
        binding = ActivityScanResultPaySuccessBinding.bind(findViewById<View>(android.R.id.content).rootView)

        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val to_account = args.getJSONObject("to_account")
        val trx_id = args.optJSONArray("result")?.optJSONObject(0)?.getString("id") ?: ""
        val to_account_id = to_account.optString("id", null)
        val success_tip_string = args.optString("success_tip_string", null)

        val tv_pay_amount = binding.tvPayAmountFromScanResultPaySuccess
        val tv_receiver_account = binding.tvReceiverAccountFromScanResultPaySuccess
        val tv_tv_transaction_id = binding.tvTransactionIdFromScanResultPaySuccess

        //  UI - 支付成功图标
        binding.imgPaySuccess.setColorFilter(resources.getColor(R.color.theme01_textColorHighlight))
        //  UI - 支付成功提示文字
        binding.tvPaySuccessText.text = success_tip_string ?: resources.getString(R.string.kVcScanResultTipsPaySuccess)
        //  UI - 支付金额、收款人、交易ID
        tv_pay_amount.text = args.optString("amount_string")
        tv_receiver_account.text = to_account.getString("name")
        tv_tv_transaction_id.text = trx_id

        //  返回按钮点击
        binding.layoutBackFromScanResultPaySuccess.setOnClickListener { finish() }

        //  完成按钮点击
        binding.buttonFinishFromScanResultPaySuccess.setOnClickListener { finish() }

        //  接收账号整行点击
        binding.layoutReceiverAccountFromScanResultPaySuccess.setOnClickListener {
            if (to_account_id != null && to_account_id != "") {
                viewUserAssets(to_account_id)
            }
        }

        //  交易账号整行点击
        binding.layoutTransactionIdFromScanResultPaySuccess.setOnClickListener {
            if (trx_id != "") {
                openURL("https://bts.ai/tx/$trx_id")
            }
        }
    }
}
