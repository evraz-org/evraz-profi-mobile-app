package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import android.widget.Button
import bitshares.Promise
import com.btsplusplus.fowallet.databinding.ActivityLoginBinding
import org.json.JSONObject

class ActivityLogin : BtsppActivity() {

    private var _checkActivePermission = true
    private var _resultPromise: Promise? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setAutoLayoutContentView(binding.root)

        //  读取参数
        val args = _btspp_params as? JSONObject
        if (args != null) {
            _checkActivePermission = args.getBoolean("checkActivePermission")
            _resultPromise = args.get("result_promise") as Promise
        }

        setFullScreen()

        //  事件 - 返回按钮
        binding.layoutBackFromLogin.setOnClickListener { onBackClicked(false) }

        //  初始化界面（部分界面在某些模式下不可见）
        if (_checkActivePermission) {
            binding.buttonRegister.setOnClickListener {
                goTo(ActivityRegisterEntry::class.java, true)
            }
        } else {
            findViewById<Button>(R.id.button_register).visibility = View.INVISIBLE
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FragmentLoginAccountMode().initWithCheckActivePermission(_checkActivePermission, _resultPromise))
            .commit()
    }

    /**
     * 事件 - 返回按钮或系统返回键点击。
     */
    override fun onBackClicked(result: Any?) {
        _resultPromise?.resolve(result)
        finish()
    }

}
