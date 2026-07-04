package com.btsplusplus.fowallet

import android.graphics.Bitmap
import android.os.Bundle
import bitshares.Utils
import com.btsplusplus.fowallet.databinding.ActivityBlindBackupReceiptBinding

class ActivityBlindBackupReceipt : BtsppActivity() {

    private lateinit var _binding: ActivityBlindBackupReceiptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityBlindBackupReceiptBinding.inflate(layoutInflater)

        //  设置自动布局
        setAutoLayoutContentView(_binding.root)

        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        val blind_receipt_string = args.getString("blind_receipt_string")

        //  初始化UI - 二维码
        _binding.ivQrcodeFromBlindBackupReceipt.setImageBitmap(args.get("qrbitmap") as Bitmap)

        //  UI - 收据信息
        _binding.tvBlindReceiptString.text = blind_receipt_string

        //  复制按钮点击
        _binding.btnCopyBlindReceipt.setOnClickListener { onCopyAddressClicked(blind_receipt_string) }

        //  完成点击
        _binding.btnNaviLeftDone.setOnClickListener { onDoneClicked() }
        _binding.btnDone.setOnClickListener { onDoneClicked() }
    }

    private fun onDoneClicked() {
        UtilsAlert.showMessageConfirm(this, resources.getString(R.string.kWarmTips),
                resources.getString(R.string.kVcStTipAskConfrimForCloseBackupReceiptUI)).then {
            if (it != null && it as Boolean) {
                //  关闭
                finish()
            }
        }
    }

    private fun onCopyAddressClicked(blind_receipt_string: String) {
        if (Utils.copyToClipboard(this, blind_receipt_string)) {
            showToast(resources.getString(R.string.kVcStTipReceiptBackupCopied))
        }
    }
}
