package com.btsplusplus.fowallet.http

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import bitshares.OrgUtils
import bitshares.delay_main
import com.btsplusplus.fowallet.BtsppApp
import com.btsplusplus.fowallet.R
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.http.multipart.MultipartFile
import java.io.File
import java.io.IOException

@RestController
class WalletUploadController {

    companion object {
        const val ACTION: String = "com.btsplusplus.fowallet.http.wallet.upload"
    }

    @PostMapping("/upload")
    fun upload(@RequestParam(name = "fileUpload") file: MultipartFile): String {
        val importDir = OrgUtils.getAppDirWebServerImport()
        val uploadedFile = File(importDir, file.filename)
        val uploadFileDir = File(importDir)
        if (!uploadFileDir.exists()) {
            uploadFileDir.mkdirs()
        }

        try {
            file.transferTo(uploadedFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        delay_main {
            LocalBroadcastManager.getInstance(BtsppApp.getInstance()).sendBroadcast(Intent(ACTION))
        }

        return BtsppApp.getInstance().resources.getString(R.string.registerLoginPageUploadSuccessPleaseContinueForPhone)
    }
}