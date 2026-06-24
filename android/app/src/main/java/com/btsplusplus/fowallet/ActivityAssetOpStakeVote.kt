package com.btsplusplus.fowallet

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityAssetOpStakeVoteBinding
import com.btsplusplus.fowallet.utils.ModelUtils
import com.fowallet.walletcore.bts.BitsharesClientManager
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

//  #ticket.hpp
const val liquid            = 0
const val lock_180_days     = 1
const val lock_360_days     = 2
const val lock_720_days     = 3
const val lock_forever      = 4
const val TICKET_TYPE_COUNT = 5

class ActivityAssetOpStakeVote : BtsppActivity() {

    companion object {

        fun getTicketTypeDesc(ticket_type: Int, ctx: Context): String {
            return when (ticket_type) {
                lock_180_days -> R.string.kVcAssetOpStakeVoteTicketTypeDesc180.xmlstring(ctx)
                lock_360_days -> R.string.kVcAssetOpStakeVoteTicketTypeDesc360.xmlstring(ctx)
                lock_720_days -> R.string.kVcAssetOpStakeVoteTicketTypeDesc720.xmlstring(ctx)
                lock_forever -> R.string.kVcAssetOpStakeVoteTicketTypeDescForever.xmlstring(ctx)
                else -> ""
            }
        }

    }

    private var _ticket_type = liquid                       //  锁仓类型
    private lateinit var _curr_asset: JSONObject            //  当前资产
    private var _full_account_data: JSONObject? = null      //  REMARK：提取手续费池等部分操作该参数为nil。
    private var _result_promise: Promise? = null
    private var _nCurrBalance = BigDecimal.ZERO
    private lateinit var _tf_amount_watcher: UtilsDigitTextWatcher

    private lateinit var binding: ActivityAssetOpStakeVoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  设置自动布局
        setAutoLayoutContentView(R.layout.activity_asset_op_stake_vote)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        binding = ActivityAssetOpStakeVoteBinding.bind(findViewById<View>(android.R.id.content).rootView)

        //  获取参数
        val args = btspp_args_as_JSONObject()
        _curr_asset = args.getJSONObject("current_asset")
        _full_account_data = args.optJSONObject("full_account_data")
        _result_promise = args.opt("result_promise") as? Promise

        _auxGenCurrBalanceAndBalanceAsset()

        //  初始化UI
        drawUI_title()
        drawUI_once()
        drawUI_currAsset()
        drawUI_ticketType(binding_event = true)
        drawUI_balance(false)

        //  事件 - 全部
        binding.btnTfTailerAll.setOnClickListener { onSelectAllClicked() }

        //  事件 - 提交
        binding.btnOpSubmit.setOnClickListener { onSubmitClicked() }

        //  事件 - 返回
        binding.layoutBackFromAssetsOpCommon.setOnClickListener { finish() }

