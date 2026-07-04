package com.btsplusplus.fowallet

import android.os.Bundle
import android.widget.TextView
import bitshares.AppCacheManager
import bitshares.TempManager
import bitshares.toList
import bitshares.values
import com.btsplusplus.fowallet.databinding.ActivityAccountQueryBaseBinding
import org.json.JSONObject

/**
 *  枚举 - 搜索类型
 */
enum class ENetworkSearchType(val value: Int) {
    enstAccount(0),             //  搜索用户（帐号）
    enstTradingPair(1),         //  搜索资产（添加交易对）TODO:5.0 暂时不支持
    enstAssetAll(2),            //  搜索资产（所有）
    enstAssetSmart(3),          //  搜索资产（智能币）
    enstAssetUIA(4)             //  搜索资产（用户发行）
}

class ActivityAccountQueryBase : BtsppActivity() {

    private var _searchType = ENetworkSearchType.enstAccount
    private lateinit var _binding: ActivityAccountQueryBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAccountQueryBaseBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)

        setFullScreen()

        //  获取参数
        val args = _btspp_params as? JSONObject
        if (args != null) {
            if (args.has("kSearchType")) {
                _searchType = args.get("kSearchType") as ENetworkSearchType
            }
            val title = args.optString("kTitle")
            if (title.isNotEmpty()) {
                findViewById<TextView>(R.id.title).text = title
            }
        }

        //  初始化UI
        drawUI_defaultValues()

        //  事件 - 返回
        _binding.layoutBackFromServicesAccountQuery.setOnClickListener { finish() }

        //  事件 - 点击输入框

        _binding.tfSearchEntry.isFocusable = false
        _binding.tfSearchEntry.isFocusableInTouchMode = false
        _binding.tfSearchEntry.setOnClickListener {
            goTo(ActivityAccountQueryResult::class.java, false, args = JSONObject().apply {
                put("kSearchType", _searchType)
            })
        }
    }

    private fun drawUI_defaultValues() {
        when (_searchType) {
            ENetworkSearchType.enstAccount -> {
                _binding.tfSearchEntry.hint = resources.getString(R.string.kSearchPlaceholderAccount)

                val data_array = AppCacheManager.sharedAppCacheManager().get_all_fav_accounts().values().toList<JSONObject>().sortedBy { it.getString("name") }

                findViewById<TextView>(R.id.tv_my_favs).text = String.format(resources.getString(R.string.kSearchTipsMyFavAccount), "${data_array.size}")

                _binding.lytDefaultResultView.removeAllViews()
                for (data in data_array) {
                    val v = ViewUtils.auxGenSearchAccountLineView(this, data.getString("name"), data.getString("id"), data) {
                        TempManager.sharedTempManager().call_query_account_callback(this, it as JSONObject)
                    }
                    _binding.lytDefaultResultView.addView(v)
                }
            }
            ENetworkSearchType.enstAssetAll -> {
                _binding.tfSearchEntry.hint = resources.getString(R.string.kSearchPlaceholderAsset)
                findViewById<TextView>(R.id.tv_my_favs).text = String.format(resources.getString(R.string.kSearchTipsMyFavAssets), "0")
                //  TODO:5.0资产搜索暂时无默认值
            }
            ENetworkSearchType.enstAssetUIA -> {
                _binding.tfSearchEntry.hint = resources.getString(R.string.kSearchPlaceholderAsset)
                findViewById<TextView>(R.id.tv_my_favs).text = String.format(resources.getString(R.string.kSearchTipsMyFavAssets), "0")
                //  TODO:5.0资产搜索暂时无默认值
            }
            ENetworkSearchType.enstAssetSmart -> {
                _binding.tfSearchEntry.hint = resources.getString(R.string.kSearchPlaceholderAsset)
                findViewById<TextView>(R.id.tv_my_favs).text = String.format(resources.getString(R.string.kSearchTipsMyFavAssets), "0")
                //  TODO:5.0资产搜索暂时无默认值
            }
            ENetworkSearchType.enstTradingPair -> {
                //  TODO:5.0交易对搜索暂时在其他界面。还没整合完毕。
            }
        }
    }

}
