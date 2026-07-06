package com.btsplusplus.fowallet

import android.os.Bundle
import com.fowallet.walletcore.bts.ChainObjectManager
import com.btsplusplus.fowallet.databinding.ActivityRegisterEntryBinding
import org.json.JSONObject

class ActivityRegisterEntry : BtsppActivity() {

    private lateinit var _account_condition: ViewFormatConditons
    private lateinit var _binding: ActivityRegisterEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityRegisterEntryBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  初始化账号条件格式说明
        _binding.tfAccountName.let { tf ->
            _account_condition = ViewFormatConditons(this).apply {
                //  一直显示
                this.isAlwaysShow = true
                auxFastConditionsViewForAccountNameFormat()
                bindingTextField(tf)
            }
            _binding.layoutFormatViewContainer.addView(_account_condition)
        }

        //  事件 - 返回
        _binding.layoutBackFromRegisterEntry.setOnClickListener { finish() }

        //  事件 - 下一步
        _binding.btnNextStep.setOnClickListener { onNextButtonClicked() }
    }

    /**
     *  (private) 事件 - 下一步
     */
    private fun onNextButtonClicked() {
        //  检测参数有效性
        if (!_account_condition.isAllConditionsMatched()) {
            showToast(resources.getString(R.string.kLoginSubmitTipsAccountFmtIncorrect))
            return
        }
        val self = this
        val new_account_name = _binding.tfAccountName.text.toString().toLowerCase()
        val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
        ChainObjectManager.sharedChainObjectManager().isAccountExistOnBlockChain(new_account_name).then {
            mask.dismiss()
            if (it != null && it as Boolean) {
                showToast(resources.getString(R.string.kLoginSubmitTipsAccountAlreadyExist))
            } else {
                goTo(ActivityNewAccountPassword::class.java, true, args = JSONObject().apply {
                    put("args", new_account_name)
                    put("title", self.resources.getString(R.string.kVcTitleBackupYourPassword))
                    put("scene", kNewPasswordSceneRegAccount)
                })
            }
            return@then null
        }.catch {
            mask.dismiss()
            showToast(resources.getString(R.string.tip_network_error))
        }
    }
}
