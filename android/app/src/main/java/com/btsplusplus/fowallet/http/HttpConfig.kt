package com.btsplusplus.fowallet.http

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite

@Config
class HttpConfig: WebConfig {
    companion object {
        var website: AssetsWebsite? = null
    }

    override fun onConfig(
        context: Context?,
        delegate: WebConfig.Delegate?
    ) {
        delegate?.addWebsite(website)
    }
}