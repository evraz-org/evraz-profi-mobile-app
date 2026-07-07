package com.btsplusplus.fowallet

import android.os.Bundle
import androidx.core.content.ContextCompat
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityIndexMyBinding
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.WalletManager

class ActivityIndexMy : BtsppActivity() {

    private lateinit var _binding: ActivityIndexMyBinding

    /**
     * 重载 - 返回键按下
     */
    override fun onBackPressed() {
        goHome()
    }

    override fun onResume() {
        super.onResume()
        _refreshFaceUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityIndexMyBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root, navigationBarColor = R.color.theme01_tabBarColor)

        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  设置底部导航栏样式
        setBottomNavigationStyle(_binding.bottomNav, 0)

        //  设置图标颜色
        val iconcolor = ContextCompat.getColor(applicationContext,R.color.theme01_textColorNormal)
        _binding.imgIconAvatar.setColorFilter(iconcolor)
        _binding.imgIconAssets.setColorFilter(iconcolor)
        _binding.imgIconOrders.setColorFilter(iconcolor)
        _binding.imgIconWallet.setColorFilter(iconcolor)
        _binding.imgIconProposal.setColorFilter(iconcolor)
        _binding.imgIconAssetMgr.setColorFilter(iconcolor)
        _binding.imgIconFaq.setColorFilter(iconcolor)
        _binding.imgIconShareLink.setColorFilter(iconcolor)
        _binding.imgIconSetting.setColorFilter(iconcolor)

        //  刷新UI
        _refreshFaceUI()

        //  需要判断登录
        _binding.layoutMyTop.setOnClickListener {
            if (WalletManager.sharedWalletManager().isWalletExist()) {
                goTo(ActivityAccountInfo::class.java, true)
            } else {
                goTo(ActivityLogin::class.java, true)
            }
        }

        //  事件 - 分享链接
        _binding.layoutShareLink.setOnClickListener { _onShareLinkClicked() }

        //  事件 - 设置
        _binding.layoutSettingFromMy.setOnClickListener {
            val saveCurrLangCode = LangManager.sharedLangManager().currLangCode
            val result_promise = Promise()
            goTo(ActivitySetting::class.java, true, args = jsonObjectfromKVS("result_promise", result_promise))
            result_promise.then {
                if (LangManager.sharedLangManager().currLangCode != saveCurrLangCode) {
                    recreate()
                }
            }
        }

        //  [待处提案] 需要判断登录
        _binding.layoutMyProposalWaitingForProcess.setOnClickListener {
            guardWalletExist {
                goTo(ActivityProposal::class.java, true)
            }
        }

        //  资产管理
        _binding.layoutAssetMgr.setOnClickListener {
            guardWalletExist {
                goTo(ActivityAssetManager::class.java, true)
            }
        }

        //  [钱包 & 多签]
        _binding.layoutMyWalletAndMutiSignature.setOnClickListener {
            guardWalletExistWithWalletMode(resources.getString(R.string.kLblTipsPasswordModeNotSupportMultiSign)) {
                goTo(ActivityWalletManager::class.java, true)
            }
        }

        //  我的资产：需要钱包存在
        _binding.layoutMyAssetsOfMy.setOnClickListener {
            guardWalletExist {
                viewUserAssets(WalletManager.sharedWalletManager().getWalletAccountName()!!)
            }
        }

        //  订单管理：需要钱包存在
        _binding.layoutOrderManagementOfMy.setOnClickListener {
            guardWalletExist {
                val uid = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account").getString("id")
                viewUserLimitOrders(uid, null)
            }
        }

        _binding.layoutFaqFromMy.setOnClickListener {
            goToWebView(resources.getString(R.string.faq), "https://btspp.io/qa.html")
        }
    }

    private fun _onShareLinkClicked() {
        val value = VcUtils.genShareLink(this, true)
        if (Utils.copyToClipboard(this, value)) {
            showToast(resources.getString(R.string.kShareLinkCopied))
        }
    }

    private fun _refreshFaceUI() {
        val walletMgr = WalletManager.sharedWalletManager()
        if (walletMgr.isWalletExist()) {
            val account = walletMgr.getWalletAccountInfo()!!.getJSONObject("account")
            //  第一行
            val name = account.getString("name")
            if (walletMgr.isLocked()) {
                _binding.labelTxtAccoutname.text = "${name}(${R.string.kLblAccountLocked.xmlstring(this)})"
            } else {
                _binding.labelTxtAccoutname.text = "${name}(${R.string.kLblAccountUnlocked.xmlstring(this)})"
            }
            //  第二行
            if (Utils.isBitsharesVIP(account.optString("membership_expiration_date", ""))) {
                _binding.labelTxtStatus.text = "${R.string.kLblMembership.xmlstring(this)}${R.string.kLblMembershipLifetime.xmlstring(this)}"
            } else {
                _binding.labelTxtStatus.text = "${R.string.kLblMembership.xmlstring(this)}${R.string.kLblMembershipBasic.xmlstring(this)}"
            }
        } else {
            _binding.labelTxtAccoutname.text = R.string.kAccountManagement.xmlstring(this)
            _binding.labelTxtStatus.text = R.string.tip_click_to_login.xmlstring(this)
        }
    }
}