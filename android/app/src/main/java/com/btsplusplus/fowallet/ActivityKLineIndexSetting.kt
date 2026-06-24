package com.btsplusplus.fowallet

import android.os.Bundle
import android.view.View
import android.widget.TextView
import bitshares.*
import com.btsplusplus.fowallet.databinding.ActivityKlineQuotaSettingBinding
import org.json.JSONArray
import org.json.JSONObject

class ActivityKLineIndexSetting : BtsppActivity() {

    private lateinit var binding: ActivityKlineQuotaSettingBinding

    private lateinit var _result_promise: Promise
    private lateinit var _picker_data_array: MutableList<Int>
    private lateinit var _configValueHash: JSONObject

    private lateinit var _main_index_type_array: JSONArray
    private lateinit var _sub_index_type_array: JSONArray

    override fun onBackClicked(result: Any?) {
        _result_promise.resolve(false)
        super.onBackClicked(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKlineQuotaSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  get params
        _picker_data_array = mutableListOf()
        val args = btspp_args_as_JSONObject()
        _result_promise = args.get("result_promise") as Promise

        _configValueHash = JSONObject(SettingManager.sharedSettingManager().getKLineIndexInfos().toString())

        _main_index_type_array = jsonArrayfrom(jsonObjectfromKVS("name", resources.getString(R.string.kKlineIndexCellHide), "value", ""),
                jsonObjectfromKVS("name", "MA", "value", "ma"),
                jsonObjectfromKVS("name", "EMA", "value", "ema"),
                jsonObjectfromKVS("name", "BOLL", "value", "boll"))

        _sub_index_type_array = jsonArrayfrom(jsonObjectfromKVS("name", resources.getString(R.string.kKlineIndexCellHide), "value", ""),
                jsonObjectfromKVS("name", "MACD", "value", "macd"))

        //  refresh ui
        refreshIndexAll()

        binding.layoutBackFromKlineQuotaSetting.setOnClickListener { onBackClicked(false) }
        binding.layoutMain.setOnClickListener { onMainViewClick() }
        binding.layoutSub.setOnClickListener { onSubViewClick() }
        binding.buttonCommit.setOnClickListener { onCommitClicked() }

        //  index binding events
        binding.layoutMainIndex01.setOnClickListener { onMainIndexClicked(0) }
        binding.layoutMainIndex02.setOnClickListener { onMainIndexClicked(1) }
        binding.layoutMainIndex03.setOnClickListener { onMainIndexClicked(2) }
        binding.layoutSubIndex01.setOnClickListener { onSubIndexClicked(0) }
        binding.layoutSubIndex02.setOnClickListener { onSubIndexClicked(1) }
        binding.layoutSubIndex03.setOnClickListener { onSubIndexClicked(2) }
    }

    private fun onCommitClicked() {
        SettingManager.sharedSettingManager().setUseConfig(kSettingKey_KLineIndexInfo, _configValueHash)
        _result_promise.resolve(true)
        finish()
    }

    private fun onMainIndexClicked(row: Int) {
        val value_type = _configValueHash.optString("kMain")
        if (value_type == "ma") {
            onSelectIndexMA(row)
        } else if (value_type == "ema") {
            onSelectIndexEMA(row)
        } else if (value_type == "boll") {
            onSelectIndexBOLL(row)
        }
    }

    private fun onSubIndexClicked(row: Int) {
        val value_type = _configValueHash.optString("kSub")
        if (value_type == "macd") {
            onSelectIndexMACD(row)
        }
    }

    private fun onSelectNumberFromRange(title: String, bgn: Int, end: Int, current_value: Int): Promise {
        val p = Promise()

        val nameList = JSONArray()
        val valueList = JSONArray()
        var default_select = -1
        for (i in bgn..end) {
            if (i == 0) {
                nameList.put(resources.getString(R.string.kKlineIndexCellHide))
            } else {
                nameList.put(i.toString())
            }
            if (i == current_value) {
                default_select = nameList.length() - 1
            }
            valueList.put(i)
        }

        ViewDialogNumberPicker(this, title, nameList, null, default_select) { _index: Int, _: String ->
            p.resolve(valueList.getInt(_index))
        }.show()

        return p
    }

    private fun onSelectIndexMA(row: Int) {
        onSelectNumberFromRange("MA${row + 1}", 0, 120, _configValueHash.getJSONArray("ma_value").getInt(row)).then {
            val value = it as Int
            _configValueHash.getJSONArray("ma_value").put(row, value)
            when (row) {
                0 -> refreshIndexValueLabel(binding.mainIndex01Value, value)
                1 -> refreshIndexValueLabel(binding.mainIndex02Value, value)
                2 -> refreshIndexValueLabel(binding.mainIndex03Value, value)
            }
            return@then null
        }
    }

    private fun onSelectIndexEMA(row: Int) {
        onSelectNumberFromRange("EMA${row + 1}", 0, 120, _configValueHash.getJSONArray("ema_value").getInt(row)).then {
            val value = it as Int
            _configValueHash.getJSONArray("ema_value").put(row, value)
            when (row) {
                0 -> refreshIndexValueLabel(binding.mainIndex01Value, value)
                1 -> refreshIndexValueLabel(binding.mainIndex02Value, value)
                2 -> refreshIndexValueLabel(binding.mainIndex03Value, value)
            }
            return@then null
        }
    }

    private fun onSelectIndexBOLL(row: Int) {
        val key: String
        val title: String
        val bgn: Int
        val end: Int
        if (row == 0) {
            key = "n"
            title = resources.getString(R.string.kKlineIndexCellBollN)
            bgn = 1
            end = 120
        } else {
            key = "p"
            title = resources.getString(R.string.kKlineIndexCellBollP)
            bgn = 1
            end = 9
        }
        onSelectNumberFromRange(title, bgn, end, _configValueHash.getJSONObject("boll_value").getInt(key)).then {
            val value = it as Int
            _configValueHash.getJSONObject("boll_value").put(key, value)
            when (row) {
                0 -> binding.mainIndex01Value.text = value.toString()
                1 -> binding.mainIndex02Value.text = value.toString()
            }
            return@then null
        }
    }

    private fun onSelectIndexMACD(row: Int) {
        val title = when (row) {
            0 -> resources.getString(R.string.kKlineIndexCellMacdS)
            1 -> resources.getString(R.string.kKlineIndexCellMacdL)
            else -> resources.getString(R.string.kKlineIndexCellMacdM)
        }
        val key = when (row) {
            0 -> "s"
            1 -> "l"
            else -> "m"
        }
        onSelectNumberFromRange(title, 2, 120, _configValueHash.getJSONObject("macd_value").getInt(key)).then {
            val value = it as Int
            when (row) {
                0 -> {
                    _configValueHash.getJSONObject("macd_value").put(key, value)
                    binding.subIndex01Value.text = value.toString()
                }
                1 -> {
                    _configValueHash.getJSONObject("macd_value").put(key, value)
                    binding.subIndex02Value.text = value.toString()
                }
                2 -> {
                    _configValueHash.getJSONObject("macd_value").put(key, value)
                    binding.subIndex03Value.text = value.toString()
                }
            }
            return@then null
        }
    }

    private fun refreshIndexAll() {
        refreshMainIndexAll()
        refreshSubIndexAll()
    }

    private fun refreshIndexValueLabel(label: TextView, value: Int) {
        if (value > 0) {
            label.text = value.toString()
        } else {
            label.text = resources.getString(R.string.kKlineIndexCellHide)
        }
    }

    private fun refreshMainIndexAll() {
        binding.layoutMainIndex01.visibility = View.GONE
        binding.layoutMainIndex02.visibility = View.GONE
        binding.layoutMainIndex03.visibility = View.GONE

        val value_type = _configValueHash.optString("kMain")
        if (value_type == "") {
            binding.layoutMainIndexAll.visibility = View.GONE
            binding.layoutMainValue.text = resources.getString(R.string.kKlineIndexCellHide)
        } else {
            binding.layoutMainIndexAll.visibility = View.VISIBLE
            binding.layoutMainValue.text = value_type.uppercase()
            if (value_type == "ma") {
                val value_values = _configValueHash.getJSONArray("${value_type}_value")
                assert(value_values.length() == 3)
                binding.layoutMainIndex01.visibility = View.VISIBLE
                binding.layoutMainIndex02.visibility = View.VISIBLE
                binding.layoutMainIndex03.visibility = View.VISIBLE
                binding.mainIndex01Name.text = "MA1"
                binding.mainIndex02Name.text = "MA2"
                binding.mainIndex03Name.text = "MA3"
                binding.mainIndex01Name.setTextColor(resources.getColor(R.color.theme01_ma5Color))
                binding.mainIndex02Name.setTextColor(resources.getColor(R.color.theme01_ma10Color))
                binding.mainIndex03Name.setTextColor(resources.getColor(R.color.theme01_ma30Color))
                refreshIndexValueLabel(binding.mainIndex01Value, value_values.getInt(0))
                refreshIndexValueLabel(binding.mainIndex02Value, value_values.getInt(1))
                refreshIndexValueLabel(binding.mainIndex03Value, value_values.getInt(2))
            } else if (value_type == "ema") {
                val value_values = _configValueHash.getJSONArray("${value_type}_value")
                assert(value_values.length() == 3)
                binding.layoutMainIndex01.visibility = View.VISIBLE
                binding.layoutMainIndex02.visibility = View.VISIBLE
                binding.layoutMainIndex03.visibility = View.VISIBLE
                binding.mainIndex01Name.text = "EMA1"
                binding.mainIndex02Name.text = "EMA2"
                binding.mainIndex03Name.text = "EMA3"
                binding.mainIndex01Name.setTextColor(resources.getColor(R.color.theme01_ma5Color))
                binding.mainIndex02Name.setTextColor(resources.getColor(R.color.theme01_ma10Color))
                binding.mainIndex03Name.setTextColor(resources.getColor(R.color.theme01_ma30Color))
                refreshIndexValueLabel(binding.mainIndex01Value, value_values.getInt(0))
                refreshIndexValueLabel(binding.mainIndex02Value, value_values.getInt(1))
                refreshIndexValueLabel(binding.mainIndex03Value, value_values.getInt(2))
            } else if (value_type == "boll") {
                val value_values = _configValueHash.getJSONObject("${value_type}_value")
                assert(value_values.length() == 2)
                binding.layoutMainIndex01.visibility = View.VISIBLE
                binding.layoutMainIndex02.visibility = View.VISIBLE
                binding.mainIndex01Name.text = resources.getString(R.string.kKlineIndexCellBollN)
                binding.mainIndex02Name.text = resources.getString(R.string.kKlineIndexCellBollP)
                binding.mainIndex01Name.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                binding.mainIndex02Name.setTextColor(resources.getColor(R.color.theme01_textColorMain))
                binding.mainIndex01Value.text = value_values.getInt("n").toString()
                binding.mainIndex02Value.text = value_values.getInt("p").toString()
            } else {
                assert(false)
            }
        }
    }

    private fun refreshSubIndexAll() {
        binding.layoutSubIndex01.visibility = View.GONE
        binding.layoutSubIndex02.visibility = View.GONE
        binding.layoutSubIndex03.visibility = View.GONE

        val value_type = _configValueHash.optString("kSub")
        if (value_type == "") {
            binding.layoutSubIndexAll.visibility = View.GONE
            binding.layoutSubValue.text = resources.getString(R.string.kKlineIndexCellHide)
        } else {
            binding.layoutSubIndexAll.visibility = View.VISIBLE
            binding.layoutSubValue.text = value_type.uppercase()
            if (value_type == "macd") {
                val value_values = _configValueHash.getJSONObject("${value_type}_value")
                assert(value_values.length() == 3)
                binding.layoutSubIndex01.visibility = View.VISIBLE
                binding.layoutSubIndex02.visibility = View.VISIBLE
                binding.layoutSubIndex03.visibility = View.VISIBLE
                binding.subIndex01Name.text = resources.getString(R.string.kKlineIndexCellMacdS)
                binding.subIndex02Name.text = resources.getString(R.string.kKlineIndexCellMacdL)
                binding.subIndex03Name.text = resources.getString(R.string.kKlineIndexCellMacdM)
                binding.subIndex01Value.text = value_values.getInt("s").toString()
                binding.subIndex02Value.text = value_values.getInt("l").toString()
                binding.subIndex03Value.text = value_values.getInt("m").toString()
            } else {
                assert(false)
            }
        }
    }

    private fun onMainViewClick() {
        val list = JSONArray()
        _main_index_type_array.forEach<JSONObject> { list.put(it!!.getString("name")) }
        ViewSelector.show(this, resources.getString(R.string.kKlineIndexSelectMainIndex), list.toList<String>().toTypedArray()) { index: Int, result: String ->
            binding.layoutMainValue.text = result
            _configValueHash.put("kMain", _main_index_type_array.getJSONObject(index).getString("value"))
            refreshMainIndexAll()
        }
    }

    private fun onSubViewClick() {
        val list = JSONArray()
        _sub_index_type_array.forEach<JSONObject> { list.put(it!!.getString("name")) }
        ViewSelector.show(this, resources.getString(R.string.kKlineIndexSelectSubIndex), list.toList<String>().toTypedArray()) { index: Int, result: String ->
            binding.layoutSubValue.text = result
            _configValueHash.put("kSub", _sub_index_type_array.getJSONObject(index).getString("value"))
            refreshSubIndexAll()
        }
    }
}
