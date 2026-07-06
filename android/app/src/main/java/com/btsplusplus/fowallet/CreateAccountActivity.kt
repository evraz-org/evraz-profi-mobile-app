package com.btsplusplus.fowallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import bitshares.OrgUtils
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityCreateAccountBinding
import com.fowallet.walletcore.bts.ChainObjectManager
import org.json.JSONObject

class CreateAccountActivity : BtsppActivity() {
    private lateinit var mMask: ViewMask
    private lateinit var mBinding: ActivityCreateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setAutoLayoutContentView(mBinding.root)
        setFullScreen()

        mBinding.layoutBack.setOnClickListener {
            finish()
        }

        mMask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)

        mBinding.buttonCreate.setOnClickListener {
            mMask.show()

            val strAccount = mBinding.editTextAccountName.text.toString()
            val strPassword = mBinding.editTextPassword.text.toString()
            val strPasswordConfirm = mBinding.editTextPasswordConfirm.text.toString()

            val checkBox = mBinding.checkBoxConfirm
            val checkBox2 = mBinding.checkBoxConfirm2
            val checkBox3 = mBinding.checkBoxConfirm3

            val textViewAccountError = mBinding.textViewErrorAccount
            val textViewPasswordError = mBinding.textViewErrorPasswrod
            val textViewPasswordConfirmError = mBinding.textViewErrorInfo

            var bError = false
            if (strAccount.isEmpty()) {
                textViewAccountError.setText(R.string.create_account_account_name_empty)
                bError = true
            }

            if (strPassword.isEmpty()) {
                textViewPasswordError.setText(R.string.create_account_password_empty)
                bError = true
            }

            if (strPasswordConfirm.isEmpty()) {
                textViewPasswordConfirmError.setText(R.string.create_account_password_confirm_empty)
                bError = true
            }

            if (!bError && textViewAccountError.text.isNullOrEmpty()) {
                if (checkBox.isChecked && checkBox2.isChecked && checkBox3.isChecked) {
                    processCreateAccount(strAccount, strPassword, strPasswordConfirm)
                } else {
                    val builder =
                        AlertDialog.Builder(this@CreateAccountActivity, R.style.CustomDialogTheme)
                    builder.setMessage(R.string.create_account_check_box_confirm)
                    builder.show()
                    mMask.dismiss()
                }
            } else {
                mMask.dismiss()
            }
        }

        mBinding.editTextAccountName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int,  count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int,  before: Int,  count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val strAccountName = s.toString()
                if (strAccountName.isEmpty()) {
                    return
                }

                val textViewError = mBinding.textViewErrorAccount
                if (!Character.isLetter(strAccountName.get(0))) {
                    textViewError.setText(R.string.create_account_account_name_error_start_letter)
                    mBinding.imageViewAccountCheck.visibility = View.INVISIBLE
                } else if (strAccountName.length <= 4) {  // 用户名太短
                    textViewError.setText(R.string.create_account_account_name_too_short)
                    mBinding.imageViewAccountCheck.visibility = View.INVISIBLE
                } else if (strAccountName.endsWith("-")) {
                    textViewError.setText(R.string.create_account_account_name_error_dash_end)
                    mBinding.imageViewAccountCheck.visibility = View.INVISIBLE
                } else {
                    var bCombineAccount = false
                    for (c in strAccountName.toCharArray()) {
                        if (Character.isLetter(c) == false) {
                            bCombineAccount = true
                        }
                    }

                    if (bCombineAccount == false) {
                        textViewError.setText(R.string.create_account_account_name_error_full_letter)
                        mBinding.imageViewAccountCheck.visibility = View.INVISIBLE
                    } else {
                        textViewError.text = ""
                        processCheckAccount(strAccountName)
                    }
                }

            }
        })

        mBinding.editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val strPassword = s.toString()

                if (strPassword.length < 12) {
                    mBinding.textViewErrorPasswrod.setText(R.string.create_account_password_requirement)
                } else {
                    val bDigit = strPassword.matches(".*\\d+.*".toRegex())
                    val bUpperCase = strPassword.matches(".*[A-Z]+.*".toRegex())
                    val bLowerCase = strPassword.matches(".*[a-z]+.*".toRegex())
                    if ((bDigit && bUpperCase && bLowerCase) == false) {
                        mBinding.textViewErrorPasswrod.setText(R.string.create_account_password_requirement)
                    } else {
                        mBinding.textViewErrorPasswrod.text = ""
                    }
                }
            }
        })

        mBinding.editTextPasswordConfirm.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val strPassword = mBinding.editTextPassword.getText().toString()
                val strPasswordConfirm = s.toString()

                if (strPassword.compareTo(strPasswordConfirm) == 0) {
                    mBinding.imageViewPasswordConfirmCheck.visibility = View.VISIBLE
                    mBinding.textViewErrorInfo.text = ""
                } else {
                    mBinding.imageViewPasswordConfirmCheck.visibility = View.GONE
                    mBinding.textViewErrorInfo.setText(R.string.create_account_password_confirm_error)
                }
            }
        })
    }

    private fun processCreateAccount(strAccount: String, strPassword: String, strPasswordConfirm: String) {
        if (strPassword.compareTo(strPasswordConfirm) != 0) {
            mBinding.textViewErrorInfo.setText(R.string.create_account_password_confirm_error)
            return
        }

        val seed_owner = "${strAccount}owner$strPassword"
        val seed_active = "${strAccount}active$strPassword"
        val seed_memo = "${strAccount}memo$strPassword"
        val owner_key = OrgUtils.genBtsAddressFromPrivateKeySeed(seed_owner)!!
        val active_key = OrgUtils.genBtsAddressFromPrivateKeySeed(seed_active)!!
        val memo_key = OrgUtils.genBtsAddressFromPrivateKeySeed(seed_memo)!!

        OrgUtils.asyncCreateAccountFromFaucet(this, strAccount, owner_key, active_key, memo_key, "", BuildConfig.kAppChannelID).then { error ->
            mMask.dismiss()
            if(error != null) {
                mBinding.textViewErrorAccount.text = error as String
            }
            else {
                goTo(ActivityIndexMy::class.java)
                finish()
            }
        }
    }

    private fun processCheckAccount(strAccount: String) {
        ChainObjectManager.sharedChainObjectManager().queryAccountData(strAccount).then { accountObject ->
            runOnUiThread {
                if ((accountObject == null) || (accountObject !is JSONObject)) {
                    mBinding.imageViewAccountCheck.visibility = View.VISIBLE
                } else {
                    val editTextAccount = mBinding.editTextAccountName
                    val strAccountName = editTextAccount.getText().toString()
                    val name = accountObject.getString("name")
                    if (strAccountName.compareTo(name) == 0) {
                        mBinding.textViewErrorAccount.setText(R.string.create_account_activity_account_object_exist)
                        mBinding.imageViewAccountCheck.visibility =  View.INVISIBLE
                    }
                }
            }
        }
    }
}