        //  输入框 TODO:7.0 如果切换资产则需要切换精度
        val tf = findViewById<EditText>(R.id.tf_amount)
        _tf_amount_watcher = UtilsDigitTextWatcher().set_tf(tf).set_precision(_curr_asset.getInt("precision"))
        tf.addTextChangedListener(_tf_amount_watcher)
        _tf_amount_watcher.on_value_changed(::onAmountChanged)
    }

    /**
     *  (private) 生成当前余额 以及 余额对应的资产。
     */
    private fun _auxGenCurrBalanceAndBalanceAsset() {
        //  其他操作，从账号获取余额。
        assert(_full_account_data != null)
        _nCurrBalance = ModelUtils.findAssetBalance(_full_account_data!!, _curr_asset)
    }


    private fun drawUI_title() {
        findViewById<TextView>(R.id.title).text = resources.getString(R.string.kVcTitleAssetOpStakeVoteCreateTicket)
    }

    private fun drawUI_once() {
        binding.tfAmount.hint = resources.getString(R.string.kVcAssetOpStakeVoteCellPlaceholderAmount)
        binding.btnOpSubmit.text = resources.getString(R.string.kVcAssetOpStakeVoteBtnName)
        binding.tvUiMsg.text = resources.getString(R.string.kVcAssetOpStakeVoteUiTips)
    }

    /**
     *  (private) 是否允许切换资产
     */
    private fun isEnableSwitchAsset(): Boolean {
        return false
    }

    private fun drawUI_currAsset() {
        //  REMARK：这里显示选中资产名称，而不是余额资产名称。
        binding.tvAssetSymbol.text = _curr_asset.getString("symbol")

        if (isEnableSwitchAsset()) {
            binding.tvAssetSymbol.setTextColor(resources.getColor(R.color.theme01_textColorMain))
            binding.ivSelectAssetRightArrow.visibility = View.VISIBLE

            //  事件 - 选择资产
            binding.ivSelectAssetRightArrow.setColorFilter(resources.getColor(R.color.theme01_textColorGray))
            binding.layoutSelectAssetFromAssetsOpCommon.setOnClickListener { onSelectAsset() }
        } else {
            binding.tvAssetSymbol.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            binding.ivSelectAssetRightArrow.visibility = View.INVISIBLE
            //  事件 - 无（不可选择）
            binding.layoutSelectAssetFromAssetsOpCommon.setOnClickListener(null)
        }

        //  输入框尾部资产名称：这是当前余额资产名
        binding.tvTfTailerAssetSymbol.text = _curr_asset.getString("symbol")
    }

    private fun drawUI_ticketType(binding_event: Boolean = false) {
        if (_ticket_type == liquid) {
            binding.tvTicketType.text = resources.getString(R.string.kVcAssetOpStakeVoteCellValueLiquid)
            binding.tvTicketType.setTextColor(resources.getColor(R.color.theme01_textColorGray))
        } else {
            binding.tvTicketType.text = ActivityAssetOpStakeVote.getTicketTypeDesc(_ticket_type, this)
            binding.tvTicketType.setTextColor(resources.getColor(R.color.theme01_textColorMain))
        }
        if (binding_event) {
            binding.layoutSelectTicketType.setOnClickListener { onSelectTicketType() }
        }
    }

    private fun drawUI_balance(not_enough: Boolean) {
        val symbol = _curr_asset.getString("symbol")
        if (not_enough) {
            binding.tvCurrBalance.text = "${resources.getString(R.string.kOtcMcAssetCellAvailable)} ${_nCurrBalance.toPlainString()} $symbol(${resources.getString(R.string.kOtcMcAssetTransferBalanceNotEnough)})"
            binding.tvCurrBalance.setTextColor(resources.getColor(R.color.theme01_tintColor))
        } else {
            binding.tvCurrBalance.text = "${resources.getString(R.string.kOtcMcAssetCellAvailable)} ${_nCurrBalance.toPlainString()} $symbol"
            binding.tvCurrBalance.setTextColor(resources.getColor(R.color.theme01_textColorMain))
        }
    }

    /**
     *  (private) 转账数量发生变化。
     */
    private fun onAmountChanged(str_amount: String) {
        drawUI_balance(_nCurrBalance < Utils.auxGetStringDecimalNumberValue(str_amount))
    }

    /**
     *  (private) 选择全部数量
     */
    private fun onSelectAllClicked() {
        val tf = findViewById<EditText>(R.id.tf_amount)
        tf.setText(_nCurrBalance.toPlainString())
        tf.setSelection(tf.text.toString().length)
        //  onAmountChanged 会自动触发
    }

    /**
     *  事件 - 点击选择资产
     */
    private fun onSelectAsset() {
        //  REMARK:6.2 只支持锁BTS，不用切换。
    }

    /**
     *  事件 - 点击选择锁仓类型
     */
    private fun onSelectTicketType() {
        val self = this
        val items = JSONArray().apply {
            put(JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcAssetOpStakeVoteTicketTypeList180))
                put("type", lock_180_days)
            })
            put(JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcAssetOpStakeVoteTicketTypeList360))
                put("type", lock_360_days)
            })
            put(JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcAssetOpStakeVoteTicketTypeList720))
                put("type", lock_720_days)
            })
            put(JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcAssetOpStakeVoteTicketTypeListForever))
                put("type", lock_forever)
            })
        }
        var defaultIndex = 0
        var index = 0
        for (item in items.forin<JSONObject>()) {
            if (item!!.getInt("type") == _ticket_type) {
                defaultIndex = index
                break
            }
            ++index
        }

        //  显示列表
        ViewDialogNumberPicker(this, "", items, "title", defaultIndex) { _index: Int, text: String ->
            val result = items.getJSONObject(_index)
            val type = result.getInt("type")
            //  刷新UI
            if (type != _ticket_type) {
                _ticket_type = type
                drawUI_ticketType(binding_event = false)
            }
        }.show()
    }

    /**
     *  事件 - 点击提交操作
     */
    private fun onSubmitClicked() {
        if (_ticket_type == liquid) {
            showToast(resources.getString(R.string.kVcAssetOpStakeVoteSubmitTipsPleaseSelectTicketType))
            return
        }

        val n_amount = Utils.auxGetStringDecimalNumberValue(_tf_amount_watcher.get_tf_string())

        if (n_amount <= BigDecimal.ZERO) {
            showToast(resources.getString(R.string.kVcAssetOpStakeVoteSubmitTipsPleaseInputAmount))
            return
        }

        if (_nCurrBalance < n_amount) {
            showToast(resources.getString(R.string.kOtcMcAssetSubmitTipBalanceNotEnough))
            return
        }

        //  二次确认
        val value = String.format(resources.getString(R.string.kVcAssetOpStakeVoteSubmitAskForCreateTicket),
                n_amount.toPlainString(), _curr_asset.getString("symbol"), ActivityAssetOpStakeVote.getTicketTypeDesc(_ticket_type, this))
        UtilsAlert.showMessageConfirm(this, resources.getString(R.string.kVcHtlcMessageTipsTitle), value).then {
            if (it != null && it as Boolean) {
                guardWalletUnlocked(false) { unlocked ->
                    if (unlocked) {
                        _execAssetStakeVoteCore(n_amount)
                    }
                }
            }
        }
    }

    /**
     *  (private) 执行锁仓投票
     */
    private fun _execAssetStakeVoteCore(n_amount: BigDecimal) {
        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val op_account = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account")

        val n_amount_pow = n_amount.multiplyByPowerOf10(_curr_asset.getInt("precision"))
        val op = JSONObject().apply {
            put("fee", JSONObject().apply {
                put("amount", 0)
                put("asset_id", chainMgr.grapheneCoreAssetID)
            })
            put("account", op_account.getString("id"))
            put("target_type", _ticket_type)
            put("amount", JSONObject().apply {
                put("amount", n_amount_pow.toPlainString())
                put("asset_id", _curr_asset.getString("id"))
            })
        }

        //  确保有权限发起普通交易，否则作为提案交易处理。
        GuardProposalOrNormalTransaction(EBitsharesOperations.ebo_ticket_create, false, false,
                op, op_account) { isProposal, _ ->
            assert(!isProposal)
            //  请求网络广播
            val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
            BitsharesClientManager.sharedBitsharesClientManager().ticketCreate(op).then {
                mask.dismiss()
                showToast(resources.getString(R.string.kVcAssetOpStakeVoteSubmitTipOK))
                //  [统计]
                btsppLogCustom("txAssetStakeVoteFullOK", jsonObjectfromKVS("account", op_account.getString("id")))
                //  返回上一个界面并刷新
                _result_promise?.resolve(true)
                _result_promise = null
                finish()
                return@then null
            }.catch { err ->
                mask.dismiss()
                showGrapheneError(err)
                //  [统计]
                btsppLogCustom("txAssetStakeVoteFailed", jsonObjectfromKVS("account", op_account.getString("id")))
            }
        }
    }

}
