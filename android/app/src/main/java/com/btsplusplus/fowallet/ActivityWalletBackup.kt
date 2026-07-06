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
import com.btsplusplus.fowallet.http.HttpConfig
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.framework.website.AssetsWebsite
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date

class ActivityWalletBackup : BtsppActivity() {

    private var _webserver: Server? = null
    private var _fullpath: String = ""
    private var _filename: String = ""
    private lateinit var _binding: ActivityWalletBackupBinding

    override fun onDestroy() {
        _webserver?.shutdown()
        _webserver = null
        super.onDestroy()
    }

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

    private fun startInitWebserver(context: Context) {
        if (_webserver != null) {
            return
        }
        val ipv4 = Utils.getIpv4Address(context)
        if (ipv4 == null) {
            _binding.labelTxtAddressOrError.text = R.string.registerLoginWebServerErrorIp.xmlstring(context)
            return
        }
        //  REMARK：不能绑定到80端口，会出现无权限错误。
        val port = 9999
        val address = InetAddress.getByName(ipv4)
        HttpConfig.website = AssetsWebsite(context, "/www/${R.string.webserverDownloadPage.xmlstring(context)}")
        
        _webserver = AndServer.webServer(context).port(port).inetAddress(address).listener(object : Server.ServerListener {
            override fun onStarted() {
                _binding.labelTxtAddressOrError.text = "${ipv4}:${port}"
            }

            override fun onException(e: Exception) {
                btsppLogCustom("webserver_download_init_error", jsonObjectfromKVS("message", e.message
                    ?: "unknown"))
                _binding.labelTxtAddressOrError.text = R.string.registerLoginWebServerErrorInit.xmlstring(context)
            }

            override fun onStopped() {
            }
        }).build()
        _webserver!!.startup()
    }
}
