package com.btsplusplus.fowallet

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.webkit.WebViewClient
import com.btsplusplus.fowallet.databinding.ActivityWebviewBinding

class ActivityWebView : BtsppActivity() {

    private lateinit var _binding: ActivityWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityWebviewBinding.inflate(layoutInflater)

        val params = _btspp_params as Array<String>
        assert(params.size == 2)

        setAutoLayoutContentView(_binding.root)

        //  设置标题
        _binding.titleOfWebview.text = params[0]

        //  设置webview参数
        _binding.webView.setBackgroundColor(Color.TRANSPARENT)
        _binding.webView.setPadding(0, 0, 0, 0)
        _binding.webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        _binding.webView.webViewClient = WebViewClient()
        val setting = _binding.webView.settings
        setting.cacheMode = LOAD_NO_CACHE
        setting.javaScriptEnabled = true
        setting.domStorageEnabled = true

        //  加载
        _binding.webView.loadUrl(params[1])

        _binding.layoutBackFromFaq.setOnClickListener { finish() }

        _binding.buttonRefreshOfWebview.setOnClickListener { _binding.webView.reload() }

        setFullScreen()
    }

}
