package com.btsplusplus.fowallet

import android.content.Context
import android.os.Bundle
import bitshares.AppCacheManager
import bitshares.OrgUtils
import bitshares.Utils
import bitshares.btsppLogCustom
import bitshares.hexDecode
import bitshares.jsonObjectfromKVS
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityWalletBackupBinding
import java.io.File
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class ActivityWalletBackup : BtsppActivity() {

    private var _fullpath: String = ""
    private var _filename: String = ""
    private lateinit var _binding: ActivityWalletBackupBinding
    private var _server : EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityWalletBackupBinding.inflate(layoutInflater)
        setAutoLayoutContentView(_binding.root)

        _binding.layoutBackFromBackupWallet.setOnClickListener { finish() }

        //  导出钱包
        if (backupWalletToWebdir()) {
            //  初始化webserver
            val nowifi = Utils.isWifi(this)
            if (!nowifi) {
                _binding.labelTxtAddressOrError.text = resources.getString(R.string.kBackupWalletOnlyViaWIFI)
            } else {
                startInitWebserver(this)
            }
        } else {
            _binding.labelTxtAddressOrError.text = resources.getString(R.string.registerLoginTipBackupError)
        }
    }

    override fun onDestroy() {
        _server?.stop()
        _server = null
        super.onDestroy()
    }

    /**
     *  (private) 备份钱包bin到web目录供用户下载。
     */
    private fun backupWalletToWebdir(): Boolean {
        val wallet_info = AppCacheManager.sharedAppCacheManager().getWalletInfo()
        val hex_wallet_bin = wallet_info.getString("kFullWalletBin")
        val wallet_bin = hex_wallet_bin.hexDecode()

        val prefix = SimpleDateFormat("yyyyMMdd").format(Date())
        _filename = "${prefix}_${wallet_info.getString("kAccountName")}.bin"
        _fullpath = "${OrgUtils.getAppDirWebServerImport()}${_filename}"

        //  [统计]
        btsppLogCustom("action_backupwallet", jsonObjectfromKVS("prefix", prefix, "account", wallet_info.optString("kAccountName", "")))

        return OrgUtils.write_file(_fullpath, wallet_bin)
    }

    private fun startInitWebserver(androidContext: Context) {
        if(_server != null) return

        val ipv4 = Utils.getIpv4Address(androidContext)
        if (ipv4 == null) {
            _binding.labelTxtAddressOrError.text = R.string.registerLoginWebServerErrorIp.xmlstring(androidContext)
            return
        }
        //  REMARK：不能绑定到80端口，会出现无权限错误。
        val port = 9999
        val address = InetAddress.getByName(ipv4)

        try {
           _server = embeddedServer(CIO, host = address.hostAddress!!, port = port) {
                routing {
                    get("/") {
                        val assetPath = "www/${R.string.webserverDownloadPage.xmlstring(androidContext)}/index.html"
                        try {
                            androidContext.assets.open(assetPath).use { inputStream ->
                                call.respondBytes(inputStream.readBytes(), ContentType.Text.Html)
                            }
                        } catch (e: Exception) {
                            call.respondText("Download page not found", status = HttpStatusCode.NotFound)
                        }
                    }

                    get("/download") {
                        val file = File(_fullpath)
                        if (file.exists()) {
                            call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$_filename\"")
                            call.respondFile(file)
                        } else {
                            call.respondText("Wallet file not found", status = HttpStatusCode.NotFound)
                        }
                    }
                }
            }.start(wait = false)
            _binding.labelTxtAddressOrError.text = "${ipv4}:${port}"
        } catch (e: Exception) {
            btsppLogCustom("webserver_download_init_error", jsonObjectfromKVS("message", e.message
                ?: "unknown"))
            _binding.labelTxtAddressOrError.text = R.string.registerLoginWebServerErrorInit.xmlstring(androidContext)
        }
    }
}
