package dev.ryu4696.stampmanager

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

data class Usage(
    val company: String,
    val product: String,
    val location: String,
    val lastDelivery: String
)
data class Stamp(val number: String, val usages: List<Usage>)

class MainActivity : Activity() {
    private lateinit var searchBox: EditText
    private lateinit var countText: TextView
    private lateinit var allButton: Button
    private lateinit var starButton: Button
    private lateinit var listView: ListView
    private lateinit var adapter: StampAdapter

    private val allStamps = mutableListOf<Stamp>()
    private val shownStamps = mutableListOf<Stamp>()
    private var starOnly = false
    private val prefs by lazy { getSharedPreferences("stamp_marks", MODE_PRIVATE) }

    private data class DateMaps(
        val common: Map<Int, String>,
        val overrides: Map<Int, String>
    )

    private data class StampSortParts(
        val type: Int,
        val zoneNumber: Int,
        val zoneText: String,
        val position: String,
        val suffix: String,
        val normalized: String
    )

    private val positionOrder = mapOf(
        "上" to 10, "上前" to 11,
        "中" to 20, "中前" to 21,
        "下" to 30, "下前" to 31, "下前後" to 32,
        "前上" to 40, "前中" to 41, "前下" to 42,
        "左上" to 50, "左上前" to 51, "左中" to 52, "左前" to 53, "左" to 54,
        "右上" to 60, "右上前" to 61, "右中" to 62, "右前" to 63, "右" to 64,
        "中左" to 70, "中右" to 71, "上右" to 72
    )

    private val numericOnlyRegex = Regex("^\\d+$")
    private val numericRackRegex = Regex("^(\\d+)(\\D+?)(\\d+)(.*)$")
    private val alphaRackRegex = Regex("^([A-Za-z]+)(\\D*?)(\\d+)(.*)$")
    private val naturalTokenRegex = Regex("\\d+|\\D+")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchBox = findViewById(R.id.searchBox)
        countText = findViewById(R.id.countText)
        allButton = findViewById(R.id.allButton)
        starButton = findViewById(R.id.starButton)
        listView = findViewById(R.id.listView)

        allStamps += loadStamps().sortedWith(Comparator { a, b -> compareStampNumbers(a.number, b.number) })
        adapter = StampAdapter()
        listView.adapter = adapter

