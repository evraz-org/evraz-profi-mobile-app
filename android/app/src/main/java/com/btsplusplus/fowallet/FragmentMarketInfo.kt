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

    private var _favorites_market: Boolean = false                          //  是否是自选市场
    private var _is_all_market: Boolean = false                             //  是否是All市场（显示所有交易对）
    private var _favorites_asset_list: JSONArray? = null                    //  自选列表（非自选市场该变量为nil。）
    private var _marketInfos: JSONObject? = null                            //  市场信息配置（基本资产、引用资产、分组信息等）
    private var _label_arrays = mutableListOf<JSONArray>()                  //  数组(base, quote, price, percent, 24volume)

    private var _inited = false

    override fun onInitParams(args: Any?) {
        val market_config_info = args as? JSONObject
        if (market_config_info != null && market_config_info.optString("type", "") == "all") {
            //  All市场模式 - 显示所有交易对
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
        //  添加到缓存
        _label_arrays.forEach {
            val base_symbol = it.getString(0)
            val quote_symbol = it.getString(1)
            val label_price = it.get(2) as TextView
            val label_percent = it.get(3) as TextView
            val label_24vol = it.get(4) as TextView
            //  更新数据
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
            //  All市场模式下，如果自选市场发生变化，重新加载所有数据
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
        //  非自选市场不刷新。
        if (!_favorites_market) {
            return
        }

        //  加载数据
        _favorites_asset_list = AppCacheManager.sharedAppCacheManager().get_all_fav_markets().values().toList<JSONObject>().sortedBy { it.getString("base") }.toJsonArray()

        //  如果有UI界面则刷新。
        if (_view != null) {
            _refreshUI()
        }
    }

    /**
     *  (private) 刷新自定义交易对
     */
    private fun refreshCustomMarket() {
        //  All市场和自选列表不处理
        if (_is_all_market || _favorites_market) {
            return
        }

        if (_marketInfos == null) {
            return
        }

        //  获取当前 base 信息
        val curr_base_symbol = _marketInfos!!.getJSONObject("base").getString("symbol")

        //  从合并后的列表筛选当前base对应的市场信息
        for (market in ChainObjectManager.sharedChainObjectManager().getMergedMarketInfos()) {
            if (market.getJSONObject("base").getString("symbol") == curr_base_symbol) {
                _marketInfos = market
                break
            }
        }

        //  重新加载
        _refreshUI()
    }

    /**
     * 刷新UI，描绘所有数据。
     */
    private fun _refreshUI() {
        if (_view == null) {
            return
        }

        val container = _view!!.findViewById<LinearLayout>(R.id.markets_info_sv)
        container.removeAllViews()

        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        //  先清空
        _label_arrays.clear()

        if (_favorites_market) {
            //  - 自定义市场
            if (_favorites_asset_list != null && _favorites_asset_list!!.length() > 0) {
                //  描绘所有自选交易对
                for (fav_item in _favorites_asset_list!!.forin<JSONObject>()) {
                    fav_item!!.tap {
                        _refreshDrawOnCell(null, _context!!, container,
                                chainMgr.getChainObjectByID(it.getString("quote")),
                                chainMgr.getChainObjectByID(it.getString("base")))
                    }
                }
            } else {
                //  没有自选交易对
                container.addView(ViewUtils.createEmptyCenterLabel(_context!!, _context!!.resources.getString(R.string.kLabelNoFavMarket)))
            }
        } else if (_is_all_market) {
            //  All市场模式 - 遍历所有市场的所有交易对
            val mergedMarkets = chainMgr.getMergedMarketInfos()
            for (market in mergedMarkets) {
                val base_symbol = market.getJSONObject("base").getString("symbol")
                val base_asset = chainMgr.getAssetBySymbol(base_symbol)

                //  遍历所有分组
                val group_list = market.getJSONArray("group_list")
                for (i in 0 until group_list.length()) {
                    val group = group_list.getJSONObject(i)
                    val group_key = group.getString("group_key")
                    val group_info = chainMgr.getGroupInfoFromGroupKey(group_key)

                    //  遍历该分组下的所有quote资产
                    val quote_list = group.getJSONArray("quote_list")
                    for (j in 0 until quote_list.length()) {
                        val quote_symbol = quote_list.getString(j)
                        val quote_asset = chainMgr.getAssetBySymbol(quote_symbol)
                        _refreshDrawOnCell(group_info, _context!!, container, quote_asset, base_asset)
                    }
                }
            }
        } else {
            //  普通市场

            //  遍历描绘所有分组
            val group_list = _marketInfos!!.getJSONArray("group_list")
            for (i in 0 until group_list.length()) {
                val group = group_list.getJSONObject(i)

                //  获取分组信息
                val group_key = group.getString("group_key")
                val group_info = chainMgr.getGroupInfoFromGroupKey(group_key)

                //  描绘单个分组下的所有交易对
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

    /**
     * 生成 Ticker 最终显示数据，格式化数据等。
     */
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
                sym = "¥"   //  REMARK：半角形式，如果需要全角用这个￥。
            } else if (base_symbol == "USD") {
                sym = "$"   //  REMARK：半角形式，如果需要全角用这个＄。
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
        //  -- 准备数据
        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        //  获取资产信息
        val base_symbol = base_asset.getString("symbol")
        val quote_symbol = quote_asset.getString("symbol")

        //  获取 base market 名
        val base_market = chainMgr.getDefaultMarketInfoByBaseSymbol(base_symbol)
        val base_market_name = if (base_market != null) {
            base_market.getJSONObject("base").getString("name")
        } else {
            base_symbol
        }

        //  获取交易资产（quote资产）显示名  REMARK：如果是网关资产、则移除网关前缀。自选市场没有分组信息，网关资产也显示全称。
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

        //  获取报价资产显示名称
        var base_name = base_market_name
        //  REMARK：如果 base 的别名刚好和交易资产名字相同，则显示 base 的原始资产名字。
        if (base_name == quote_name) {
            base_name = base_symbol
        }

        val ticker_show_data = _getTickerData(base_symbol, quote_symbol)

        //  -- 初始化UI
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

        //  24H量
        val tv_volume = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                setMargins(toDp(10f), toDp(23f), 0, 0)
            }
            text = ticker_show_data.getString("volume_str")
            setTextColor(resources.getColor(R.color.theme01_textColorNormal))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
        }

        //  价格
        val tv_price = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT or Gravity.CENTER_VERTICAL).apply {
                setMargins(0, 0, toDp(85f), 0)
            }
            text = ticker_show_data.getString("price_str")
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setTextColor(resources.getColor(R.color.theme01_textColorMain))
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f)
        }

        //  百分比
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

        //  渲染每一行
        cell.addView(layout_quote_base_flag)
        cell.addView(tv_volume)
        cell.addView(tv_price)
        cell.addView(tv_percent)

        container.addView(cell)

        //  添加到缓存
        _label_arrays.add(jsonArrayfrom(base_symbol, quote_symbol, tv_price, tv_percent, tv_volume))

        //  点击cell进入K线界面
        cell.setOnClickListener {
            btsppLogTrack("goto kline base: $base_symbol quote: $quote_symbol")
            activity?.goTo(ActivityKLine::class.java, true, args = jsonArrayfrom(base_asset, quote_asset))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _context = inflater.context
        return inflater.inflate(R.layout.fragment_market_info, container, false).also {
            _view = it
            _refreshUI()
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }
}
