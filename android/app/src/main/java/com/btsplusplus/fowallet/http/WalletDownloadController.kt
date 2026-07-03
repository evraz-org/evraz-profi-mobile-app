package com.btsplusplus.fowallet.http

import bitshares.AppCacheManager
import bitshares.OrgUtils
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.framework.body.FileBody
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@RestController
class WalletDownloadController {
    @GetMapping(path = ["/download"])
    fun download(request: HttpRequest, response: HttpResponse): FileBody {
        val walletInfo = AppCacheManager.sharedAppCacheManager().getWalletInfo()
        val prefix = SimpleDateFormat("yyyyMMdd").format(Date())
        val filename = "${prefix}_${walletInfo.getString("kAccountName")}.bin"
        response.addHeader("Content-Disposition", "attachment;filename=${filename}")
        return FileBody(File("${OrgUtils.getAppDirWebServerImport()}${filename}"))
    }
}