package com.btsplusplus.fowallet

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import bitshares.Utils
import bitshares.xmlstring
import com.btsplusplus.fowallet.gateway.GatewayAssetItemData
import com.btsplusplus.fowallet.databinding.ActivityGatewayRechargeBinding
import org.json.JSONObject

class ActivityGatewayDeposit : BtsppActivity() {

    private lateinit var binding: ActivityGatewayRechargeBinding

    private lateinit var _fullAccountData: JSONObject
    private lateinit var _depositAddrItem: JSONObject
    private lateinit var _depositAssetItem: JSONObject
    private var _depositMemoData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGatewayRechargeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  获取参数 / get params
        val args = btspp_args_as_JSONObject()
        _fullAccountData = args.getJSONObject("fullAccountData")
        _depositAddrItem = args.getJSONObject("depositAddrItem")
        _depositAssetItem = args.getJSONObject("depositAssetItem")
        //  获取 memo
        _depositMemoData = _depositAddrItem.opt("inputMemo") as? String
        if (_depositMemoData != null && _depositMemoData!!.isEmpty()) {
            _depositMemoData = null
        }

        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  back button
        binding.layoutBackFromGatewayRecharge.setOnClickListener { finish() }

        //  title
        binding.titleOfGatewayRecharge.text = args.getString("title")

        //  qrcode
        binding.ivQrcodeOfRechargePage.setImageBitmap(args.get("qrbitmap") as Bitmap)

        //  地址
        binding.idLabelAddress.text = _depositAddrItem.getString("inputAddress")
        //  备注
        if (_depositMemoData != null) {
            binding.idLabelMemo.text = _depositMemoData!!
        } else {
            binding.idLabelMemo.visibility = View.GONE
            binding.btnCopyMemo.visibility = View.GONE
        }

        //  Tip
        drawDepositTipMessages()

        //  event - copy address
        binding.btnCopyAddress.setOnClickListener { onCopyAddressClicked() }

        //  events - copy memo
        if (_depositMemoData != null) {
            binding.btnCopyMemo.setOnClickListener { onCopyMemoClicked() }
        }
    }

    private fun drawDepositTipMessages() {
        val appext = _depositAssetItem.get("kAppExt") as GatewayAssetItemData
        val msgArray = mutableListOf<String>()
        msgArray.add(R.string.kVcDWTipsImportantTitle.xmlstring(this))
        //  min deposit value
        val inputCoinType = _depositAddrItem.getString("inputCoinType").uppercase()
        val minAmount = appext.depositMinAmount
        if (minAmount != null && minAmount.isNotEmpty()) {
            msgArray.add(String.format(R.string.kVcDWTipsMinDepositAmount.xmlstring(this), minAmount, inputCoinType))
        }
        //  sec tips
        msgArray.add(String.format(R.string.kVcDWTipsDepositMatchAsset.xmlstring(this), inputCoinType, inputCoinType))
        //  confirm tips
        val confirm_block_number = appext.confirm_block_number
        if (confirm_block_number != null && confirm_block_number.isNotEmpty()) {
            msgArray.add(String.format(R.string.kVcDWTipsNetworkConfirmWithN.xmlstring(this), confirm_block_number))
        } else {
            msgArray.add(R.string.kVcDWTipsNetworkConfirm.xmlstring(this))
        }
        //  default tips
        msgArray.add(R.string.kVcDWTipsFindCustomService.xmlstring(this))

        binding.tipOfRechargePage.text = msgArray.joinToString("\n")
    }

    private fun onCopyAddressClicked() {
        if (Utils.copyToClipboard(this, _depositAddrItem.getString("inputAddress"))) {
            showToast(R.string.kVcDWTipsCopyAddrOK.xmlstring(this))
        }
    }

    private fun onCopyMemoClicked() {
        _depositMemoData?.let {
            if (Utils.copyToClipboard(this, it)) {
                showToast(R.string.kVcDWTipsCopyMemoOK.xmlstring(this))
            }
        }
    }
}
