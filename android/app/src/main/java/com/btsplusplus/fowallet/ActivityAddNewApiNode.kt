package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.BTS_NETWORK_CHAIN_ID
import bitshares.GrapheneConnection
import bitshares.Promise
import com.btsplusplus.fowallet.databinding.ActivityAddNewApiNodeBinding
import org.json.JSONObject

class ActivityAddNewApiNode : BtsppActivity() {

    private lateinit var _url_hash: JSONObject
    private var _result_promise: Promise? = null
    private lateinit var _binding: ActivityAddNewApiNodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAddNewApiNodeBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)
        setFullScreen()

        //  获取参数
        val args = btspp_args_as_JSONObject()
        _url_hash = args.getJSONObject("url_hash")
        _result_promise = args.opt("result_promise") as? Promise

        //  事件 - 返回
        _binding.layoutBackFromNewApiNode.setOnClickListener { finish() }

        //  事件 - 确定
        _binding.btnSubmit.setOnClickListener { onSubmitBtnClick() }
    }

    /**
     * 提交事件
     */
    private fun onSubmitBtnClick() {
        val name = _binding.tfNodeName.text.toString().trim()
        val url = _binding.tfNodeUrl.text.toString().trim()

        if (name.isEmpty()) {
            showToast(resources.getString(R.string.kSettingNewApiSubmitTipsPleaseInputNodeName))
            return
        }

        if (url.isEmpty()) {
            showToast(resources.getString(R.string.kSettingNewApiSubmitTipsPleaseInputNodeURL))
            return
        }

        if (_url_hash.has(url)) {
            showToast(resources.getString(R.string.kSettingNewApiSubmitTipsURLAlreadyExist))
            return
        }

        val node = JSONObject().apply {
            put("location", name)
            put("url", url)
            put("_is_custom", true)
        }

        val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
        GrapheneConnection.checkNodeStatus(node, 0, 0, false).then {
            mask.dismiss()
            val node_status = it as JSONObject
            if (node_status.optBoolean("connected")) {
                //  TODO: 以后也许考虑添加非mainnet等api节点。
                val chain_id = node_status.getJSONObject("chain_properties").optString("chain_id", "")
                if (chain_id != null && chain_id == BTS_NETWORK_CHAIN_ID) {
                    showToast(resources.getString(R.string.kSettingNewApiSubmitTipsOK))
                    //  返回上一个界面并刷新
                    _result_promise?.resolve(node)
                    _result_promise = null
                    finish()
                } else {
                    showToast(resources.getString(R.string.kSettingNewApiSubmitTipsNotBitsharesMainnetNode))
                }
            } else {
                showToast(resources.getString(R.string.kSettingNewApiSubmitTipsConnectedFailed))
            }
            return@then null
        }
    }
}