        allButton.setOnClickListener {
            starOnly = false
            applyFilter()
        }
        starButton.setOnClickListener {
            starOnly = true
            applyFilter()
        }
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilter()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        applyFilter()
    }

    private fun loadStamps(): List<Stamp> {
        val dateMaps = loadDateMaps()
        val encoded = intArrayOf(
            R.raw.stamp0, R.raw.stamp1, R.raw.stamp2, R.raw.stamp3, R.raw.stamp4,
            R.raw.stamp5, R.raw.stamp6, R.raw.stamp7, R.raw.stamp8
        ).joinToString("") { id ->
            resources.openRawResource(id).bufferedReader(Charsets.US_ASCII).use { it.readText() }
        }
        val packed = Base64.decode(encoded, Base64.DEFAULT)
        val text = GZIPInputStream(ByteArrayInputStream(packed)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONArray(text)
        val result = ArrayList<Stamp>(root.length())

        for (i in 0 until root.length()) {
            val obj = root.getJSONObject(i)
            val stampNumber = obj.getString("s")
            val usagesJson = obj.getJSONArray("u")
            val usages = ArrayList<Usage>(usagesJson.length())

            for (j in 0 until usagesJson.length()) {
                val u = usagesJson.getJSONObject(j)
                val company = u.optString("c")
                val product = u.optString("p")
                val location = u.optString("l")

                val commonDate = dateMaps.common[hash23("$company\u0000$product")] ?: continue
                val overrideDate = dateMaps.overrides[
                    hash23("$stampNumber\u0000$company\u0000$product\u0000$location")
                ]

                usages += Usage(
                    company = company,
                    product = product,
                    location = location,
                    lastDelivery = overrideDate ?: commonDate
                )
            }

            if (usages.isNotEmpty()) {
                usages.sortWith(
                    compareByDescending<Usage> { it.lastDelivery }
                        .thenBy { normalize(it.company) }
                        .thenBy { normalize(it.product) }
                        .thenBy { normalize(it.location) }
                )
                result += Stamp(stampNumber, usages)
            }
        }
        return result
    }

    private fun loadDateMaps(): DateMaps {
        val encoded = intArrayOf(
            R.raw.date0, R.raw.date1, R.raw.date2, R.raw.date3
        ).joinToString("") { id ->
            resources.openRawResource(id).bufferedReader(Charsets.US_ASCII).use { it.readText() }
        }
        val packed = Base64.decode(encoded, Base64.DEFAULT)
        val bytes = GZIPInputStream(ByteArrayInputStream(packed)).use { it.readBytes() }
        var position = 0

        fun readU16(): Int {
            val value = ((bytes[position].toInt() and 0xFF) shl 8) or
                (bytes[position + 1].toInt() and 0xFF)
            position += 2
            return value
        }

        fun readHash23(): Int {
            val value = ((bytes[position].toInt() and 0x7F) shl 16) or
                ((bytes[position + 1].toInt() and 0xFF) shl 8) or
                (bytes[position + 2].toInt() and 0xFF)
            position += 3
            return value
        }

        val commonCount = readU16()
        val overrideCount = readU16()
        val baseDate = LocalDate.of(2020, 1, 1)
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPAN)

        fun readMap(count: Int): HashMap<Int, String> {
            val map = HashMap<Int, String>(count * 2)
            repeat(count) {
                val key = readHash23()
                val offset = readU16()
                map[key] = baseDate.plusDays((offset - 1).toLong()).format(formatter)
            }
            return map
        }

        return DateMaps(
            common = readMap(commonCount),
            overrides = readMap(overrideCount)
        )
    }

    private fun hash23(value: String): Int {
        val crc = CRC32()
        crc.update(value.toByteArray(Charsets.UTF_8))
        return (crc.value and 0x7FFFFF).toInt()
    }

    private fun isStarred(number: String): Boolean = prefs.getBoolean(number, false)

    private fun toggleStar(number: String) {
        prefs.edit().putBoolean(number, !isStarred(number)).apply()
        applyFilter()
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.JAPAN)
        .replace(" ", "")
        .replace("　", "")

    private fun sortNormalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .replace(" ", "")
        .replace("　", "")

    private fun stampSortParts(value: String): StampSortParts {
        val valueNormalized = sortNormalize(value)

        if (numericOnlyRegex.matches(valueNormalized)) {
            return StampSortParts(
                type = 0,
                zoneNumber = valueNormalized.toIntOrNull() ?: Int.MAX_VALUE,
                zoneText = "",
                position = "",
                suffix = valueNormalized,
                normalized = valueNormalized
            )
        }

        numericRackRegex.matchEntire(valueNormalized)?.let { match ->
            val zone = match.groupValues[1]
            val rackPosition = match.groupValues[2]
            val serial = match.groupValues[3]
            val rest = match.groupValues[4]
            return StampSortParts(
                type = 1,
                zoneNumber = zone.toIntOrNull() ?: Int.MAX_VALUE,
                zoneText = "",
                position = rackPosition,
                suffix = serial + rest,
                normalized = valueNormalized
            )
        }

        alphaRackRegex.matchEntire(valueNormalized)?.let { match ->
            val zone = match.groupValues[1].uppercase(Locale.JAPAN)
            val rackPosition = match.groupValues[2]
            val serial = match.groupValues[3]
            val rest = match.groupValues[4]
            return StampSortParts(
                type = 2,
                zoneNumber = 0,
                zoneText = zone,
                position = rackPosition,
                suffix = serial + rest,
                normalized = valueNormalized
            )
        }

        return StampSortParts(
            type = 3,
            zoneNumber = 0,
            zoneText = valueNormalized,
            position = "",
            suffix = "",
            normalized = valueNormalized
        )
    }

    private fun compareNatural(a: String, b: String): Int {
        if (a == b) return 0
        val left = naturalTokenRegex.findAll(a).map { it.value }.toList()
        val right = naturalTokenRegex.findAll(b).map { it.value }.toList()
        val common = minOf(left.size, right.size)

        for (i in 0 until common) {
            val l = left[i]
            val r = right[i]
            val ln = l.toLongOrNull()
            val rn = r.toLongOrNull()
            val compared = if (ln != null && rn != null) {
                val numberCompared = ln.compareTo(rn)
                if (numberCompared != 0) numberCompared else l.length.compareTo(r.length)
            } else {
                l.compareTo(r)
            }
            if (compared != 0) return compared
        }
        return left.size.compareTo(right.size)
    }

    private fun compareStampNumbers(a: String, b: String): Int {
        val left = stampSortParts(a)
        val right = stampSortParts(b)

        left.type.compareTo(right.type).takeIf { it != 0 }?.let { return it }

        when (left.type) {
            0, 1 -> left.zoneNumber.compareTo(right.zoneNumber).takeIf { it != 0 }?.let { return it }
            2, 3 -> compareNatural(left.zoneText, right.zoneText).takeIf { it != 0 }?.let { return it }
        }

        val leftPositionRank = positionOrder[left.position] ?: 100
        val rightPositionRank = positionOrder[right.position] ?: 100
        leftPositionRank.compareTo(rightPositionRank).takeIf { it != 0 }?.let { return it }
        compareNatural(left.position, right.position).takeIf { it != 0 }?.let { return it }
        compareNatural(left.suffix, right.suffix).takeIf { it != 0 }?.let { return it }
        compareNatural(left.normalized, right.normalized).takeIf { it != 0 }?.let { return it }
        return a.compareTo(b)
    }

    private fun applyFilter() {
        val q = normalize(searchBox.text?.toString().orEmpty())
        shownStamps.clear()
        for (stamp in allStamps) {
            if (starOnly && !isStarred(stamp.number)) continue
            if (q.isNotEmpty()) {
                var hit = normalize(stamp.number).contains(q)
                if (!hit) {
                    hit = stamp.usages.any {
                        normalize(it.company).contains(q) ||
                            normalize(it.product).contains(q) ||
                            normalize(it.location).contains(q) ||
                            normalize(it.lastDelivery).contains(q)
                    }
                }
                if (!hit) continue
            }
            shownStamps += stamp
        }
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
        val starCount = allStamps.count { isStarred(it.number) }
        val prefix = if (shownStamps.size == allStamps.size && !starOnly && q.isEmpty()) "${allStamps.size} 印判" else "${shownStamps.size} / ${allStamps.size} 印判"
        countText.text = "$prefix  ・  ★ $starCount"
        setFilterButtonState()
    }

    private fun setFilterButtonState() {
        styleFilterButton(allButton, !starOnly)
        styleFilterButton(starButton, starOnly)
    }

    private fun styleFilterButton(button: Button, active: Boolean) {
        button.background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(if (active) Color.parseColor("#4B63D3") else Color.WHITE)
            setStroke(dp(1), if (active) Color.parseColor("#4B63D3") else Color.parseColor("#DFE3ED"))
        }
        button.setTextColor(if (active) Color.WHITE else Color.parseColor("#4D5264"))
        button.stateListAnimator = null
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private inner class StampAdapter : BaseAdapter() {
        private val expanded = hashSetOf<String>()

        override fun getCount(): Int = shownStamps.size
        override fun getItem(position: Int): Stamp = shownStamps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: Holder
            val root: LinearLayout
            if (convertView == null) {
                root = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16), dp(14), dp(12), dp(14))
                    background = getDrawable(R.drawable.card_bg)
                    isClickable = true
                    isFocusable = true
                }
                val top = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val number = TextView(this@MainActivity).apply {
                    setTextColor(Color.parseColor("#171A24"))
                    textSize = 19f
                    setTypeface(typeface, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val count = TextView(this@MainActivity).apply {
                    setTextColor(Color.parseColor("#8B90A0"))
                    textSize = 12f
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(6), 0, dp(8), 0)
                }
                val star = TextView(this@MainActivity).apply {
                    textSize = 27f
                    gravity = Gravity.CENTER
                    background = getDrawable(R.drawable.star_bg)
                    layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
                }
                top.addView(number)
                top.addView(count)
                top.addView(star)

                val body = TextView(this@MainActivity).apply {
                    setTextColor(Color.parseColor("#414657"))
                    textSize = 14f
                    setLineSpacing(0f, 1.12f)
                    setPadding(0, dp(8), dp(4), 0)
                }
                val more = TextView(this@MainActivity).apply {
                    setTextColor(Color.parseColor("#4B63D3"))
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(7), 0, 0)
                }
                root.addView(top)
                root.addView(body)
                root.addView(more)
                holder = Holder(number, count, star, body, more)
                root.tag = holder
            } else {
                root = convertView as LinearLayout
                holder = root.tag as Holder
            }

            val stamp = getItem(position)
            val isExpanded = expanded.contains(stamp.number)
            val isMarked = isStarred(stamp.number)
            holder.number.text = stamp.number
            holder.count.text = "${stamp.usages.size}件"
            holder.star.text = if (isMarked) "★" else "☆"
            holder.star.setTextColor(if (isMarked) Color.parseColor("#F2B53C") else Color.parseColor("#989EAD"))
            holder.star.contentDescription = if (isMarked) "マークを外す" else "マークする"

            val visibleUsages = if (isExpanded || stamp.usages.size <= 2) stamp.usages else stamp.usages.take(2)
            holder.body.text = visibleUsages.joinToString("\n\n") { usageText(it) }
            holder.more.text = when {
                stamp.usages.size <= 2 -> ""
                isExpanded -> "▲ 閉じる"
                else -> "▼ 他 ${stamp.usages.size - 2}件を表示"
            }
            holder.more.visibility = if (stamp.usages.size <= 2) View.GONE else View.VISIBLE

            holder.star.setOnClickListener { toggleStar(stamp.number) }
            root.setOnClickListener {
                if (stamp.usages.size > 2) {
                    if (expanded.contains(stamp.number)) expanded.remove(stamp.number) else expanded.add(stamp.number)
                    notifyDataSetChanged()
                }
            }
            return root
        }

        private fun usageText(u: Usage): String {
            val lines = mutableListOf<String>()
            lines += "企業  ${u.company.ifBlank { "（未登録）" }}"
            lines += "商品  ${u.product.ifBlank { "（未登録）" }}"
            lines += "最終納品日  ${u.lastDelivery.ifBlank { "（未登録）" }}"
            if (u.location.isNotBlank()) lines += "場所  ${u.location}"
            return lines.joinToString("\n")
        }
    }

    private data class Holder(
        val number: TextView,
        val count: TextView,
        val star: TextView,
        val body: TextView,
        val more: TextView
    )
}
