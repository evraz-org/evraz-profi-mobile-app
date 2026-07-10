package com.btsplusplus.fowallet

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import bitshares.*
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement this
 * [FragmentMarketInfo.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FragmentMarketInfo.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FragmentMarketInfo : BtsppFragment() {
    private var listener: OnFragmentInteractionListener? = null

    private var _view: View? = null
    private var _context: Context? = null

    private var _favorites_market: Boolean = false
    private var _is_all_market: Boolean = false
    private var _favorites_asset_list: JSONArray? = null
    private var _marketInfos: JSONObject? = null
    private var _label_arrays = mutableListOf<JSONArray>()

    private var _inited = false

    override fun onInitParams(args: Any?) {
        val market_config_info = args as? JSONObject
        if (market_config_info != null && market_config_info.optString("type", "") == "all") {
            _is_all_market = true
            _favorites_market = false
            _marketInfos = null
        } else if (market_config_info != null) {
            _favorites_market = false
            _favorites_asset_list = null
            _marketInfos = market_config_info
            refreshCustomMarket()
        } else {
            _favorites_market = true
            _is_all_market = false
            _marketInfos = null
            _favorites_asset_list = null
            loadAllFavoritesMarkets()
        }
        _inited = true
    }

    /**
     * (public) 刷新UI（ticker数据变更）
     */
    fun onRefreshTickerData() {
        if (!_inited) {
            return
        }
        _label_arrays.forEach {
            val base_symbol = it.getString(0)
            val quote_symbol = it.getString(1)
            val label_price = it.get(2) as TextView
            val label_percent = it.get(3) as TextView
            val label_24vol = it.get(4) as TextView
            val ticker_show_data = _getTickerData(base_symbol, quote_symbol)
            label_price.text = ticker_show_data.getString("price_str")
            label_24vol.text = ticker_show_data.getString("volume_str")
            label_percent.text = ticker_show_data.getString("percent_str")
            label_percent.setBackgroundColor(resources.getColor(ticker_show_data.getInt("percent_color")))
        }
    }

    /**
     *  (public) 刷新自选市场
     */
    fun onRefreshFavoritesMarket() {
        if (_is_all_market) {
            _refreshUI()
        } else {
            refreshCustomMarket()
            loadAllFavoritesMarkets()
        }
    }

    /**
     *  (private) 刷新自选市场列表
     */
    private fun loadAllFavoritesMarkets() {
        if (!_favorites_market) {
            return
        }

        _favorites_asset_list = AppCacheManager.sharedAppCacheManager().get_all_fav_markets().values().toList<JSONObject>().sortedBy { it.getString("base") }.toJsonArray()

        if (_view != null) {
            _refreshUI()
        }
    }

    private fun refreshCustomMarket() {
        if (_is_all_market || _favorites_market) {
            return
        }

        if (_marketInfos == null) {
            return
        }

        val curr_base_symbol = _marketInfos!!.getJSONObject("base").getString("symbol")

        for (market in ChainObjectManager.sharedChainObjectManager().getMergedMarketInfos()) {
            if (market.getJSONObject("base").getString("symbol") == curr_base_symbol) {
                _marketInfos = market
                break
            }
        }

        _refreshUI()
    }

    private fun _refreshUI() {
        if (_view == null) {
            return
        }

        val container = _view!!.findViewById<LinearLayout>(R.id.markets_info_sv)
        container.removeAllViews()

        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        _label_arrays.clear()

        if (_favorites_market) {
            if (_favorites_asset_list != null && _favorites_asset_list!!.length() > 0) {
                for (fav_item in _favorites_asset_list!!.forin<JSONObject>()) {
                    fav_item!!.tap {
                        _refreshDrawOnCell(null, _context!!, container,
                                chainMgr.getChainObjectByID(it.getString("quote")),
                                chainMgr.getChainObjectByID(it.getString("base")))
                    }
                }
            } else {
                container.addView(ViewUtils.createEmptyCenterLabel(_context!!, _context!!.resources.getString(R.string.kLabelNoFavMarket)))
            }
        } else if (_is_all_market) {
            val mergedMarkets = chainMgr.getMergedMarketInfos()
            for (market in mergedMarkets) {
                val base_symbol = market.getJSONObject("base").getString("symbol")
                val base_asset = chainMgr.getAssetBySymbol(base_symbol)

                val group_list = market.getJSONArray("group_list")
                for (i in 0 until group_list.length()) {
                    val group = group_list.getJSONObject(i)
                    val group_key = group.getString("group_key")
                    val group_info = chainMgr.getGroupInfoFromGroupKey(group_key)

                    val quote_list = group.getJSONArray("quote_list")
                    for (j in 0 until quote_list.length()) {
                        val quote_symbol = quote_list.getString(j)
                        val quote_asset = chainMgr.getAssetBySymbol(quote_symbol)
                        _refreshDrawOnCell(group_info, _context!!, container, quote_asset, base_asset)
                    }
                }
            }
        } else {
            val group_list = _marketInfos!!.getJSONArray("group_list")
            for (i in 0 until group_list.length()) {
                val group = group_list.getJSONObject(i)

                val group_key = group.getString("group_key")
                val group_info = chainMgr.getGroupInfoFromGroupKey(group_key)

                val quote_list = group.getJSONArray("quote_list")
                for (j in 0 until quote_list.length()) {
                    val base_symbol = _marketInfos!!.getJSONObject("base").getString("symbol")
                    val quote_symbol = quote_list.getString(j)
                    _refreshDrawOnCell(group_info, _context!!, container,
                            chainMgr.getAssetBySymbol(quote_symbol),
                            chainMgr.getAssetBySymbol(base_symbol))
                }
            }
        }
    }

    private fun _getTickerData(base_symbol: String, quote_symbol: String): JSONObject {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        val ticker_data = chainMgr.getTickerData(base_symbol, quote_symbol)
        val base_asset = chainMgr.getAssetBySymbol(base_symbol)

        val latest: String
        val quote_volume: String
        val percent_change: String
        if (ticker_data != null) {
            var sym = ""
            if (base_symbol == "CNY") {
                sym = "¥"
            } else if (base_symbol == "USD") {
                sym = "$"
            }
            latest = String.format("%s%s", sym, OrgUtils.formatFloatValue(ticker_data.getString("latest").toDouble(), base_asset.getInt("precision")))
            quote_volume = ticker_data.getString("quote_volume")
            percent_change = ticker_data.getString("percent_change")
        } else {
            latest = "--"
            quote_volume = "--"
            percent_change = "0"
        }

        val percent_color: Int
        val percent_str: String

        val percent = percent_change.toDouble()
        if (percent > 0.0f) {
            percent_color = R.color.theme01_buyColor
            percent_str = "+$percent_change%"
        } else if (percent < 0) {
            percent_color = R.color.theme01_sellColor
            percent_str = "$percent_change%"
        } else {
            percent_color = R.color.theme01_zeroColor
            percent_str = "$percent_change%"
        }

        val self = this
        return JSONObject().apply {
            put("price_str", latest)
            put("volume_str", "${self.resources.getString(R.string.kLabelHeader24HVol)} $quote_volume")
            put("percent_str", percent_str)
            put("percent_color", percent_color)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun _refreshDrawOnCell(group_info: JSONObject?, ctx: Context, container: LinearLayout, quote_asset: JSONObject, base_asset: JSONObject) {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        val base_symbol = base_asset.getString("symbol")
        val quote_symbol = quote_asset.getString("symbol")

        val base_market = chainMgr.getDefaultMarketInfoByBaseSymbol(base_symbol)
        val base_market_name = if (base_market != null) {
            base_market.getJSONObject("base").getString("name")
        } else {
            base_symbol
        }

        var quote_name = quote_symbol
        if (group_info != null && group_info.optBoolean("gateway")) {
            val group_prefix = group_info.optString("prefix")
            if (quote_name.indexOf(group_prefix) == 0) {
                val ary = quote_name.split(".")
                if (ary.count() >= 2 && ary[0] == group_prefix) {
                    quote_name = ary.subList(1, ary.size).joinToString(".")
                }
            }
        }

        var base_name = base_market_name
        if (base_name == quote_name) {
            base_name = base_symbol
        }

        val ticker_show_data = _getTickerData(base_symbol, quote_symbol)

        val cell = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, toDp(48f))
        }

        val layout_quote_base_flag = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER_VERTICAL
                setMargins(10.dp, 3.dp, 0, 0)
            }
            orientation = LinearLayout.HORIZONTAL

            val tv1 = TextView(ctx).apply {
                setTextColor(resources.getColor(R.color.theme01_textColorMain))
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
                text = "$quote_name / $base_name"
            }
            addView(tv1)
        }

        val tv_volume = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                setMargins(toDp(10f), toDp(23f), 0, 0)
            }
            text = ticker_show_data.getString("volume_str")
            setTextColor(resources.getColor(R.color.theme01_textColorNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
        }

        val tv_price = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply {
                setMargins(0, 0, toDp(85f), 0)
            }
            text = ticker_show_data.getString("price_str")
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setTextColor(resources.getColor(R.color.theme01_textColorMain))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f)
        }

        val tv_percent = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(toDp(70f), toDp(25f), Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply {
                setMargins(0, 0, toDp(10f), 0)
            }
            gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
            setTextColor(resources.getColor(R.color.theme01_textColorPercent))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f)
            text = ticker_show_data.getString("percent_str")
            setBackgroundColor(resources.getColor(ticker_show_data.getInt("percent_color")))
        }

        cell.addView(layout_quote_base_flag)
        cell.addView(tv_volume)
        cell.addView(tv_price)
        cell.addView(tv_percent)

        container.addView(cell)

        _label_arrays.add(jsonArrayfrom(base_symbol, quote_symbol, tv_price, tv_percent, tv_volume))

        cell.setOnClickListener {
            btsppLogTrack("goto kline base: $base_symbol quote: $quote_symbol")
            activity?.goTo(ActivityKLine::class.java, true, args = jsonArrayfrom(base_asset, quote_asset))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _context = inflater.context
        return inflater.inflate(R.layout.fragment_market_info, container, false).also {
            _view = it
            _refreshUI()
        }
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(uri: Uri)
    }
}