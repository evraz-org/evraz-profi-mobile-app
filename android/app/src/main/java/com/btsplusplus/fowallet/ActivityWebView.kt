package com.btsplusplus.fowallet

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebViewClient
import com.btsplusplus.fowallet.databinding.ActivityWebViewBinding

class ActivityWebView : BtsppActivity() {

    private lateinit var binding: ActivityWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val params = _btspp_params as Array<String>
        assert(params.size == 2)

        setAutoLayoutContentView(R.layout.activity_webview)
        binding = ActivityWebViewBinding.bind(findViewById<View>(android.R.id.content).rootView)

        //  设置标题
        binding.titleOfWebview.text = params[0]

        //  设置webview参数
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
        binding.webView.setPadding(0, 0, 0, 0)
        binding.webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        binding.webView.webViewClient = WebViewClient()
        val setting = binding.webView.settings
        setting.cacheMode = LOAD_NO_CACHE
        setting.javaScriptEnabled = true
        setting.domStorageEnabled = true

        //  加载
        binding.webView.loadUrl(params[1])

        binding.layoutBackFromFaq.setOnClickListener { finish() }

        binding.buttonRefreshOfWebview.setOnClickListener { binding.webView.reload() }

        setFullScreen()
    }

}
