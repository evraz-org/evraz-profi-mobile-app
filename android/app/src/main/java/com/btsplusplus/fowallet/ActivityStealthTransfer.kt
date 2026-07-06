package com.btsplusplus.fowallet

import android.os.Bundle
import android.support.v4.content.ContextCompat
import bitshares.Promise
import bitshares.jsonArrayfrom
import com.btsplusplus.fowallet.databinding.ActivityStealthTransferBinding
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import org.json.JSONArray
import org.json.JSONObject

class ActivityStealthTransfer : BtsppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityStealthTransferBinding.inflate(layoutInflater)
        setAutoLayoutContentView(binding.root)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  返回事件
        binding.layoutBackFromStealthTransfer.setOnClickListener { finish() }

        //  点击跳转事件
        binding.layoutAccountManageFromStealthTransfer.setOnClickListener { OnAccountManageClicked() }
        binding.layoutMyReceiptFromStealthTransfer.setOnClickListener { onMyReceiptClicked() }
        binding.layoutTransferToBlindFromStealthTransfer.setOnClickListener { onTransferToBlindClicked() }
        binding.layoutTransferFromBlindFromStealthTransfer.setOnClickListener { onTransferFromBlindClicked() }
        binding.layoutBlindTransferFromStealthTransfer.setOnClickListener { onBlindTransferClicked() }

        //  设置图标颜色
        binding.imgIconBlindAccounts.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal))
        binding.imgIconBlindBalances.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal))
        binding.imgIconTransferToBlind.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal))
        binding.imgIconTransferFromBlind.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal))
        binding.imgIconBlindTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal))

        //  设置箭头颜色
        binding.ivAccountManageRightArrowFromStealthTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorGray))
        binding.ivMyReceiptRightArrowFromStealthTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorGray))
        binding.ivTransferToBlindRightArrowFromStealthTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorGray))
        binding.ivTransferFromBlindRightArrowFromStealthTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorGray))
        binding.ivBlindTransferRightArrowFromStealthTransfer.setColorFilter(ContextCompat.getColor(applicationContext,R.color.theme01_textColorGray))
    }

    private fun OnAccountManageClicked() {
        val self = this
        goTo(ActivityBlindAccounts::class.java, true, args = JSONObject().apply {
            put("title", self.resources.getString(R.string.kVcTitleBlindAccountsMgr))
        })
    }

    private fun onMyReceiptClicked() {
        goTo(ActivityBlindBalance::class.java, true)
    }

    private fun onTransferToBlindClicked() {
        //  REMARK：默认隐私转账资产为 CORE 资产。
        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val core_asset_id = chainMgr.grapheneCoreAssetID
        val p1 = get_full_account_data_and_asset_hash(WalletManager.sharedWalletManager().getWalletAccountName()!!)
        val p2 = chainMgr.queryAllGrapheneObjects(jsonArrayfrom(core_asset_id))
        VcUtils.simpleRequest(this, Promise.all(p1, p2)) {
            val data_array = it as JSONArray
            val full_account_data = data_array.getJSONObject(0)
            val core = chainMgr.getChainObjectByID(core_asset_id)
            goTo(ActivityTransferToBlind::class.java, true, args = JSONObject().apply {
                put("core_asset", core)
                put("full_account_data", full_account_data)
            })
        }
    }

    private fun onTransferFromBlindClicked() {
        goTo(ActivityTransferFromBlind::class.java, true, args = JSONObject())
    }

    private fun onBlindTransferClicked() {
        goTo(ActivityBlindTransfer::class.java, true, args = JSONObject())
    }
}