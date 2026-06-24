package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityAssetCreateOrEditBinding
import com.btsplusplus.fowallet.utils.ModelUtils
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.BitsharesClientManager
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale.getDefault

const val kPermissionActionDisablePermanently = 0   //  永久禁用（不可再开启）
const val kPermissionActionActivateLater = 1        //  暂不激活（后续可开启）
const val kPermissionActionActivateNow = 2          //  立即激活（创建后开启）

class ActivityAssetCreateOrEdit : BtsppActivity() {

    //  外部参数
    private var _result_promise: Promise? = null
    private var _edit_asset: JSONObject? = null
    private var _edit_bitasset: JSONObject? = null

    //  实例变量
    private var _enable_more_args = false                   //  是否启用高级设置
    private var _symbol = ""                                //  资产符号
    private var _max_supply: BigDecimal? = null             //  最大供应量
    private var _market_fee_percent = 0                     //  交易手续费百分比
    private var _reward_percent = 0                         //  手续费引荐人分成比例
    private var _max_market_fee: BigDecimal? = null         //  单笔手续费最大值
    private var _description = ""                           //  描述信息
    private var _precision = 0                              //  资产精度
    private var _max_supply_editable = BigDecimal.ZERO      //  可编辑的最大供应量

    private var _bitasset_options_args: JSONObject? = null  //  智能币相关参数（默认为空）
    private var _issuer_permissions = 0                     //  权限
    private var _old_issuer_permissions = 0                 //  编辑之前的权限。（仅编辑资产才存在）
    private var _flags = 0                                  //  激活标记

    private lateinit var binding: ActivityAssetCreateOrEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  设置自动布局
        setAutoLayoutContentView(R.layout.activity_asset_create_or_edit)
        //  设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        binding = ActivityAssetCreateOrEditBinding.bind(findViewById<View>(android.R.id.content).rootView)

        //  获取参数
        val args = btspp_args_as_JSONObject()
        _edit_asset = args.optJSONObject("kEditAsset")
        _edit_bitasset = args.optJSONObject("kEditBitAsset")
        _result_promise = args.opt("result_promise") as? Promise
        if (_edit_bitasset != null) {
            _bitasset_options_args = _edit_bitasset!!.getJSONObject("options").shadowClone()
        }
        genDefaultAssetArgs()

        //  初始化UI
        findViewById<TextView>(R.id.title).text = args.getString("kTitle")
        _drawUI_fixInfo()
        _drawUI_basicInfo()
        _drawUI_marketFeeInfo()
        _drawUI_permissionInfo()
        _drawUI_smartInfo()
        _drawUI_visible_onAdvSwitchChanged(false)
        _drawUI_button()
        _drawUI_tips()

        //  绑定事件 - 基本信息
        binding.layoutBasicAssetSymbol.setOnClickListener { onAssetSymbolClicked() }
        binding.layoutBasicMaxSupply.setOnClickListener { onAssetMaxSupplyClicked() }
        binding.layoutBasicDesc.setOnClickListener { onAssetDescClicked() }
        binding.layoutBasicAssetPrecision.setOnClickListener { onAssetPrecisionClicked() }

