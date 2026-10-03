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
import java.util.Locale
import java.util.zip.GZIPInputStream

data class Usage(val company: String, val product: String, val location: String)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchBox = findViewById(R.id.searchBox)
        countText = findViewById(R.id.countText)
        allButton = findViewById(R.id.allButton)
        starButton = findViewById(R.id.starButton)
        listView = findViewById(R.id.listView)

        allStamps += loadStamps()
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
        val encoded = resources.openRawResource(R.raw.stamps).bufferedReader(Charsets.US_ASCII).use { it.readText() }
        val packed = Base64.decode(encoded, Base64.DEFAULT)
        val text = GZIPInputStream(ByteArrayInputStream(packed)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONArray(text)
        val result = ArrayList<Stamp>(root.length())
        for (i in 0 until root.length()) {
            val obj = root.getJSONObject(i)
            val usagesJson = obj.getJSONArray("u")
            val usages = ArrayList<Usage>(usagesJson.length())
            for (j in 0 until usagesJson.length()) {
                val u = usagesJson.getJSONObject(j)
                usages += Usage(
                    company = u.optString("c"),
                    product = u.optString("p"),
                    location = u.optString("l")
                )
            }
            result += Stamp(obj.getString("s"), usages)
        }
        return result
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
                            normalize(it.location).contains(q)
                    }
                }
                if (!hit) continue
            }
            shownStamps += stamp
        }
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
        val starCount = allStamps.count { isStarred(it.number) }
        val prefix = if (shownStamps.size == allStamps.size && !starOnly && q.isEmpty()) "\${allStamps.size} 印判" else "\${shownStamps.size} / \${allStamps.size} 印判"
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
            holder.count.text = "\${stamp.usages.size}件"
            holder.star.text = if (isMarked) "★" else "☆"
            holder.star.setTextColor(if (isMarked) Color.parseColor("#F2B53C") else Color.parseColor("#989EAD"))
            holder.star.contentDescription = if (isMarked) "マークを外す" else "マークする"

            val visibleUsages = if (isExpanded || stamp.usages.size <= 2) stamp.usages else stamp.usages.take(2)
            holder.body.text = visibleUsages.joinToString("\n\n") { usageText(it) }
            holder.more.text = when {
                stamp.usages.size <= 2 -> ""
                isExpanded -> "▲ 閉じる"
                else -> "▼ 他 \${stamp.usages.size - 2}件を表示"
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
            lines += "企業  \${u.company.ifBlank { "（未登録）" }}"
            lines += "商品  \${u.product.ifBlank { "（未登録）" }}"
            if (u.location.isNotBlank()) lines += "場所  \${u.location}"
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
