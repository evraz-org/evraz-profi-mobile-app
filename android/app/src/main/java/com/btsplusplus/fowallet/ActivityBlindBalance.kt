package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityBlindBalanceBinding
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONArray
import org.json.JSONObject

class ActivityBlindBalance : BtsppActivity() {

    private lateinit var _binding: ActivityBlindBalanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityBlindBalanceBinding.inflate(layoutInflater)

        // 设置自动布局
        setAutoLayoutContentView(_binding.root)
        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  导入收据按钮事件
        _binding.buttonAddFromBlindBalance.setOnClickListener { onAddbuttonClicked() }

        //  返回事件
        _binding.layoutBackFromBlindBalance.setOnClickListener { finish() }

        //  查询数据依赖
        queryBlindBalanceAndDependence()
    }

    private fun onQueryBlindBalanceAndDependenceResponsed(data_array: JSONArray) {
        refreshUI(data_array)
    }

    private fun queryBlindBalanceAndDependence() {
        val data_array = AppCacheManager.sharedAppCacheManager().getAllBlindBalance().values()
        val ids = JSONObject()
        for (blind_balance in data_array.forin<JSONObject>()) {
            ids.put(blind_balance!!.getJSONObject("decrypted_memo").getJSONObject("amount").getString("asset_id"), true)
        }
        if (ids.length() > 0) {
            VcUtils.simpleRequest(this, ChainObjectManager.sharedChainObjectManager().queryAllGrapheneObjects(ids.keys().toJSONArray())) {
                onQueryBlindBalanceAndDependenceResponsed(data_array)
            }
        } else {
            onQueryBlindBalanceAndDependenceResponsed(data_array)
        }
    }

    private fun refreshUI(data_array: JSONArray?) {
        val list = data_array
                ?: AppCacheManager.sharedAppCacheManager().getAllBlindBalance().values()

        //  清空
        val container = _binding.layoutReceiptListFromBlindBalance
        container.removeAllViews()

        if (list.length() > 0) {
            //  描绘
            var index = 0
            list.forEach<JSONObject> {
                val blind_balance = it!!

                val cell = ViewBlindReceiptCell(this, blind_balance, index, can_check = false)
                //  事件
                cell.setOnClickListener { onCellClicked(blind_balance) }
                container.addView(cell)
                container.addView(ViewLine(this, margin_top = 8.dp))

                index++
            }
        } else {
            //  无数据
            container.addView(ViewUtils.createEmptyCenterLabel(this, resources.getString(R.string.kVcStTipEmptyNoBlindCanImport)))
        }
    }

    private fun onCellClicked(blind_balance: JSONObject) {
        ViewSelector.show(this, "", arrayOf(resources.getString(R.string.kVcStBlindBalanceActionTransferFromBlind),
                resources.getString(R.string.kVcStBlindBalanceActionBlindTransfer))) { index: Int, _: String ->
            if (index == 0) {
                goTo(ActivityTransferFromBlind::class.java, true, args = JSONObject().apply {
                    put("blind_balance", blind_balance)
                })
            } else {
                goTo(ActivityBlindTransfer::class.java, true, args = JSONObject().apply {
                    put("blind_balance", blind_balance)
                })
            }
        }
    }

    private fun onAddbuttonClicked() {
        val result_promise = Promise()
        goTo(ActivityBlindBalanceImport::class.java, true, args = JSONObject().apply {
            put("result_promise", result_promise)
        })
        result_promise.then { dirty ->
            //  刷新UI
            if (dirty != null && dirty as Boolean) {
                refreshUI(null)
            }
        }
    }
}
