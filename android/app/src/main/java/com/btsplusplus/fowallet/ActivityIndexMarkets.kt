package com.btsplusplus.fowallet

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityIndexMarketsBinding
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONObject
import java.util.*


class ActivityIndexMarkets : BtsppActivity() {

    private val fragmens: ArrayList<Fragment> = ArrayList()

    private var _tickerRefreshTimer: Timer? = null

    private lateinit var _binding: ActivityIndexMarketsBinding

    /**
     * 重载 - 返回键按下
     */
    override fun onBackPressed() {
        goHome()
    }

    override fun onPause() {
        super.onPause()
        stopTickerRefreshTimer()
        AppCacheManager.sharedAppCacheManager().saveToFile()
    }

    //  事件：已经进入前台
    override fun onResume() {
        super.onResume()
        GrapheneConnectionManager.sharedGrapheneConnectionManager().reconnect_all()
        onRefreshFavoritesMarket()
        startTickerRefreshTimer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityIndexMarketsBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root, navigationBarColor = R.color.theme01_tabBarColor)

        _binding.tablayout.let { tab ->
            tab.addTab(tab.newTab().apply {
                this.icon =  ContextCompat.getDrawable(this@ActivityIndexMarkets, R.drawable.ic_btn_star)
            })
            tab.addTab(tab.newTab().apply {
                text = "All"
            })
            ChainObjectManager.sharedChainObjectManager().getMergedMarketInfos().forEach { market ->
                tab.addTab(tab.newTab().apply {
                    text = market.getJSONObject("base").getString("name")
                })
            }
        }

        setFragments()
        setViewPager(1, R.id.view_pager, R.id.tablayout, fragmens)
        setTabListener(R.id.tablayout, R.id.view_pager)

        setAddBtnListener()
        setFullScreen()
        setBottomNavigationStyle(_binding.bottomNav, 1)
    }

    /**
     * 启动定时器：刷新Ticker数据用
     */
    private fun startTickerRefreshTimer() {
        if (_tickerRefreshTimer == null) {
            _tickerRefreshTimer = Timer()
            _tickerRefreshTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    delay_main {
                        onTimerTickerRefresh()
                    }
                }
            }, 300, 1000)
        }
    }

    /**
     * 停止定时器
     */
    private fun stopTickerRefreshTimer() {
        if (_tickerRefreshTimer != null) {
            _tickerRefreshTimer!!.cancel()
            _tickerRefreshTimer = null
        }
    }

    /**
     * 定时器 tick 执行逻辑
     */
    private fun onTimerTickerRefresh() {
        if (TempManager.sharedTempManager().tickerDataDirty) {
            TempManager.sharedTempManager().tickerDataDirty = false
            for (fragment in fragmens) {
                val fr = fragment as FragmentMarketInfo
                fr.onRefreshTickerData()
            }
        }
    }

    /**
     *  (private) 事件 - 刷新自选(关注、收藏)市场
     */
    private fun onRefreshFavoritesMarket() {
        if (TempManager.sharedTempManager().favoritesMarketDirty) {
            ChainObjectManager.sharedChainObjectManager().buildAllMarketsInfos()
            TempManager.sharedTempManager().favoritesMarketDirty = false
            for (fragment in fragmens) {
                val fr = fragment as FragmentMarketInfo
                fr.onRefreshFavoritesMarket()
            }
            ScheduleManager.sharedScheduleManager().autoRefreshTickerScheduleByMergedMarketInfos()
        }
    }

    private fun setAddBtnListener() {
       _binding.buttonAdd.setOnClickListener { goTo(ActivityTradingPairMgr::class.java, true) }
    }

    private fun setFragments() {
        fragmens.add(FragmentMarketInfo().initialize(null))
        fragmens.add(FragmentMarketInfo().initialize(JSONObject().apply { put("type", "all") }))
        ChainObjectManager.sharedChainObjectManager().getMergedMarketInfos().forEach { market: JSONObject ->
            fragmens.add(FragmentMarketInfo().initialize(market))
        }
    }
}
