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
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import bitshares.EBitsharesOperations
import bitshares.OrgUtils
import bitshares.Promise
import bitshares.Utils
import bitshares.forin
import bitshares.jsonObjectfromKVS
import bitshares.multiplyByPowerOf10
import bitshares.toJSONArray
import bitshares.xmlstring
import com.btsplusplus.fowallet.databinding.ActivityIndexTransferBinding
import com.fowallet.walletcore.bts.BitsharesClientManager
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Double.parseDouble
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow

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
    private var mAssetList: MutableList<Map<String, String>> = mutableListOf()
    private lateinit var assetAdapter: ArrayAdapter<String>
    private lateinit var feeAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBnding = ActivityIndexTransferBinding.inflate(layoutInflater)
        setAutoLayoutContentView(mBnding.root)
        setFullScreen()
        setBottomNavigationStyle(mBnding.bottomNav, 2)

        mMask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)

        val args = btspp_args_as_JSONObject()
        mFull_account_data = args.getJSONObject("full_account_data")
        mDefault_asset = args.optJSONObject("default_asset")
        mDefault_to = args.optJSONObject("default_to")

        val accountName = mFull_account_data?.getJSONObject("account")?.optString("name") ?: ""
        mBnding.editTextFrom.setText(accountName)

        fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
        val sha256Name =
            NativeInterface.sharedNativeInterface().sha256(accountName.toByteArray(Charsets.UTF_8))
                .toHex()
        loadWebView(mBnding.webViewAvatarFrom, 40, sha256Name)

        @SuppressLint("ClickableViewAccessibility")
        mBnding.webViewAvatarFrom.setOnTouchListener(this)

        val id =
            mFull_account_data?.getJSONObject("account")?.optString("id")?.replace(".", "") ?: ""
        @SuppressLint("SetTextI18n")
        mBnding.textViewFromId.text = "#$id"

        mBnding.btnSend.setOnClickListener {
            processSendClick()
        }

        mBnding.editTextTo.onFocusChangeListener = { _, hasFocus ->
            val strText = mBnding.editTextTo.getText().toString()
            if (!hasFocus) {
                processGetTransferToId(strText)
            }
        }

        mBnding.editTextTo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
                val sha256Name = NativeInterface.sharedNativeInterface()
                    .sha256(s.toString().toByteArray(Charsets.UTF_8)).toHex()
                loadWebView(mBnding.webViewAvatarTo, 40, sha256Name)
                processGetTransferToId(mBnding.editTextTo.text.toString())
            }
        })

        mBnding.qrScan.setOnClickListener {
            processQrScanClick()
        }

        mBnding.editTextQuantity.onFocusChangeListener = { _, hasFocus ->
            if (!hasFocus) {
                val strQuantity = mBnding.editTextQuantity.text?.toString().orEmpty()
                if (strQuantity.isNotEmpty()) {
                    val quantity = Utils.parseDouble(strQuantity, -1.0)
                    if (quantity >= 0.0) {
                        processCalculateFee()
                    } else {
                        mBnding.editTextQuantity.setText("0")
                    }
                }
            }
        }

        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val userAssetDetailInfos = OrgUtils.calcUserAssetDetailInfos(mFull_account_data ?: JSONObject())
        val validBalancesHash = userAssetDetailInfos.getJSONObject("validBalancesHash").keys().toJSONArray()
        chainMgr.queryAllAssetsInfo(validBalancesHash).then {
            val bitasset_data_id_list = JSONArray()
            for (asset_id in validBalancesHash.forin<String>()) {
                val bitasset_data_id = chainMgr.getChainObjectByID(asset_id!!).optString("bitasset_data_id")
                if (bitasset_data_id.isNotEmpty()) {
                    bitasset_data_id_list.put(bitasset_data_id)
                }
            }
            return@then chainMgr.queryAllGrapheneObjects(bitasset_data_id_list).then {
                val balancesHash = userAssetDetailInfos.getJSONObject("validBalancesHash")
                for (k in balancesHash.keys()) {
                    val balanceItem = balancesHash.getJSONObject(k)
                    val asset_type = balanceItem.getString("asset_type")
                    val balance = balanceItem.get("balance")
                    val asset_detail = chainMgr.getChainObjectByID(asset_type)
                    if(parseDouble(balance.toString()) > 0) {
                        val asset = mutableMapOf<String, String>()
                        asset["symbol"] = asset_detail.getString("symbol")
                        asset["balance"] = balance.toString()
                        asset["precision"] = asset_detail.getString("precision")
                        mAssetList.add(asset)
                    }
                }
                var selectedItem = mBnding.spinnerUnit.getSelectedItem() as String?
                selectedItem = selectedItem ?: getString(R.string.label_evraz)

                assetAdapter = object : ArrayAdapter<String>(
                    this,
                    R.layout.new_custom_spinner_item,
                    mAssetList.map {it["symbol"]}.toTypedArray()
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        view.findViewById<View>(R.id.text1).isSelected = true
                        return view
                    }
                }

                assetAdapter.setDropDownViewResource(R.layout.new_spinner_style)
                mBnding.spinnerUnit.setAdapter(assetAdapter)

                var position = assetAdapter.getPosition(selectedItem)
                position = if (position < 0) 0 else position
                mBnding.spinnerUnit.setSelection(position)

                val item = mAssetList[position]
                mBnding.editTextAvailable.text = calcBalance(item["balance"], item["precision"])

                feeAdapter = object : ArrayAdapter<String>(
                    this,
                    R.layout.new_custom_spinner_item,
                    mAssetList.map {it["symbol"]}.toTypedArray()
                ) {}

                selectedItem = mBnding.spinnerFeeUnit.getSelectedItem() as String?
                selectedItem = selectedItem ?: getString(R.string.label_evraz)

                feeAdapter.setDropDownViewResource(R.layout.new_spinner_style)
                mBnding.spinnerFeeUnit.setAdapter(feeAdapter)

                position = feeAdapter.getPosition(selectedItem)
                position = if (position < 0) 0 else position
                mBnding.spinnerFeeUnit.setSelection(position)

                processCalculateFee()

                return@then null
            }
        }

        mBnding.spinnerUnit.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                processCalculateFee()
                val item = mAssetList[i]
                mBnding.editTextAvailable.text = calcBalance(item["balance"], item["precision"])
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        })

        mBnding.spinnerFeeUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                processCalculateFee()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }
        }

     mBnding.editTextQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                processLock()
            }
        })

        mBnding.editTextFee.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {
            }

            override fun afterTextChanged(editable: Editable?) {
                processLock()
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (mBnding.editTextTo.text.isNotEmpty()) {
            processGetTransferToId(mBnding.editTextTo.text.toString())
        }
    }

    fun calcBalance(balance: String?, precision: String?): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val decimalFormat = DecimalFormat("#.#####", symbols)

        val p = parseDouble(precision!!)
        return decimalFormat.format(parseDouble(balance!!) / 10.0.pow(p))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadWebView(webView: WebView, size: Int, encryptText: String?) {
        val htmlShareAccountName =
            "<html><head><style>body,html {margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=$size,user-scalable=no/></head><body><canvas width=$size height=$size data-jdenticon-hash=$encryptText></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>"
        val webSettings = webView.getSettings()
        webSettings.javaScriptEnabled = true
        webView.loadData(htmlShareAccountName, "text/html", "UTF-8")
    }

    private fun processLock() {
        mBnding.btnSend.setEnabled(getSendAvailable())

        if (getSendAvailable()) {
            mBnding.editTextAvailable.setTextColor(ContextCompat.getColor(this, R.color.beige_color))
            mBnding.btnSend.background = ContextCompat.getDrawable(this, R.drawable.btn_green_background)
        } else {
            mBnding.editTextAvailable.setTextColor(ContextCompat.getColor(this, R.color.label_red))
            mBnding.btnSend.background = ContextCompat.getDrawable(this, R.drawable.btn_red_background)
        }
    }

    private fun getSendAvailable(): Boolean {
        val available: Double = getAvailable()
        val amount: Double = getAmount()
        val fee: Double = getFee()
        if (mBnding.spinnerUnit.getSelectedItem() === mBnding.spinnerFeeUnit.getSelectedItem()) {
            val diff: Double = available - Utils.sumDouble(amount, fee)
            return diff >= 0.0
        } else {
            val diff = available - amount
            return diff >= 0.0
        }
    }

    private fun getAvailable(): Double {
        return parseDouble( mBnding.editTextAvailable.text.toString())
    }

    private fun getFee(): Double {
        return try {
            return parseDouble(mBnding.editTextFee.text.toString())
        } catch(_: Exception) {
            0.0
        }
    }

    private fun getAmount(): Double {
        return try {
            parseDouble(mBnding.editTextQuantity.text.toString())
        } catch(_: Exception) {
            0.0
        }
    }

    private fun processCalculateFee() {
        var strQuantity = mBnding.editTextQuantity.getText().toString()
        val strAssetSymbol = mBnding.spinnerUnit.getSelectedItem() as String? ?: ""
        val strFeeAssetSymbol = mBnding.spinnerFeeUnit.getSelectedItem() as String? ?: ""
        //val strMemo = mBnding.editTextMemo.getText().toString()

        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val asset = chainMgr.getAssetBySymbol(strAssetSymbol)
        val feeAsset = chainMgr.getAssetBySymbol(strFeeAssetSymbol)

        strQuantity = Utils.auxGetStringDecimalNumberValue(strQuantity).multiplyByPowerOf10(asset.getInt("precision")).toPlainString()

        val id = mFull_account_data?.getJSONObject("account")?.optString("id")

        val op = JSONObject().apply {
            put("fee", jsonObjectfromKVS("amount", "0", "asset_id", feeAsset.getString("id")))
            put("from", id)
            put("to", id)
            put("amount", jsonObjectfromKVS("amount", strQuantity, "asset_id", asset.getString("id")))
            put("memo", null)
        }

        BitsharesClientManager.sharedBitsharesClientManager().calcOperationFee(op, EBitsharesOperations.ebo_transfer).then { data ->
            if(data is JSONObject) {
                processDisplayFee(data.getString("amount"), feeAsset.getString("precision"))
            }
        }
    }

    private fun processDisplayFee(fee: String, precision: String) {
        val symbols = DecimalFormatSymbols(Locale.US)
        val decimalFormat = DecimalFormat("#.#####", symbols)
        val str = decimalFormat.format(parseDouble(fee) / 10.0.pow(parseDouble(precision)))
        mBnding.editTextFee.setText(str)
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
                    if (nRet.optString("err") == "ok") {
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

    private fun processQrScanClick() {
        val resultPromise = Promise()
        goTo(ActivityQrScan::class.java, true, args = JSONObject().apply {
            put("result_promise", resultPromise)
        })
        resultPromise.then {
            (it as? String)?.let { scanData ->
                if (scanData.startsWith("btswallet")) {
                    val splited: Array<String?> = scanData.substring(9).split("'".toRegex())
                        .dropLastWhile { sp -> sp.isEmpty() }.toTypedArray()
                    mBnding.editTextTo.setText(splited[0])
                    mBnding.editTextQuantity.setText(splited[1])
                    val index = mAssetList.map { m -> m["symbol"] }.toTypedArray().indexOf(splited[2]) ?: -1
                    if (index >= 0) {
                        mBnding.spinnerUnit.setSelection(index)
                        mBnding.spinnerFeeUnit.setSelection(index)
                    } else {
                        Toast.makeText(this, R.string.no_req_token, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val invoice = OrgUtils.merchantInvoiceDecode(scanData)
                    mBnding.editTextTo.setText(invoice?.optString("to"))
                    mBnding.editTextQuantity.setText(
                        invoice?.optJSONArray("line_items")?.optJSONObject(0)
                            ?.optString("price")
                    )
                    var asset = invoice?.optString("currency")?.uppercase() ?: ""
                    if (asset.startsWith("BIT")) asset = asset.substring(3)
                    val index = mAssetList.map { m -> m["symbol"] }.toTypedArray().indexOf(asset) ?: -1
                    if (index >= 0) {
                        mBnding.spinnerUnit.setSelection(index)
                        mBnding.spinnerFeeUnit.setSelection(index)
                    } else {
                        Toast.makeText(this, R.string.no_req_token, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processTransfer(strFrom: String?, strTo: String?, strQuantity: String?, strSymbol: String?,  strMemo: String?,  strFeeSymbol: String?) {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val asset = chainMgr.getAssetBySymbol(strSymbol!!)
        val feeAsset = chainMgr.getAssetBySymbol(strFeeSymbol!!)
        val quantity = Utils.auxGetStringDecimalNumberValue(strQuantity!!).multiplyByPowerOf10(asset.getInt("precision")).toPlainString()

        val fromID = mFull_account_data?.getJSONObject("account")?.optString("id")
        mMask.show()
        ChainObjectManager.sharedChainObjectManager().queryAccountData(strTo!!)
            .then { accountObject ->
                if (accountObject is JSONObject) {
                    val toID = accountObject.optString("id")

                    val op = JSONObject().apply {
                        put("fee", jsonObjectfromKVS("amount", "0", "asset_id", feeAsset.getString("id")))
                        put("from", fromID)
                        put("to", toID)
                        put("amount", jsonObjectfromKVS("amount", quantity, "asset_id", asset.getString("id")))
                        put("memo", null)
                    }

                    BitsharesClientManager.sharedBitsharesClientManager().transfer(op).then {
                        val txData = it as? JSONArray
                        mMask.dismiss()
                        if (txData != null) {
                            Toast.makeText(this, R.string.kVcTransferTipTxTransferFullOK.xmlstring(this), Toast.LENGTH_LONG)
                                .show()
                            mBnding.editTextAvailable.text = calcBalance(asset.getString("balance"), asset.getString("precision"))
                        } else {
                            Toast.makeText(this, R.string.transfer_fail.xmlstring(this), Toast.LENGTH_LONG).show()
                        }
                        return@then null
                    }.catch { err ->
                        mMask.dismiss()
                        showGrapheneError(err)
                    }
                } else {
                    mMask.dismiss()
                }
            }
    }

    private fun processGetTransferToId(strAccount: String) {
        @SuppressLint("SetTextI18n")
        ChainObjectManager.sharedChainObjectManager().queryAccountData(strAccount)
            .then { accountObject ->
                if (accountObject is JSONObject) {
                    val id = accountObject?.optString("id")?.replace(".", "") ?: ""
                    mBnding.textViewToId.text = "#$id"
                } else {
                    mBnding.textViewToId.text = "#none"
                }
            }
    }

    private fun generateQR() {
        mMask.show()
        Thread {
            val accountName = mFull_account_data?.optJSONObject("account")?.optString("name") ?: ""
            val data = "btswallet$accountName'0' "
            val writer = QRCodeWriter()
            try {
                val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 1000, 1000)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0..<width) {
                    for (y in 0..<height) {
                        bmp[x, y] =
                            if (bitMatrix.get(x, y)) 0xFF303F9F.toInt() else 0xFF000000.toInt()
                    }
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
        }.start()
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
