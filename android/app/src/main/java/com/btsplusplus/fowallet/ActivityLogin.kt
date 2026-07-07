package com.btsplusplus.fowallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import bitshares.AppCacheManager
import bitshares.EAccountPermissionStatus
import bitshares.OrgUtils
import bitshares.btsppLogCustom
import bitshares.jsonArrayfrom
import bitshares.jsonObjectfromKVS
import bitshares.utf8String
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityLoginBinding
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import kotlin.CharSequence
import kotlin.Exception
import kotlin.Int
import kotlin.text.isNullOrEmpty
import kotlin.toString

class ActivityLogin: BtsppActivity() {
    private inner class AccountNameWatcher: TextWatcher {
        private fun getStatus(accountObject: JSONObject): String {
            if (accountObject.getString("lifetime_referrer") == accountObject.getString("id")) return this@ActivityLogin.getString(R.string.lifetime)

            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

            try {
                val exp =
                    Objects.requireNonNull<Date>(sdf.parse(accountObject.getString("membership_expiration_date"))).time
                val now = Date().time
                if (exp < now) return this@ActivityLogin.getString(R.string.basic)
            } catch (ignored: Exception) {
            }
            return this@ActivityLogin.getString(R.string.annual)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int,  count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int,  before: Int,  count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if(s.isNullOrEmpty()) {
                mBidding.editTextAccountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                mBidding.textViewAccountInfo.visibility = View.GONE
            }
            ChainObjectManager.sharedChainObjectManager().queryAccountData(s.toString()).then { accountObject ->
                this@ActivityLogin.runOnUiThread {
                    if ((accountObject == null) || (accountObject !is JSONObject)) {
                        mBidding.editTextAccountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cross_circle, 0)
                        mBidding.textViewAccountInfo.setText(if (accountObject == null) R.string.import_activity_account_invalid else R.string.import_activity_connect_failed)
                        mBidding.textViewAccountInfo.setTextColor(ContextCompat.getColor(this@ActivityLogin,R.color.red))
                        mBidding.textViewAccountInfo.visibility = View.VISIBLE
                    } else {
                        mBidding.editTextAccountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.checkmark_circle, 0)
                        mBidding.textViewAccountInfo.text = String.format(
                            Locale.getDefault(),
                            getStatus(accountObject) + " #%s",
                            accountObject.getString("id").replace(".", "")
                        )
                        mBidding.textViewAccountInfo.setTextColor(ContextCompat.getColor(this@ActivityLogin,R.color.quotation_top_green))
                        mBidding.textViewAccountInfo.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private val mAccountNameWatcher = AccountNameWatcher()
    private lateinit var mBidding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBidding = ActivityLoginBinding.inflate(layoutInflater)
        setAutoLayoutContentView(mBidding.root)
        setFullScreen()

        mBidding.layoutBackFromLogin.setOnClickListener { finish() }

        mBidding.editTextAccountName.addTextChangedListener(mAccountNameWatcher)

        mBidding.buttonImport.setOnClickListener {
            processImport(mBidding.editTextAccountName.text.toString(), mBidding.editTextPassword.text.toString())
        }
    }

    private fun processImport(account_name: String, password: String) {
        if (account_name.isEmpty()) {
            mBidding.textViewErrorInfo.text = resources.getString(R.string.kLoginSubmitTipsAccountIsEmpty)
            return
        }
        if (password.isEmpty()) {
            mBidding.textViewErrorInfo.text = resources.getString(R.string.kMsgPasswordCannotBeNull)
            return
        }

        val username = account_name.lowercase()
        val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
        mask.show()
        ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(username).then {
            mask.dismiss()
            val full_data = it as? JSONObject
            if (full_data == null) {
                mBidding.textViewErrorInfo.text = resources.getString(R.string.kLoginSubmitTipsAccountIsNotExist)
                return@then null
            }

            val account_active = full_data.getJSONObject("account").getJSONObject("active")
            val active_seed = "${username}active$password"
            val calc_bts_active_address = OrgUtils.genBtsAddressFromPrivateKeySeed(active_seed)!!

            val status = WalletManager.calcPermissionStatus(account_active, jsonObjectfromKVS(calc_bts_active_address, true))
            if (status == EAccountPermissionStatus.EAPS_NO_PERMISSION) {
                mBidding.textViewErrorInfo.text = R.string.kLoginSubmitTipsAccountPasswordIncorrect.xmlstring(this)
            }
            if (status == EAccountPermissionStatus.EAPS_PARTIAL_PERMISSION) {
                mBidding.textViewErrorInfo.text = R.string.kLoginSubmitTipsAccountPasswordPermissionNotEnough.xmlstring(this)
            }

            val active_private_wif = OrgUtils.genBtsWifPrivateKey(active_seed.utf8String())
            val owner_seed = "${username}owner$password"
            val memo_seed = "${username}memo$password"
            val owner_private_wif = OrgUtils.genBtsWifPrivateKey(owner_seed.utf8String())
            val memo_private_wif = OrgUtils.genBtsWifPrivateKey(memo_seed.utf8String())

            val full_wallet_bin = WalletManager.sharedWalletManager().genFullWalletData(this, username, jsonArrayfrom(active_private_wif, owner_private_wif, memo_private_wif), password)
            assert(full_wallet_bin != null)

            AppCacheManager.sharedAppCacheManager().setWalletInfo(AppCacheManager.EWalletMode.kwmPasswordWithWallet.value, full_data, username, full_wallet_bin)
            AppCacheManager.sharedAppCacheManager().autoBackupWalletToWebdir(false)

            val unlockInfos = WalletManager.sharedWalletManager().unLock(password, this)
            assert(unlockInfos.getBoolean("unlockSuccess") && unlockInfos.optBoolean("haveActivePermission"))

            btsppLogCustom("loginEvent", jsonObjectfromKVS("mode", AppCacheManager.EWalletMode.kwmPasswordWithWallet.value, "desc", "password+wallet"))
            showToast(resources.getString(R.string.kLoginTipsLoginOK))
            setResult(RESULT_OK)
            finish()
        }.catch { err ->
            mask.dismiss()
            showGrapheneError(err)
        }
    }
}