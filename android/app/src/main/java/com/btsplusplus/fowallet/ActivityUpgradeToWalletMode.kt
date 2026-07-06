package com.btsplusplus.fowallet

import android.os.Bundle
import bitshares.*
import com.fowallet.walletcore.bts.WalletManager
import com.btsplusplus.fowallet.databinding.ActivityUpgradeToWalletModeBinding
import org.json.JSONObject

class ActivityUpgradeToWalletMode : BtsppActivity() {

    private lateinit var _result_promise: Promise
    private lateinit var _binding: ActivityUpgradeToWalletModeBinding

    /**
     * 系统返回键
     */
    override fun onBackPressed() {
        onBackClicked(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityUpgradeToWalletModeBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        setFullScreen()

        //  获取参数 / get params
        val args = btspp_args_as_JSONObject()
        _result_promise = args.get("result_promise") as Promise

        //  刷新UI
        refreshHeaderInfoUI()

        //  返回按钮事件
        _binding.layoutBackFromPageOfUpgradeToWalletModel.setOnClickListener { onBackClicked(false) }

        //  帮助按钮事件
        _binding.tipLinkWalletPasswordOfUpgradeToWallet.setOnClickListener {
            UtilsAlert.showMessageBox(this, R.string.kLoginRegTipsWalletPasswordFormat.xmlstring(this))
        }

        //  创建钱包按钮事件
        _binding.buttonCreateWalletOfUpgradeToWallet.setOnClickListener { onSubmitClicked() }
    }

    override fun onBackClicked(success: Any?) {
        _result_promise.resolve(success)
        finish()
    }

    private fun onSubmitClicked() {
        val password = _binding.tfPasswordOfUpgradeToWallet.text.toString()
        val wallet_password = _binding.tfWalletPasswordOfUpgradeToWallet.text.toString()

        if (password.isEmpty()) {
            showToast(resources.getString(R.string.kMsgPasswordCannotBeNull))
            return
        }

        if (!Utils.isValidBitsharesWalletPassword(wallet_password)) {
            showToast(R.string.kLoginSubmitTipsWalletPasswordFmtIncorrect.xmlstring(this))
            return
        }

        //  1、再次验证账号密码是否正确
        val full_account_data = WalletManager.sharedWalletManager().getWalletAccountInfo()!!
        val accountName = full_account_data.getJSONObject("account").getString("name")
        val walletMgr = WalletManager.sharedWalletManager()
        val currUnlockInfos = walletMgr.unLock(password, this)
        if (!(currUnlockInfos.getBoolean("unlockSuccess") && currUnlockInfos.optBoolean("haveActivePermission"))) {
            showToast(R.string.kLoginSubmitTipsAccountPasswordIncorrect.xmlstring(this))
            return
        }

        //  2、验证通过，开始创建钱包文件。
        val active_seed = "${accountName}active$password"
        val active_private_wif = OrgUtils.genBtsWifPrivateKey(active_seed.utf8String())
        val owner_seed = "${accountName}owner$password"
        val owner_private_wif = OrgUtils.genBtsWifPrivateKey(owner_seed.utf8String())
        val memo_seed = "${accountName}memo$password"
        val memo_private_wif = OrgUtils.genBtsWifPrivateKey(memo_seed.utf8String())
        val pub_key_owner = OrgUtils.genBtsAddressFromWifPrivateKey(owner_private_wif)
        val pub_key_active = OrgUtils.genBtsAddressFromWifPrivateKey(active_private_wif)
        val pub_key_memo = OrgUtils.genBtsAddressFromWifPrivateKey(memo_private_wif)

        //  3、创建钱包
        val status = walletMgr.createNewWallet(this, full_account_data, JSONObject().apply {
            put(pub_key_owner, owner_private_wif)
            put(pub_key_active, active_private_wif)
            put(pub_key_memo, memo_private_wif)
        }, false, null, wallet_password,
                AppCacheManager.EWalletMode.kwmPasswordWithWallet, "upgrade password+wallet")
        assert(status == EImportToWalletStatus.eitws_ok)

        //  转换成功 - 关闭界面

        //  返回 - 创建钱包完毕。
        showToast(R.string.kLblTipsConvertToWalletModeDone.xmlstring(this))
        onBackClicked(true)
    }

    private fun refreshHeaderInfoUI() {
        val full_account_data = WalletManager.sharedWalletManager().getWalletAccountInfo()!!
        val account_data = full_account_data.getJSONObject("account")
        _binding.accountNameOfUpgradeToWallet.text = account_data.getString("name")
        _binding.accountIdOfUpgradeToWallet.text = "#${account_data.getString("id").split(".").last()}"
    }

}