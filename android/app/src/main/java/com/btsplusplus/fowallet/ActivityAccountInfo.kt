package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.view.animation.OvershootInterpolator
import androidx.viewpager.widget.ViewPager
import com.btsplusplus.fowallet.databinding.ActivityAccountInfoBinding
import com.google.android.material.tabs.TabLayout
import java.lang.reflect.Field

class ActivityAccountInfo : BtsppActivity() {

    private val fragmens: ArrayList<Fragment> = ArrayList()
    private var tablayout: TabLayout? = null
    private var view_pager: ViewPager? = null

    private lateinit var binding: ActivityAccountInfoBinding

    override fun onBackClicked(result: Any?) {
        super.onBackClicked(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAutoLayoutContentView(R.layout.activity_account_info)
        setFullScreen()

        binding = ActivityAccountInfoBinding.bind(findViewById<View>(android.R.id.content).rootView)

        // 设置 tablelayout 和 view_pager
        tablayout = binding.tablayoutOfAccountInfo
        view_pager = binding.viewPagerOfAccountInfo

        // 添加 fargments
        setFragments()

        // 设置 viewPager 并配置滚动速度
        setViewPager()

        // 监听 tab 并设置选中 item
        setTabListener()

        //  返回
        binding.layoutBackFromAccountDetail.setOnClickListener { onBackClicked(null) }
    }


    private fun setViewPager() {
        view_pager!!.adapter = ViewPagerAdapter(super.getSupportFragmentManager(), fragmens)
        val f: Field = ViewPager::class.java.getDeclaredField("mScroller")
        f.isAccessible = true
        val vpc: ViewPagerScroller = ViewPagerScroller(view_pager!!.context, OvershootInterpolator(0.6f))
        f.set(view_pager, vpc)
        vpc.duration = 700

        view_pager!!.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                println(position)
                tablayout!!.getTabAt(position)!!.select()
            }
        })
    }

    private fun setFragments() {
        fragmens.add(FragmentUserBaseInfo())
        fragmens.add(FragmentUserMemberInfo())
        fragmens.add(FragmentPermissionList())
    }

    private fun setTabListener() {
        tablayout!!.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val pos = tab.position
                view_pager!!.setCurrentItem(tab.position, true)
                fragmens[pos].let {
                    if (it is FragmentPermissionList) {
                        it.refreshCurrAccountData()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                //tab未被选择的时候回调
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                //tab重新选择的时候回调
            }
        })
    }

}
