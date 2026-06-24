package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.Promise
import bitshares.jsonArrayfrom
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import com.btsplusplus.fowallet.databinding.ActivityStealthTransferBinding
import org.json.JSONArray
import org.json.JSONObject

class ActivityStealthTransfer : BtsppActivity() {

    private lateinit var binding: ActivityStealthTransferBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置自动布局
        setAutoLayoutContentView(R.layout.activity_stealth_transfer)
        binding = ActivityStealthTransferBinding.bind(findViewById<View>(android.R.id.content).rootView)

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
        binding.imgIconBlindAccounts.setColorFilter(resources.getColor(R.color.theme01_textColorNormal))
        binding.imgIconBlindBalances.setColorFilter(resources.getColor(R.color.theme01_textColorNormal))
        binding.imgIconTransferToBlind.setColorFilter(resources.getColor(R.color.theme01_textColorNormal))
        binding.imgIconTransferFromBlind.setColorFilter(resources.getColor(R.color.theme01_textColorNormal))
        binding.imgIconBlindTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorNormal))

        //  设置箭头颜色
        binding.ivAccountManageRightArrowFromStealthTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
        binding.ivMyReceiptRightArrowFromStealthTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
        binding.ivTransferToBlindRightArrowFromStealthTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
        binding.ivTransferFromBlindRightArrowFromStealthTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
        binding.ivBlindTransferRightArrowFromStealthTransfer.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
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
