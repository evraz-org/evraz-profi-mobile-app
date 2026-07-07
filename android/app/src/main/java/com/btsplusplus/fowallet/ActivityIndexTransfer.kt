package com.btsplusplus.fowallet

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import bitshares.Promise
import bitshares.Utils
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityIndexTransferBinding
import com.fowallet.walletcore.bts.WalletManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

class ActivityIndexTransfer : BtsppActivity(), OnTouchListener, Handler.Callback {

    companion object {
        const val CLICK_ON_WEBVIEW: Int = 1
        const val CLICK_ON_URL: Int = 2
    }

    private var mFull_account_data: JSONObject? = null
    private var mDefault_asset: JSONObject? = null
    private var mDefault_to: JSONObject? = null

    private lateinit var mMask: ViewMask

    private lateinit var mBnding: ActivityIndexTransferBinding
    private val mHandler: Handler = Handler(Looper.getMainLooper(),this)
    private var symbolList: MutableList<String?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBnding = ActivityIndexTransferBinding.inflate(layoutInflater)
        setAutoLayoutContentView(mBnding.root)
        setFullScreen()
        setBottomNavigationStyle(mBnding.bottomNav,2)

        mMask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)

        val args = btspp_args_as_JSONObject()
        mFull_account_data = args.getJSONObject("full_account_data")
        mDefault_asset = args.optJSONObject("default_asset")
        mDefault_to = args.optJSONObject("default_to")

        val accountName = mFull_account_data?.getJSONObject("account")?.getString("name") ?: ""
        mBnding.editTextFrom.setText(accountName)

        fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
        val sha256Name = NativeInterface.sharedNativeInterface().sha256(accountName.toByteArray(Charsets.UTF_8)).toHex()
        loadWebView(mBnding.webViewAvatarFrom, 40, sha256Name)

        @SuppressLint("ClickableViewAccessibility")
        mBnding.webViewAvatarFrom.setOnTouchListener(this)

        val id = mFull_account_data?.getJSONObject("account")?.getString("id")?.replace(".", "") ?: ""
        @SuppressLint("SetTextI18n")
        mBnding.textViewFromId.text = "#$id"

        mBnding.btnSend.setOnClickListener {
             processSendClick()
        }

        mBnding.editTextTo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
                val sha256Name = NativeInterface.sharedNativeInterface().sha256(s.toString().toByteArray(Charsets.UTF_8)).toHex()
                loadWebView(mBnding.webViewAvatarTo, 40, sha256Name)
            }
        })

        mBnding.qrScan.setOnClickListener {
            val resultPromise = Promise()
            goTo(ActivityQrScan::class.java, true, args = JSONObject().apply {
                put("result_promise", resultPromise)
            })
            resultPromise.then {
                (it as? String)?.let { scanData ->
                    if (scanData.startsWith("btswallet")) {
                       /* val splited: Array<String?> = scanData.substring(9).split("'".toRegex())
                            .dropLastWhile { sp -> sp.isEmpty() }.toTypedArray()
                       mBnding.editTextTo.setText(splited[0])
                        mBnding.editTextQuantity.setText(splited[1])
                        val index: Int = 0 // symbolList!.indexOf(splited[2])
                        if (index >= 0) {
                            mBnding.spinnerUnit.setSelection(index)
                            mBnding.spinnerFeeUnit.setSelection(index)
                        } else {
                            Toast.makeText(this, R.string.no_req_token, Toast.LENGTH_SHORT).show()
                        }*/
                    } else {
                       /* val invoice: Invoice = Invoice.fromQrCode(scanData)
                        mBnding.editTextTo.setText(invoice.getTo())
                        mBnding.editTextQuantity.setText(java.lang.String.valueOf(invoice.getLineItems()[0].getPrice()))
                        var asset: String = invoice.getCurrency().toUpperCase()
                        if (asset.startsWith("BIT")) asset = asset.substring(3)
                        val index = symbolList.indexOf(asset)
                        if (index >= 0) {
                            mSpinner.setSelection(index)
                            feeSpinner.setSelection(index)
                        } else {
                            Toast.makeText(this, R.string.no_req_token, Toast.LENGTH_SHORT).show()*/
                        }
                    }
                }
            }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView(webView: WebView, size: Int, encryptText: String?) {
        val htmlShareAccountName =
            "<html><head><style>body,html {margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=$size,user-scalable=no/></head><body><canvas width=$size height=$size data-jdenticon-hash=$encryptText></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>"
        val webSettings = webView.getSettings()
        webSettings.javaScriptEnabled = true
        webView.loadData(htmlShareAccountName, "text/html", "UTF-8")
    }

    private fun processSendClick() {
        if (WalletManager.sharedWalletManager().isLocked()) {
            val builder = AlertDialog.Builder(this)
            val layoutInflater = layoutInflater
            val viewGroup: View = layoutInflater.inflate(R.layout.dialog_password_confirm, null)
            builder.setPositiveButton(
                R.string.password_confirm_button_confirm,
                null
            )

            builder.setNegativeButton(
                R.string.password_confirm_button_cancel
            ) { _, _ -> }
        builder.setView(viewGroup)
            val dialog = builder.create()
            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val editText =
                        viewGroup.findViewById<View?>(R.id.editTextPassword) as EditText
                    val strPassword = editText.getText().toString()
                    val nRet = WalletManager.sharedWalletManager().unLock(strPassword, this)
                    if (nRet.getString("err") == "ok") {
                        dialog.dismiss()
                        val strFrom = mBnding.editTextFrom.getText().toString()
                        val strTo = mBnding.editTextTo.getText().toString()
                        val strQuantity = mBnding.editTextQuantity.getText().toString()
                        val strSymbol = mBnding.spinnerUnit.getSelectedItem() as String?
                        val strFeeSymbol = mBnding.spinnerFeeUnit.getSelectedItem() as String?
                        val strMemo = mBnding.editTextMemo.getText().toString()
                        processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo, strFeeSymbol)
                    } else {
                        viewGroup.findViewById<View?>(R.id.textViewPasswordInvalid)?.visibility = View.VISIBLE
                    }
                }
    } else {
            val strFrom = mBnding.editTextFrom.getText().toString()
            val strTo = mBnding.editTextTo.getText().toString()
            val strQuantity = mBnding.editTextQuantity.getText().toString()

            val strSymbol = mBnding.spinnerUnit.getSelectedItem() as String?
            val strFeeSymbol = mBnding.spinnerFeeUnit.getSelectedItem() as String?
            val strMemo = mBnding.editTextMemo.getText().toString()

            processTransfer(strFrom, strTo, strQuantity, strSymbol, strMemo, strFeeSymbol)
        }
    }

    private fun processTransfer(strFrom: String?, strTo: String?, strQuantity: String?, strSymbol: String?,  strMemo: String?,  strFeeSymbol: String?) {
        print("")
    }

    private fun generateQR() {
        mMask.show()
        Thread(Runnable {
            val accountName = mFull_account_data?.getJSONObject("account")?.getString("name") ?: ""
            val data = "btswallet$accountName'0' "
            val writer = QRCodeWriter()
            try {
                val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 1000, 1000)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0..<width) {
                    for (y in 0..<height) {
                        bmp[x, y] = if (bitMatrix.get(x, y)) 0xFF303F9F.toInt() else 0xFF000000.toInt()                    }
                }
                Handler(Looper.getMainLooper()).post(Runnable {
                    val imageView = ImageView(this)
                    imageView.setImageBitmap(bmp)

                    val dialog: AlertDialog = AlertDialog.Builder(this)
                        .setView(imageView)
                        .setTitle("")
                        .setNeutralButton(R.string.label_ok, null)
                        .setPositiveButton(
                            R.string.share
                        ) { _: DialogInterface?, i: Int ->
                            Utils.shareImage(this, bmp)
                        }
                        .create()

                    dialog.show()
                    mMask.dismiss()
                })
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }).start()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        if (view?.id == R.id.webViewAvatarFrom && motionEvent?.action == MotionEvent.ACTION_DOWN) {
            mHandler.sendEmptyMessageDelayed(CLICK_ON_WEBVIEW, 500)
        }
        return false
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == CLICK_ON_URL) {
            mHandler.removeMessages(CLICK_ON_WEBVIEW)
            return true
        }
        if (msg.what == CLICK_ON_WEBVIEW) {
            generateQR()
            return true
        }
        return false
    }
}