        //  绑定事件 - 手续费信息
        binding.layoutMarketFeePercent.setOnClickListener {
            onInputDecimalClicked(resources.getString(R.string.kVcAssetMgrCellTitleFeeMarketFeeRatio),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderFeeMarketFeeRatio), 2,
                    BigDecimal.valueOf(100),
                    Utils.auxGetStringDecimalNumberValue(GRAPHENE_1_PERCENT.toString())) { n_value ->
                _market_fee_percent = n_value.toInt()
                _drawValue_percentValue(binding.tvMarketFeePercent, _market_fee_percent)
            }
        }
        binding.layoutFeeMaxValue.setOnClickListener {
            onInputDecimalClicked(resources.getString(R.string.kVcAssetMgrCellTitleFeeMaxFeeValue),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderFeeMaxFeeValue), _precision,
                    _max_supply_editable,
                    null) { n_value ->
                _max_market_fee = n_value
                _drawValue_maxMarketFee()
            }
        }
        binding.layoutFeeRefPercent.setOnClickListener {
            //  REMARK：这个最大值小于 100
            onInputDecimalClicked(resources.getString(R.string.kVcAssetMgrCellTitleFeeRefPercent),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderFeeRefPercent), 2,
                    BigDecimal("99.99"),
                    Utils.auxGetStringDecimalNumberValue(GRAPHENE_1_PERCENT.toString())) { n_value ->
                _reward_percent = n_value.toInt()
                _drawValue_percentValue(binding.tvFeeRefPercent, _reward_percent)
            }
        }

        //  绑定事件 - 智能币
        if (isCreateAsset()) {
            binding.layoutSmartBackingAsset.setOnClickListener { onSmartBackingAssetClicked() }
        }
        binding.layoutSmartFeedLifetime.setOnClickListener {
            onSmartArgsClicked(resources.getString(R.string.kVcAssetMgrInputTitleSmartFeedLifeTime),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderSmartFeedLifeTime), "feed_lifetime_sec", null,
                    BigDecimal.valueOf(60), true, 0)
        }
        binding.layoutSmartMinFeedNum.setOnClickListener {
            onSmartArgsClicked(resources.getString(R.string.kVcAssetMgrCellTitleSmartMinFeedNum),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderSmartMinFeedNum), "minimum_feeds", null,
                    null, true, 0)
        }
        binding.layoutSmartDelayForSettle.setOnClickListener {
            onSmartArgsClicked(resources.getString(R.string.kVcAssetMgrInputTitleSmartDelayForSettle),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderSmartDelayForSettle), "force_settlement_delay_sec", null,
                    BigDecimal.valueOf(60), false, 0)
        }
        binding.layoutSmartOffsetSettle.setOnClickListener {
            onSmartArgsClicked(resources.getString(R.string.kVcAssetMgrCellTitleSmartOffsetSettle),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderSmartOffsetSettle), "force_settlement_offset_percent", BigDecimal.valueOf(100),
                    Utils.auxGetStringDecimalNumberValue(GRAPHENE_1_PERCENT.toString()), false, 2)
        }
        binding.layoutSmartMaxSettleVolume.setOnClickListener {
            onSmartArgsClicked(resources.getString(R.string.kVcAssetMgrCellTitleSmartMaxSettleValuePerHour),
                    resources.getString(R.string.kVcAssetMgrInputPlaceholderSmartMaxSettleValuePerHour), "maximum_force_settlement_volume", BigDecimal.valueOf(100),
                    Utils.auxGetStringDecimalNumberValue(GRAPHENE_1_PERCENT.toString()), true, 2)
        }

        //  绑定事件 - 权限信息
        binding.layoutPermissionMarketFee.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_charge_market_fee) }
        binding.layoutPermissionWhitelisted.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_white_list) }
        binding.layoutPermissionOverrideTransfer.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_override_authority) }
        binding.layoutPermissionNeedIssuerApproved.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_transfer_restricted) }
        binding.layoutPermissionDisabledCondTransfer.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_disable_confidential) }
        binding.layoutPermissionDisabledForceSettlements.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_disable_force_settle) }
        binding.layoutPermissionAllowGlobalSettle.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_global_settle) }
        binding.layoutPermissionAllowWitnessFeed.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_witness_fed_asset) }
        binding.layoutPermissionAllowCommitteeFeed.setOnClickListener { onSmartPermissionClicked(EBitsharesAssetFlags.ebat_committee_fed_asset) }

        //  事件 - 提交按钮
        binding.btnSubmit.setOnClickListener { onSubmitClicked() }


        //  事件 - 返回
        binding.layoutBackFromAssetsCreateOrEdit.setOnClickListener { finish() }
    }

    /**
     *  初始化 智能币默认参数
     */
    private fun genDefaultSmartCoinArgs() {
        if (_bitasset_options_args == null) {
            _bitasset_options_args = JSONObject().apply {
                put("feed_lifetime_sec", 1440 * 60)
                put("minimum_feeds", 1)
                put("force_settlement_delay_sec", 1440 * 60)
                put("force_settlement_offset_percent", 5 * GRAPHENE_1_PERCENT)
                put("maximum_force_settlement_volume", 5 * GRAPHENE_1_PERCENT)
            }
        }
    }

    /**
     *  初始化 资产默认参数
     */
    private fun genDefaultAssetArgs() {
        if (isCreateAsset()) {
            //  创建
            updatePrecision(5)
            _symbol = ""
            _max_supply = null
            _description = ""

            _issuer_permissions = EBitsharesAssetFlags.ebat_issuer_permission_mask_uia.value
            _flags = 0

            _market_fee_percent = 0
            _max_market_fee = null
            _reward_percent = 0
        } else {
            //  编辑
            updatePrecision(_edit_asset!!.getInt("precision"))
            _symbol = _edit_asset!!.getString("symbol")
            val asset_options = _edit_asset!!.getJSONObject("options")
            _max_supply = bigDecimalfromAmount(asset_options.getString("max_supply"), _precision)
            _description = asset_options.optString("description")

            _issuer_permissions = asset_options.getInt("issuer_permissions")
            _flags = asset_options.getInt("flags")
            //  记录编辑之前的权限
            _old_issuer_permissions = _issuer_permissions

            _market_fee_percent = asset_options.getInt("market_fee_percent")
            _max_market_fee = bigDecimalfromAmount(asset_options.getString("max_market_fee"), _precision)
            _reward_percent = asset_options.getJSONObject("extensions").optInt("reward_percent")
        }
    }

    private fun updatePrecision(precision: Int) {
        assert(precision >= 0 && precision <= 12)
        _precision = precision
        _max_supply_editable = bigDecimalfromAmount(GRAPHENE_MAX_SHARE_SUPPLY.toString(), _precision)
    }

    /**
     *  （private) 辅助 - 判断UI操作行为
     */
    private fun isEditSmartInfo(): Boolean {
        return _edit_asset != null && _edit_bitasset != null
    }

    private fun isEditBasicInfo(): Boolean {
        return _edit_asset != null && _edit_bitasset == null
    }

    private fun isCreateAsset(): Boolean {
        return _edit_asset == null && _edit_bitasset == null
    }

    private fun isEditAsset(): Boolean {
        return !isCreateAsset()
    }

    /**
     *  描绘 - 固定信息（编辑资产时存在）
     */
    private fun _drawUI_fixInfo() {
        if (isEditAsset()) {
            binding.layoutSegmentFixinfos.visibility = View.VISIBLE

            binding.tvFixedAssetSymbol.text = _edit_asset!!.getString("symbol")
            binding.tvFixedAssetPrecision.text = String.format(resources.getString(R.string.kVcAssetMgrCellValueAssetPrecision), _edit_asset!!.getString("precision"))

            binding. tvFixedAssetSymbol.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
            binding.tvFixedAssetPrecision.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
        } else {
            binding.layoutSegmentFixinfos.visibility = View.GONE
        }
    }

    /**
     *  (private) 设置控件可见性 - 高级设置开关变更时
     */
    private fun _drawUI_visible_onAdvSwitchChanged(on: Boolean) {
        if (isCreateAsset()) {
            _enable_more_args = on

            if (_enable_more_args) {
                binding.layoutBasicAssetPrecision.visibility = View.VISIBLE
                binding.layoutBasicAssetPrecisionLine.visibility = View.VISIBLE

                _drawUI_smartInfo()
            } else {
                binding.layoutBasicAssetPrecision.visibility = View.GONE
                binding.layoutBasicAssetPrecisionLine.visibility = View.GONE

                binding.layoutSegmentSmartinfos.visibility = View.GONE
            }
        }
    }

    /**
     *  描绘 - 基本信息（创建和更新资产都存在，但不同）
     */
    private fun _drawUI_basicInfo() {
        if (isCreateAsset()) {
            binding.layoutSegmentBasicinfos.visibility = View.VISIBLE

            binding.layoutBasicAssetSymbol.visibility = View.VISIBLE
            binding.layoutBtnSwitchAdv.visibility = View.VISIBLE
            binding.layoutBasicAssetPrecision.visibility = View.VISIBLE
            binding.layoutBasicAssetSymbolLine.visibility = View.VISIBLE
            binding.layoutBtnSwitchAdvLine.visibility = View.VISIBLE
            binding.layoutBasicAssetPrecisionLine.visibility = View.VISIBLE

            //  描绘
            _drawValue_assetSymbol()
            _drawValue_assetPrecision()
            _drawValue_maxSupply()
            _drawValue_assetDesc()

            //  事件 - 高级设置
            binding.btnSwitchAdv.setOnCheckedChangeListener { _, isChecked: Boolean -> _drawUI_visible_onAdvSwitchChanged(isChecked) }
        } else if (isEditBasicInfo()) {
            binding.layoutSegmentBasicinfos.visibility = View.VISIBLE

            binding.layoutBasicAssetSymbol.visibility = View.GONE
            binding.layoutBtnSwitchAdv.visibility = View.GONE
            binding.layoutBasicAssetPrecision.visibility = View.GONE
            binding.layoutBasicAssetSymbolLine.visibility = View.GONE
            binding.layoutBtnSwitchAdvLine.visibility = View.GONE
            binding.layoutBasicAssetPrecisionLine.visibility = View.GONE

            //  描绘
            _drawValue_maxSupply()
            _drawValue_assetDesc()
        } else {
            binding.layoutSegmentBasicinfos.visibility = View.GONE
        }
    }

    /**
     *  描绘值 - 资产名称
     */
    private fun _drawValue_assetSymbol() {
        if (!isCreateAsset()) {
            return
        }
        binding.tvBasicAssetSymbol.let { label ->
            if (_symbol.isNotEmpty()) {
                label.text = _symbol
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellPlaceholderAssetName)
                label.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  描绘值 - 最大供应量
     */
    private fun _drawValue_maxSupply() {
        binding.tvBasicMaxSupply.let { label ->
            if (_max_supply != null && _max_supply!! > BigDecimal.ZERO) {
                label.text = OrgUtils.formatFloatValue(_max_supply!!.toDouble(), _precision, has_comma = true)
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellPlaceholderMaxSupply)
                label.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  描绘值 - 资产描述
     */
    private fun _drawValue_assetDesc() {
        binding.tvBasicDesc.let { label ->
            if (_description.isNotEmpty()) {
                label.text = _description
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellPlaceholderAssetDesc)
                label.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  描绘值 - 资产精度
     */
    private fun _drawValue_assetPrecision() {
        if (!isCreateAsset()) {
            return
        }
        binding.tvBasicAssetPrecision.let { label ->
            label.text = String.format(resources.getString(R.string.kVcAssetMgrCellValueAssetPrecision), _precision.toString())
            label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
        }
    }

    /**
     *  描绘值 - 单笔最大手续费
     */
    private fun _drawValue_maxMarketFee() {
        binding.tvFeeMaxValue.let { label ->
            if (_max_market_fee != null) {
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                label.text = OrgUtils.formatFloatValue(_max_market_fee!!.toDouble(), _precision, has_comma = true)
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellValueNotSet)
                label.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  描绘 - 手续费信息
     */
    private fun _drawUI_marketFeeInfo() {
        if (isEditBasicInfo() && _flags.and(EBitsharesAssetFlags.ebat_charge_market_fee.value) != 0) {
            binding.layoutSegmentMarketfeeinfos.visibility = View.VISIBLE

            //  描绘
            _drawValue_percentValue(binding.tvMarketFeePercent, _market_fee_percent)
            _drawValue_maxMarketFee()
            _drawValue_percentValue(binding.tvFeeRefPercent, _reward_percent)
        } else {
            binding.layoutSegmentMarketfeeinfos.visibility = View.GONE
        }
    }

    /**
     *  (private)  设置控件可见性 - 智能币相关的权限信息字段
     */
    private fun _drawUI_visible_smartPermissionRows(isSmartCorin: Boolean) {
        if (isSmartCorin) {
            binding.layoutPermissionDisabledForceSettlements.visibility = View.VISIBLE
            binding.layoutPermissionAllowGlobalSettle.visibility = View.VISIBLE
            binding.layoutPermissionAllowWitnessFeed.visibility = View.VISIBLE
            binding.layoutPermissionAllowCommitteeFeed.visibility = View.VISIBLE

            binding.layoutPermissionDisabledForceSettlementsLine.visibility = View.VISIBLE
            binding.layoutPermissionAllowGlobalSettleLine.visibility = View.VISIBLE
            binding.layoutPermissionAllowWitnessFeedLine.visibility = View.VISIBLE
            binding.layoutPermissionAllowCommitteeFeedLine.visibility = View.VISIBLE
        } else {
            binding.layoutPermissionDisabledForceSettlements.visibility = View.GONE
            binding.layoutPermissionAllowGlobalSettle.visibility = View.GONE
            binding.layoutPermissionAllowWitnessFeed.visibility = View.GONE
            binding.layoutPermissionAllowCommitteeFeed.visibility = View.GONE

            binding.layoutPermissionDisabledForceSettlementsLine.visibility = View.GONE
            binding.layoutPermissionAllowGlobalSettleLine.visibility = View.GONE
            binding.layoutPermissionAllowWitnessFeedLine.visibility = View.GONE
            binding.layoutPermissionAllowCommitteeFeedLine.visibility = View.GONE
        }
    }

    /**
     *  描绘 - 权限信息（编辑资产存在）
     */
    private fun _drawUI_permissionInfo() {
        if (isEditBasicInfo()) {
            binding.layoutSegmentPermissioninfos.visibility = View.VISIBLE

            //  描绘
            _drawUI_onePermission(binding.tvTitlePermissionMarketFee, binding.tvPermissionMarketFee, binding.imgArrowPermissionMarketFee, EBitsharesAssetFlags.ebat_charge_market_fee)
            _drawUI_onePermission(binding.tvTitlePermissionWhitelisted, binding.tvPermissionWhitelisted, binding.imgArrowPermissionWhitelisted, EBitsharesAssetFlags.ebat_white_list)
            _drawUI_onePermission(binding.tvTitlePermissionOverrideTransfer, binding.tvPermissionOverrideTransfer, binding.imgArrowPermissionOverrideTransfer, EBitsharesAssetFlags.ebat_override_authority)
            _drawUI_onePermission(binding.tvTitlePermissionNeedIssuerApproved, binding.tvPermissionNeedIssuerApproved, binding.imgArrowPermissionNeedIssuerApproved, EBitsharesAssetFlags.ebat_transfer_restricted)
            _drawUI_onePermission(binding.tvTitlePermissionDisabledCondTransfer, binding.tvPermissionDisabledCondTransfer, binding.imgArrowPermissionDisabledCondTransfer, EBitsharesAssetFlags.ebat_disable_confidential)

            _drawUI_onePermission(binding.tvTitlePermissionDisabledForceSettlements, binding.tvPermissionDisabledForceSettlements, binding.imgArrowPermissionDisabledForceSettlements, EBitsharesAssetFlags.ebat_disable_force_settle)
            _drawUI_onePermission(binding.tvTitlePermissionAllowGlobalSettle, binding.tvPermissionAllowGlobalSettle, binding.imgArrowPermissionAllowGlobalSettle, EBitsharesAssetFlags.ebat_global_settle)
            _drawUI_onePermission(binding.tvTitlePermissionAllowWitnessFeed, binding.tvPermissionAllowWitnessFeed, binding.imgArrowPermissionAllowWitnessFeed, EBitsharesAssetFlags.ebat_witness_fed_asset)
            _drawUI_onePermission(binding.tvTitlePermissionAllowCommitteeFeed, binding.tvPermissionAllowCommitteeFeed, binding.imgArrowPermissionAllowCommitteeFeed, EBitsharesAssetFlags.ebat_committee_fed_asset)

            _drawUI_visible_smartPermissionRows(ModelUtils.assetIsSmart(_edit_asset!!))
        } else {
            binding.layoutSegmentPermissioninfos.visibility = View.GONE
        }
    }

    /**
     *  (private)  设置控件可见性 - 智能币相关字段
     */
    private fun _drawUI_visible_smartRows(isSmartCorin: Boolean) {
        if (isSmartCorin) {
            binding.layoutSmartFeedLifetime.visibility = View.VISIBLE
            binding.layoutSmartMinFeedNum.visibility = View.VISIBLE
            binding.layoutSmartDelayForSettle.visibility = View.VISIBLE
            binding.layoutSmartOffsetSettle.visibility = View.VISIBLE
            binding.layoutSmartMaxSettleVolume.visibility = View.VISIBLE

            binding.layoutSmartFeedLifetimeLine.visibility = View.VISIBLE
            binding.layoutSmartMinFeedNumLine.visibility = View.VISIBLE
            binding.layoutSmartDelayForSettleLine.visibility = View.VISIBLE
            binding.layoutSmartOffsetSettleLine.visibility = View.VISIBLE
            binding.layoutSmartMaxSettleVolumeLine.visibility = View.VISIBLE
        } else {
            binding.layoutSmartFeedLifetime.visibility = View.GONE
            binding.layoutSmartMinFeedNum.visibility = View.GONE
            binding.layoutSmartDelayForSettle.visibility = View.GONE
            binding.layoutSmartOffsetSettle.visibility = View.GONE
            binding.layoutSmartMaxSettleVolume.visibility = View.GONE

            binding.layoutSmartFeedLifetimeLine.visibility = View.GONE
            binding.layoutSmartMinFeedNumLine.visibility = View.GONE
            binding.layoutSmartDelayForSettleLine.visibility = View.GONE
            binding.layoutSmartOffsetSettleLine.visibility = View.GONE
            binding.layoutSmartMaxSettleVolumeLine.visibility = View.GONE
        }
    }

    /**
     *  描绘值 - 智能币背书资产
     */
    private fun _drawValue_smartBackingAsset() {
        binding.imgArrowSmartBackingAsset.visibility = View.VISIBLE
        binding.tvSmartBackingAsset.let { label ->
            if (_bitasset_options_args != null) {
                if (isEditSmartInfo()) {
                    binding.imgArrowSmartBackingAsset.visibility = View.GONE
                    label.text = ChainObjectManager.sharedChainObjectManager().getChainObjectByID(_bitasset_options_args!!.getString("short_backing_asset")).getString("symbol")
                    label.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
                } else {
                    label.text = _bitasset_options_args!!.getJSONObject("short_backing_asset").getString("symbol")
                    label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                }
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellValueSmartBackingAssetNone)
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
            }
        }
    }

    /**
     *  描绘值 - 智能币相关参数
     */
    private fun _drawValue_smartValues(label: TextView, key: String, have_value_callback: (lb: TextView, value: Any) -> Unit) {
        if (_bitasset_options_args != null) {
            if (_bitasset_options_args!!.has(key)) {
                label.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                have_value_callback(label, _bitasset_options_args!!.get(key))
            } else {
                label.text = resources.getString(R.string.kVcAssetMgrCellValueNotSet)
                label.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  描绘值 - 百分比格式
     */
    private fun _drawValue_percentValue(label: TextView, value: Any) {
        val n_100 = BigDecimal(GRAPHENE_1_PERCENT.toString())
        val n_percent = Utils.auxGetStringDecimalNumberValue(value.toString()).divide(n_100, 2, BigDecimal.ROUND_UP)
        label.text = "${n_percent.toPlainString()}%"
    }

    /**
     *  描绘值 - 所有智能币参数
     */
    private fun _drawValue_allSmartArgs() {
        _drawValue_smartBackingAsset()
        _drawValue_smartValues(binding.tvSmartFeedLifetime, "feed_lifetime_sec") { lb, value ->
            lb.text = String.format(resources.getString(R.string.kVcAssetMgrCellValueSmartMinN), value.toString().toInt() / 60)
        }
        _drawValue_smartValues(binding.tvSmartMinFeedNum, "minimum_feeds") { lb, value ->
            lb.text = value.toString()
        }
        _drawValue_smartValues(binding.tvSmartDelayForSettle, "force_settlement_delay_sec") { lb, value ->
            lb.text = String.format(resources.getString(R.string.kVcAssetMgrCellValueSmartMinN), value.toString().toInt() / 60)
        }
        _drawValue_smartValues(binding.tvSmartOffsetSettle, "force_settlement_offset_percent") { lb, value -> _drawValue_percentValue(lb, value) }
        _drawValue_smartValues(binding.tvSmartMaxSettleVolume, "maximum_force_settlement_volume") { lb, value -> _drawValue_percentValue(lb, value) }
    }

    /**
     *  描绘 - 智能币信息（更新智能币和创建资产高级设置存在）
     */
    private fun _drawUI_smartInfo() {
        if ((isCreateAsset() && _enable_more_args) || isEditSmartInfo()) {
            binding.layoutSegmentSmartinfos.visibility = View.VISIBLE

            //  描绘
            _drawValue_allSmartArgs()

            if (isEditSmartInfo()) {
                _drawUI_visible_smartRows(true)
            } else {
                _drawUI_visible_smartRows(_bitasset_options_args != null)
            }
        } else {
            binding.layoutSegmentSmartinfos.visibility = View.GONE
        }
    }

    /**
     *  描绘 - 提交按钮名字
     */
    private fun _drawUI_button() {
        if (isCreateAsset()) {
            binding.btnSubmit.text = resources.getString(R.string.kVcAssetMgrAssetCreateButton)
        } else if (isEditBasicInfo()) {
            binding.btnSubmit.text = resources.getString(R.string.kVcAssetMgrAssetUpdateAssetButton)
        } else {
            binding.btnSubmit.text = resources.getString(R.string.kVcAssetMgrAssetUpdateBitassetButton)
        }
    }

    /**
     *  描绘 - 底部提示信息
     */
    private fun _drawUI_tips() {
        if (isCreateAsset()) {
            binding.layoutTipsInfoSegment.visibility = View.VISIBLE
            binding.layoutTipsInfoSegment.text = resources.getString(R.string.kVcAssetMgrCreateUiTipsCreate)
        } else if (isEditBasicInfo()) {
            binding.layoutTipsInfoSegment.visibility = View.VISIBLE
            binding.layoutTipsInfoSegment.text = resources.getString(R.string.kVcAssetMgrCreateUiTipsUpdateAsset)
        } else {
            binding.layoutTipsInfoSegment.visibility = View.GONE
        }
    }

    /**
     *  (private) 描绘 - 单行权限信息的值
     */
    private fun _drawUI_onePermission(titleLabel: TextView, valueLabel: TextView, arrow: ImageView, checkFeature: EBitsharesAssetFlags) {
        arrow.visibility = View.VISIBLE
        titleLabel.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
        if (checkFeature == EBitsharesAssetFlags.ebat_global_settle) {
            if (_issuer_permissions.and(checkFeature.value) != 0) {
                valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusActivateNow)
                valueLabel.setTextColor(resources.getColor(R.color.theme01_buyColor))
            } else {
                //  编辑之前就已经永久禁用的属性，去掉末尾箭头。
                if (isEditAsset() && _old_issuer_permissions.and(checkFeature.value) == 0) {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusAlreadyDisablePermanently)
                    arrow.visibility = View.GONE
                    titleLabel.setTextColor(resources.getColor(R.color.theme01_textColorGray))
                } else {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusDisablePermanently)
                }
                valueLabel.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        } else {
            if (_issuer_permissions.and(checkFeature.value) != 0) {
                if (_flags.and(checkFeature.value) != 0) {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusActivateNow)
                    valueLabel.setTextColor(resources.getColor(R.color.theme01_buyColor))
                } else {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusActivateLater)
                    valueLabel.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                }
            } else {
                //  编辑之前就已经永久禁用的属性，去掉末尾箭头。
                if (isEditAsset() && _old_issuer_permissions.and(checkFeature.value) == 0) {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusAlreadyDisablePermanently)
                    arrow.visibility = View.GONE
                    titleLabel.setTextColor(resources.getColor(R.color.theme01_textColorGray))
                } else {
                    valueLabel.text = resources.getString(R.string.kVcAssetMgrPermissionStatusDisablePermanently)
                }
                valueLabel.setTextColor(resources.getColor(R.color.theme01_textColorGray))
            }
        }
    }

    /**
     *  事件 - 资产名称点击
     */
    private fun onAssetSymbolClicked() {
        if (!isCreateAsset()) {
            return
        }
        UtilsAlert.showInputBox(this, title = resources.getString(R.string.kVcAssetMgrCellTitleAssetName),
                placeholder = resources.getString(R.string.kVcAssetMgrCellPlaceholderAssetName),
                is_password = false).then {
            val value = it as? String
            if (value != null) {
                //  资产名称有效性再提交的时候检测
                _symbol = value.uppercase(getDefault())
                _drawValue_assetSymbol()
            }
            return@then null
        }
    }

    /**
     *  事件 - 最大供应量点击
     */
    private fun onAssetMaxSupplyClicked() {
        onInputDecimalClicked(resources.getString(R.string.kVcAssetMgrCellTitleMaxSupply),
                resources.getString(R.string.kVcAssetMgrCellPlaceholderMaxSupply), _precision, _max_supply_editable, null) { n_value ->
            if (n_value.compareTo(BigDecimal.ZERO) == 0) {
                _max_supply = null
            } else {
                _max_supply = n_value
            }
            _drawValue_maxSupply()
        }
    }

    /**
     *  事件 - 资产描述点击
     */
    private fun onAssetDescClicked() {
        UtilsAlert.showInputBox(this, title = resources.getString(R.string.kVcAssetMgrCellTitleAssetDesc),
                placeholder = resources.getString(R.string.kVcAssetMgrCellPlaceholderAssetDesc),
                is_password = false).then {
            val value = it as? String
            if (value != null) {
                _description = value
                _drawValue_assetDesc()
            }
            return@then null
        }
    }

    /**
     *  事件 - 资产精度点击
     */
    private fun onAssetPrecisionClicked() {
        if (!isCreateAsset()) {
            return
        }

        val data_list = JSONArray()
        var default_select = -1
        for (i in 0..12) {
            val name = String.format(resources.getString(R.string.kVcAssetMgrCellValueAssetPrecision), i.toString())
            if (i == _precision) {
                default_select = data_list.length()
            }
            data_list.put(JSONObject().apply {
                put("name", name)
                put("value", i)
            })
        }

        ViewDialogNumberPicker(this, resources.getString(R.string.kVcAssetMgrCellTitleAssetPrecision), data_list, "name", default_select) { _index: Int, _: String ->
            val result = data_list.getJSONObject(_index)
            val new_precision = result.getInt("value")
            val old_precision = _precision
            updatePrecision(new_precision)
            //  REMARK：更改了资产精度，则清除用户之前设置的最大供应量。
            if (new_precision != old_precision) {
                _max_supply = null
            }
            //  刷新
            _drawValue_assetPrecision()
            _drawValue_maxSupply()
        }.show()
    }

    /**
     *  事件 - 背书资产点击
     */
    private fun onSmartBackingAssetClicked() {
        if (!isCreateAsset()) {
            return
        }

        val chainMgr = ChainObjectManager.sharedChainObjectManager()

        val list = arrayOf(
                resources.getString(R.string.kVcAssetMgrCellValueSmartBackingAssetNone),
                chainMgr.grapheneAssetSymbol,
                resources.getString(R.string.kVcAssetMgrCellValueSmartBackingAssetCustom))

        ViewSelector.show(this, resources.getString(R.string.kVcAssetMgrInputTitleSelectBackAsset), list) { index: Int, result: String ->
            when (index) {
                0 -> {  //  取消
                    _bitasset_options_args = null
                    //  取消智能币相关标记
                    _issuer_permissions = _issuer_permissions.and(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value.inv())
                    _flags = _flags.and(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value.inv())
                    //  刷新UI
                    _drawUI_smartInfo()
                }
                1 -> {  //  BTS
                    genDefaultSmartCoinArgs()
                    _bitasset_options_args!!.put("short_backing_asset", JSONObject().apply {
                        put("id", chainMgr.grapheneCoreAssetID)
                        put("symbol", chainMgr.grapheneAssetSymbol)
                    })
                    //  添加智能币相关标记（flags不变）
                    _issuer_permissions = _issuer_permissions.or(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value)
                    //  刷新UI
                    _drawUI_smartInfo()
                }
                2 -> {  //  自定义
                    TempManager.sharedTempManager().set_query_account_callback { last_activity, asset_info ->
                        last_activity.goTo(ActivityAssetCreateOrEdit::class.java, true, back = true)
                        //  选择完毕
                        genDefaultSmartCoinArgs()
                        _bitasset_options_args!!.put("short_backing_asset", asset_info)
                        //  添加智能币相关标记（flags不变）
                        _issuer_permissions = _issuer_permissions.or(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value)
                        //  刷新UI
                        _drawUI_smartInfo()
                    }
                    val title = resources.getString(R.string.kVcTitleSearchBackAsset)
                    goTo(ActivityAccountQueryBase::class.java, true, args = JSONObject().apply {
                        put("kSearchType", ENetworkSearchType.enstAssetAll)
                        put("kTitle", title)
                    })
                }
            }
        }
    }

    /**
     *  事件 - 背书资产相关参数点击
     */
    private fun onSmartArgsClicked(args_title: String, args_placeholder: String, args_key: String, n_max_value: BigDecimal?, n_scale: BigDecimal?, clear_when_zero: Boolean, precision: Int) {
        onInputDecimalClicked(args_title, args_placeholder, precision, n_max_value, n_scale) { n_value ->
            if (clear_when_zero && n_value.compareTo(BigDecimal.ZERO) == 0) {
                _bitasset_options_args!!.remove(args_key)
            } else {
                _bitasset_options_args!!.put(args_key, n_value.toInt())
            }
            //  刷新智能币参数显示
            _drawValue_allSmartArgs()
        }
    }

    /**
     *  事件 - 部分数字输入框点击
     */
    private fun onInputDecimalClicked(args_title: String, args_placeholder: String, precision: Int, n_max_value: BigDecimal?, n_scale: BigDecimal?, callback: (n_value: BigDecimal) -> Unit) {
        UtilsAlert.showInputBox(this, title = args_title, placeholder = args_placeholder, is_password = false, iDecimalPrecision = precision).then {
            val value = it as? String
            if (value != null) {
                var n_value = Utils.auxGetStringDecimalNumberValue(value)
                //  最大值
                if (n_max_value != null && n_value > n_max_value) {
                    n_value = n_max_value
                }
                //  缩放
                if (n_scale != null) {
                    n_value = n_value.multiply(n_scale)
                }
                callback(n_value)
            }
            return@then null
        }
    }

    /**
     *  事件 - 权限点击
     */
    private fun onSmartPermissionClicked(feature: EBitsharesAssetFlags) {
        //  编辑资产：已经永久禁用的权限，不可再编辑。
        if (isEditAsset() && _old_issuer_permissions.and(feature.value) == 0) {
            return
        }

        val self = this
        var defaultIndex = 0
        val items = JSONArray()
        items.put(JSONObject().apply {
            put("title", self.resources.getString(R.string.kVcAssetMgrPermissionActionDisablePermanently))
            put("type", kPermissionActionDisablePermanently)
        })
        if (feature != EBitsharesAssetFlags.ebat_global_settle) {
            //  特殊处理：非全局清算权限
            items.put(JSONObject().apply {
                put("title", self.resources.getString(R.string.kVcAssetMgrPermissionActionActivateLater))
                put("type", kPermissionActionActivateLater)
            })
            if (_issuer_permissions.and(feature.value) != 0) {
                if (_flags.and(feature.value) != 0) {
                    defaultIndex = 2
                } else {
                    defaultIndex = 1
                }
            } else {
                defaultIndex = 0
            }
        } else {
            //  特殊处理：全局清算权限
            if (_issuer_permissions.and(feature.value) != 0) {
                defaultIndex = 1
            } else {
                defaultIndex = 0
            }
        }
        items.put(JSONObject().apply {
            put("title", self.resources.getString(R.string.kVcAssetMgrPermissionActionActivateNow))
            put("type", kPermissionActionActivateNow)
        })

        //  显示列表
        ViewDialogNumberPicker(this, null, items, "title", defaultIndex) { _index: Int, _: String ->
            when (items.getJSONObject(_index).getInt("type")) {
                kPermissionActionDisablePermanently -> {
                    //  取消 permission 和 flags
                    _issuer_permissions = _issuer_permissions.and(feature.value.inv())
                    _flags = _flags.and(feature.value.inv())
                    //  刷新
                    _drawUI_permissionInfo()
                    if (feature == EBitsharesAssetFlags.ebat_charge_market_fee) {
                        _drawUI_marketFeeInfo()
                    }
                    //  TODO: 5.0 白名单等支持需要刷新界面
                }
                kPermissionActionActivateLater -> {
                    //  开启 permission，取消 flags。
                    _issuer_permissions = _issuer_permissions.or(feature.value)
                    _flags = _flags.and(feature.value.inv())
                    //  刷新
                    _drawUI_permissionInfo()
                    if (feature == EBitsharesAssetFlags.ebat_charge_market_fee) {
                        _drawUI_marketFeeInfo()
                    }
                    //  TODO: 5.0 白名单等支持需要刷新界面
                }
                kPermissionActionActivateNow -> {
                    //  同时开启 permission 和 flags。
                    _issuer_permissions = _issuer_permissions.or(feature.value)
                    _flags = _flags.or(feature.value)
                    //  REMARK：全局清算不可设置flag。
                    if (feature == EBitsharesAssetFlags.ebat_global_settle) {
                        _flags = _flags.and(feature.value.inv())
                    }
                    //  刷新
                    _drawUI_permissionInfo()
                    if (feature == EBitsharesAssetFlags.ebat_charge_market_fee) {
                        _drawUI_marketFeeInfo()
                    }
                    //  TODO: 5.0 白名单等支持需要刷新界面
                }
            }
        }.show()
    }

    /**
     *  事件 - 提交按钮点击
     */
    private fun onSubmitClicked() {
        if (isEditSmartInfo()) {
            onSubmitUpdateBitAssetsClicked()
            return
        }
        if (isEditBasicInfo()) {
            onSubmitEditClicked()
            return
        }

        //  各种条件校验
        val sym = _symbol.uppercase(getDefault()).trim()
        if (!_checkAssetSymbolName(sym)) {
            return
        }

        if (_max_supply == null || _max_supply!! <= BigDecimal.ZERO) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInputMaxSupply))
            return
        }

        var bitasset_opts: JSONObject? = null
        if (_bitasset_options_args != null) {
            //  智能币 - 附加校验
            if (!_validationBitAssetsArgs()) {
                return
            }
            //  本地参数转换为链上参数
            val temp = _bitasset_options_args!!.shadowClone()
            val short_backing_asset = temp.get("short_backing_asset")
            if (short_backing_asset is JSONObject) {
                temp.put("short_backing_asset", short_backing_asset.getString("id"))
            }
            bitasset_opts = temp.shadowClone()
        } else {
            //  非智能币 - 取消多余的标记
            _issuer_permissions = _issuer_permissions.and(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value.inv())
            _flags = _flags.and(EBitsharesAssetFlags.ebat_issuer_permission_mask_smart_only.value.inv())
        }

        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val account = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account")
        val uid = account.getString("id")

        val core_asset = chainMgr.getChainObjectByID(chainMgr.grapheneCoreAssetID)
        val core_precision = core_asset.getInt("precision")

        val n_max_supply_pow = _max_supply!!.multiplyByPowerOf10(_precision)
        val n_core_asset_one_pow = BigDecimal.ONE.multiplyByPowerOf10(core_precision)

        val asset_options = JSONObject().apply {
            put("max_supply", n_max_supply_pow.toPlainString())
            put("market_fee_percent", 0)
            put("max_market_fee", 0)
            put("issuer_permissions", _issuer_permissions)
            put("flags", _flags)
            //  REMARK: 避免手续费池被薅羊毛，默认兑换比例为供应量最大值。如果需要开启自定义资产支付广播手续费，稍后可设置为合适的值。
            put("core_exchange_rate", JSONObject().apply {
                put("base", JSONObject().apply {
                    put("asset_id", chainMgr.grapheneCoreAssetID)
                    put("amount", n_core_asset_one_pow.toPlainString())
                })
                put("quote", JSONObject().apply {
                    put("asset_id", "1.3.1")
                    put("amount", n_max_supply_pow.toPlainString())
                })
            })
            put("whitelist_authorities", JSONArray())
            put("blacklist_authorities", JSONArray())
            put("whitelist_markets", JSONArray())
            put("blacklist_markets", JSONArray())
            put("description", _description)
        }

        val opdata = JSONObject().apply {
            put("fee", JSONObject().apply {
                put("asset_id", chainMgr.grapheneCoreAssetID)
                put("amount", 0)
            })
            put("issuer", uid)
            put("symbol", sym)
            put("precision", _precision)
            put("common_options", asset_options)
            put("is_prediction_market", false)
            if (_bitasset_options_args != null) {
                put("bitasset_opts", bitasset_opts)
            }
        }

        //  资产名称包含小数点。特殊判断是否已经发行过前缀资产。
        var prefix_symbol: String? = null
        val dot_idx = sym.indexOf('.')
        if (dot_idx >= 0) {
            prefix_symbol = sym.substring(0, dot_idx)
        }

        val promise_map = JSONObject().apply {
            put("kQueryOpFee", BitsharesClientManager.sharedBitsharesClientManager().calcOperationFee(opdata, EBitsharesOperations.ebo_asset_create))
            if (prefix_symbol != null) {
                put("kQueryPrefix", chainMgr.queryAssetData(prefix_symbol))
            }
            if (_bitasset_options_args != null) {
                put("kQueryBackingAsset", chainMgr.queryBackingBackingAsset(_bitasset_options_args!!.getJSONObject("short_backing_asset")))
            }
        }

        VcUtils.simpleRequest(this, Promise.map(promise_map)) {
            val data_hash = it as JSONObject

            //  检测前缀资产
            if (prefix_symbol != null) {
                val prefix_asset = data_hash.optJSONObject("kQueryPrefix")
                if (prefix_asset == null) {
                    showToast(String.format(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidAssetPrefixNameNone), sym, prefix_symbol))
                    return@simpleRequest
                }
                val prefix_issuer = prefix_asset.optString("issuer", null)
                if (prefix_issuer == null || prefix_issuer != uid) {
                    showToast(String.format(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidAssetPrefixNameNoPermission), prefix_symbol))
                    return@simpleRequest
                }
            }
            //  检测背书资产
            if (_bitasset_options_args != null) {
                val backing_backing_asset = data_hash.getJSONObject("kQueryBackingAsset")
                if (ModelUtils.assetIsSmart(backing_backing_asset)) {
                    showToast(String.format(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidBackBackAssetType),
                            _bitasset_options_args!!.getJSONObject("short_backing_asset").getString("symbol"),
                            backing_backing_asset.getString("symbol")))
                    return@simpleRequest
                }
                if (uid == BTS_GRAPHENE_COMMITTEE_ACCOUNT && !ModelUtils.assetIsCore(backing_backing_asset)) {
                    val core_sym = chainMgr.grapheneAssetSymbol
                    showToast(String.format(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidBackAssetForCommittee), core_sym, core_sym))
                    return@simpleRequest
                }
            }
            //  创建资产手续费确认
            val fee_price_item = data_hash.getJSONObject("kQueryOpFee")
            val price = OrgUtils.formatAssetAmountItem(fee_price_item)
            val value = String.format(resources.getString(R.string.kVcAssetOpCreateSubmitAskTipsForCostConfirm), sym, price)
            UtilsAlert.showMessageConfirm(this, resources.getString(R.string.kVcHtlcMessageTipsTitle), value).then {
                if (it != null && it as Boolean) {
                    guardWalletUnlocked(false) { unlocked ->
                        if (unlocked) {
                            onSubmitCore(opdata, account)
                        }
                    }
                }
            }
        }
    }

    private fun onSubmitCore(opdata: JSONObject, opaccount: JSONObject) {
        //  确保有权限发起普通交易，否则作为提案交易处理。
        GuardProposalOrNormalTransaction(EBitsharesOperations.ebo_asset_create, false, false,
                opdata, opaccount) { isProposal, _ ->
            assert(!isProposal)
            //  请求网络广播
            val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
            BitsharesClientManager.sharedBitsharesClientManager().assetCreate(opdata).then {
                mask.dismiss()
                showToast(resources.getString(R.string.kVcAssetOpCreateAssetSubmitTipsOK))
                //  [统计]
                btsppLogCustom("txAssetCreateFullOK", jsonObjectfromKVS("account", opaccount.getString("id")))
                //  返回上一个界面并刷新
                _result_promise?.resolve(true)
                _result_promise = null
                finish()
                return@then null
            }.catch { err ->
                mask.dismiss()
                showGrapheneError(err)
                //  [统计]
                btsppLogCustom("txAssetCreateFailed", jsonObjectfromKVS("account", opaccount.getString("id")))
            }
        }
    }

    /**
     *  事件 - 更新智能币
     */
    private fun onSubmitUpdateBitAssetsClicked() {
        //  校验参数
        if (!_validationBitAssetsArgs()) {
            return
        }

        //  解锁
        guardWalletUnlocked(false) { unlocked ->
            if (unlocked) {
                //  参数
                val opaccount = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account")
                val uid = opaccount.getString("id")

                //  交易数据
                val opdata = JSONObject().apply {
                    put("fee", JSONObject().apply {
                        put("asset_id", ChainObjectManager.sharedChainObjectManager().grapheneCoreAssetID)
                        put("amount", 0)
                    })
                    put("issuer", uid)
                    put("asset_to_update", _edit_asset!!.getString("id"))
                    put("new_options", _bitasset_options_args!!.shadowClone())
                }

                //  确保有权限发起普通交易，否则作为提案交易处理。
                GuardProposalOrNormalTransaction(EBitsharesOperations.ebo_asset_update_bitasset, false, false,
                        opdata, opaccount) { isProposal, _ ->
                    assert(!isProposal)
                    //  请求网络广播
                    val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
                    BitsharesClientManager.sharedBitsharesClientManager().assetUpdateBitasset(opdata).then {
                        mask.dismiss()
                        showToast(resources.getString(R.string.kVcAssetOpUpdateBitassetSubmitTipsOK))
                        //  [统计]
                        btsppLogCustom("txAssetUpdateBitassetFullOK", jsonObjectfromKVS("account", opaccount.getString("id")))
                        //  返回上一个界面并刷新
                        _result_promise?.resolve(true)
                        _result_promise = null
                        finish()
                        return@then null
                    }.catch { err ->
                        mask.dismiss()
                        showGrapheneError(err)
                        //  [统计]
                        btsppLogCustom("txAssetUpdateBitassetFailed", jsonObjectfromKVS("account", opaccount.getString("id")))
                    }
                }
            }
        }
    }

    /**
     *  事件 - 更新资产
     */
    private fun onSubmitEditClicked() {
        if (_max_supply == null || _max_supply!! <= BigDecimal.ZERO) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInputMaxSupply))
            return
        }

        if (ModelUtils.assetIsSmart(_edit_asset!!)) {
            val merged_flags = EBitsharesAssetFlags.ebat_witness_fed_asset.value.or(EBitsharesAssetFlags.ebat_committee_fed_asset.value)
            if (_flags.and(merged_flags) == merged_flags) {
                showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidPermissionWitnessAndCommittee))
                return
            }
        }

        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        val opaccount = WalletManager.sharedWalletManager().getWalletAccountInfo()!!.getJSONObject("account")
        val uid = opaccount.getString("id")

        val n_max_supply_pow = _max_supply!!.multiplyByPowerOf10(_precision)
        val n_max_market_fee_pow = _max_market_fee!!.multiplyByPowerOf10(_precision)

        //  更新部分字段，其他字段维持原样。
        val asset_options = _edit_asset!!.getJSONObject("options").shadowClone()
        asset_options.put("max_supply", n_max_supply_pow.toPlainString())
        asset_options.put("market_fee_percent", _market_fee_percent)
        asset_options.put("max_market_fee", n_max_market_fee_pow.toPlainString())
        asset_options.put("issuer_permissions", _issuer_permissions)
        asset_options.put("flags", _flags)
        asset_options.put("description", _description)

        //  更新扩展字段中的引荐人分成比例，维持其他字段不变。
        asset_options.getJSONObject("extensions").put("reward_percent", _reward_percent)

        val opdata = JSONObject().apply {
            put("fee", JSONObject().apply {
                put("asset_id", chainMgr.grapheneCoreAssetID)
                put("amount", 0)
            })
            put("issuer", uid)
            put("asset_to_update", _edit_asset!!.getString("id"))
            put("new_options", asset_options)
        }

        //  永久禁用某些属性，需要二次确认操作不可逆。
        if (_issuer_permissions.and(_old_issuer_permissions) != _old_issuer_permissions) {

            val value = resources.getString(R.string.kVcAssetOpCreateSubmitAskTipsDisableSomePermission)
            UtilsAlert.showMessageConfirm(this, resources.getString(R.string.kVcHtlcMessageTipsTitle), value).then {
                if (it != null && it as Boolean) {
                    //  二次确认后更新。
                    onSubmitEditCore(opdata, opaccount)
                }
            }
        } else {
            //  不用提示，继续更新。
            onSubmitEditCore(opdata, opaccount)
        }
    }

    /**
     *  (private) 更新资产 - 核心逻辑
     */
    private fun onSubmitEditCore(opdata: JSONObject, opaccount: JSONObject) {
        guardWalletUnlocked(false) { unlocked ->
            if (unlocked) {
                //  确保有权限发起普通交易，否则作为提案交易处理。
                GuardProposalOrNormalTransaction(EBitsharesOperations.ebo_asset_update, false, false,
                        opdata, opaccount) { isProposal, _ ->
                    assert(!isProposal)
                    //  请求网络广播
                    val mask = ViewMask(resources.getString(R.string.kTipsBeRequesting), this).apply { show() }
                    BitsharesClientManager.sharedBitsharesClientManager().assetUpdate(opdata).then {
                        mask.dismiss()
                        showToast(resources.getString(R.string.kVcAssetOpUpdateAssetSubmitTipsOK))
                        //  [统计]
                        btsppLogCustom("txAssetUpdateFullOK", jsonObjectfromKVS("account", opaccount.getString("id")))
                        //  返回上一个界面并刷新
                        _result_promise?.resolve(true)
                        _result_promise = null
                        finish()
                        return@then null
                    }.catch { err ->
                        mask.dismiss()
                        showGrapheneError(err)
                        //  [统计]
                        btsppLogCustom("txAssetUpdateFailed", jsonObjectfromKVS("account", opaccount.getString("id")))
                    }
                }
            }
        }
    }

    private fun _checkAssetSymbolName(symbol: String): Boolean {
        //  TODO:4.0 config GRAPHENE_MIN_ASSET_SYMBOL_LENGTH GRAPHENE_MAX_ASSET_SYMBOL_LENGTH
        if (symbol.isEmpty()) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsPleaseAssetSymbol))
            return false
        }
        if (symbol.length < 3 || symbol.length > 16) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidAssetSymbolLength))
            return false
        }
        val first_symbol = symbol.first()
        if (!Utils.isAlpha(first_symbol)) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidFirstSymbol))
            return false
        }

        val last_symbol = symbol.last()
        if (!Utils.isAlnum(last_symbol)) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidLastSymbol))
            return false
        }

        var dot_already_present = false
        for (c in symbol) {
            if (Utils.isAlnum(c)) {
                continue
            }
            if (c == '.') {
                if (dot_already_present) {
                    showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidDotSymbol))
                    return false
                }
                dot_already_present = true
                continue
            }
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsInvalidOtherSymbols))
            return false
        }

        return true
    }

    /**
     *  (private) 校验智能币相关参数
     */
    private fun _validationBitAssetsArgs(): Boolean {
        val options = _bitasset_options_args!!
        if (!options.has("feed_lifetime_sec") || options.getInt("feed_lifetime_sec") == 0) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsSmartInputFeedLifeTime))
            return false
        }

        if (!options.has("minimum_feeds") || options.getInt("minimum_feeds") == 0) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsSmartInputMinFeedNumber))
            return false
        }

        if (!options.has("force_settlement_delay_sec")) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsSmartInputSettleDelaySec))
            return false
        }

        if (!options.has("force_settlement_offset_percent")) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsSmartInputSettleOffsetPercent))
            return false
        }

        if (!options.has("maximum_force_settlement_volume") || options.getInt("maximum_force_settlement_volume") == 0) {
            showToast(resources.getString(R.string.kVcAssetOpCreateSubmitTipsSmartInputMaxSettleVolumePerHour))
            return false
        }

        return true
    }
}
