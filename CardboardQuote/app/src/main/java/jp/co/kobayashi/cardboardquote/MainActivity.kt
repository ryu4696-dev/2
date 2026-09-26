package jp.co.kobayashi.cardboardquote

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

class MainActivity : Activity() {
    private var selectedFlute = Flute.AF
    private var selectedMaterial = MaterialMaster.materials.first { it.name == "K6XK6" }
    private val fluteButtons = mutableMapOf<Flute, TextView>()
    private val quoteLines = mutableListOf<QuoteLine>()

    private lateinit var customerEdit: EditText
    private lateinit var personEdit: EditText
    private lateinit var productEdit: EditText
    private lateinit var materialButton: TextView
    private lateinit var lengthEdit: EditText
    private lateinit var widthEdit: EditText
    private lateinit var depthEdit: EditText
    private lateinit var printColorsEdit: EditText
    private lateinit var lotEdit: EditText
    private lateinit var processEdit: EditText
    private lateinit var plateEdit: EditText
    private lateinit var dieEdit: EditText
    private lateinit var noteEdit: EditText
    private lateinit var unitText: TextView
    private lateinit var materialSummary: TextView
    private lateinit var dimensionSummary: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var boxView: CardboardBoxView
    private lateinit var developmentView: DevelopmentView
    private lateinit var quoteListText: TextView

    private var latestInput: QuoteInput? = null
    private var latestResult: QuoteResult? = null
    private var pendingPdfLines: List<QuoteLine> = emptyList()
    private var pendingCustomer = ""
    private var pendingPerson = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = GREEN_DARK

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(32))
            setBackgroundColor(BG)
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        })

        root.addView(label("段ボール簡易見積", 24, true, GREEN_DARK))
        root.addView(label("Excel見積書PDF対応", 13, false, TEXT_SUB), lp(mb = 12))

        root.addView(sectionLabel("お客様名"), lp(mb = 4))
        customerEdit = textEdit("株式会社〇〇〇〇")
        root.addView(customerEdit, lp(h = 58))

        root.addView(sectionLabel("ご担当者名（任意）"), lp(mt = 12, mb = 4))
        personEdit = textEdit("山田 太郎")
        root.addView(personEdit, lp(h = 58))

        root.addView(sectionLabel("品名"), lp(mt = 12, mb = 4))
        productEdit = textEdit("品名")
        root.addView(productEdit, lp(h = 58))

        root.addView(sectionLabel("フルート"), lp(mt = 16, mb = 4))
        val fluteRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(Flute.AF, Flute.BF, Flute.WF).forEachIndexed { index, flute ->
            val b = TextView(this).apply {
                text = flute.label
                textSize = 18f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setOnClickListener {
                    selectedFlute = flute
                    refreshFluteButtons()
                    invalidatePrice()
                }
            }
            fluteButtons[flute] = b
            fluteRow.addView(b, LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                if (index > 0) leftMargin = dp(7)
            })
        }
        root.addView(fluteRow)
        refreshFluteButtons()

        root.addView(sectionLabel("寸法（mm）"), lp(mt = 16, mb = 4))
        val dims = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        lengthEdit = numEdit("長")
        widthEdit = numEdit("巾")
        depthEdit = numEdit("深")
        listOf(lengthEdit, widthEdit, depthEdit).forEachIndexed { index, edit ->
            dims.addView(edit, LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                if (index > 0) leftMargin = dp(8)
            })
        }
        root.addView(dims)

        root.addView(sectionLabel("材質"), lp(mt = 16, mb = 4))
        materialButton = TextView(this).apply {
            text = "${displayMaterial(selectedMaterial.name)}   ▼"
            textSize = 17f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            setTextColor(TEXT)
            background = fieldBackground()
            setOnClickListener { showMaterialDialog() }
        }
        root.addView(materialButton, lp(h = 60))

        root.addView(sectionLabel("印刷色数 / ロット"), lp(mt = 16, mb = 4))
        val printLotRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        printColorsEdit = numEdit("色数").apply { setText("0") }
        lotEdit = numEdit("ロット").apply { setText("1000") }
        printLotRow.addView(printColorsEdit, LinearLayout.LayoutParams(0, dp(60), 1f))
        printLotRow.addView(lotEdit, LinearLayout.LayoutParams(0, dp(60), 1f).apply { leftMargin = dp(8) })
        root.addView(printLotRow)

        root.addView(sectionLabel("加工賃（円 / ㎡）"), lp(mt = 16, mb = 4))
        processEdit = numEdit("加工賃", decimal = true).apply { setText("10") }
        root.addView(processEdit, lp(h = 60))

        root.addView(sectionLabel("印版代 / 木型代（円）"), lp(mt = 16, mb = 4))
        val extraRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        plateEdit = numEdit("印版代").apply { setText("0") }
        dieEdit = numEdit("木型代").apply { setText("0") }
        extraRow.addView(plateEdit, LinearLayout.LayoutParams(0, dp(60), 1f))
        extraRow.addView(dieEdit, LinearLayout.LayoutParams(0, dp(60), 1f).apply { leftMargin = dp(8) })
        root.addView(extraRow)

        root.addView(sectionLabel("備考（任意）"), lp(mt = 16, mb = 4))
        noteEdit = textEdit("備考").apply { setSingleLine(true) }
        root.addView(noteEdit, lp(h = 58))

        val calc = actionButton("計算する", true)
        root.addView(calc, lp(mt = 18, h = 58))
        calc.setOnClickListener { calculate() }

        resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(16))
            background = roundedBg(Color.WHITE, radius = 16f)
            visibility = View.GONE
        }
        boxView = CardboardBoxView(this)
        resultCard.addView(boxView, lp(h = 250))
        developmentView = DevelopmentView(this)
        resultCard.addView(developmentView, lp(mt = 8, h = 260))
        materialSummary = label("", 18, true).apply { gravity = Gravity.CENTER }
        resultCard.addView(materialSummary, lp(mt = 10))
        dimensionSummary = label("", 17, false).apply { gravity = Gravity.CENTER }
        resultCard.addView(dimensionSummary, lp(mt = 4))
        unitText = label("— 円 / 個", 38, true, GREEN_DARK).apply { gravity = Gravity.CENTER }
        resultCard.addView(unitText, lp(mt = 8))
        root.addView(resultCard, lp(mt = 14))

        val add = actionButton("この品目を見積書に追加", true)
        root.addView(add, lp(mt = 12, h = 58))
        add.setOnClickListener { addCurrentLine() }

        quoteListText = TextView(this).apply {
            textSize = 14f
            setTextColor(TEXT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBg(Color.WHITE, BORDER, 1, 12f)
        }
        root.addView(quoteListText, lp(mt = 10))
        refreshQuoteList()

        val clear = actionButton("見積行をクリア", false)
        root.addView(clear, lp(mt = 8, h = 50))
        clear.setOnClickListener {
            quoteLines.clear()
            refreshQuoteList()
        }

        val pdf = actionButton("見積書PDFを作成", true)
        root.addView(pdf, lp(mt = 14, h = 62))
        pdf.setOnClickListener { requestPdfExport() }

        attachInvalidators(lengthEdit, widthEdit, depthEdit, processEdit, printColorsEdit, lotEdit, plateEdit, dieEdit, productEdit, noteEdit)
    }

    private fun attachInvalidators(vararg edits: EditText) {
        edits.forEach { edit ->
            edit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = invalidatePrice()
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
    }

    private fun refreshFluteButtons() {
        fluteButtons.forEach { (flute, view) ->
            val selected = flute == selectedFlute
            view.setTextColor(if (selected) Color.WHITE else GREEN_DARK)
            view.background = roundedBg(
                if (selected) GREEN_DARK else Color.WHITE,
                if (selected) GREEN_DARK else BORDER,
                if (selected) 0 else 1,
                14f
            )
        }
    }

    private fun showMaterialDialog() {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val search = EditText(this).apply {
            hint = "材質を検索"
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = fieldBackground()
        }
        val list = ListView(this).apply { dividerHeight = 1 }
        shell.addView(search, lp(h = 56))
        shell.addView(list, lp(mt = 8, h = 430))

        var visible = MaterialMaster.materials.toList()
        fun bind() {
            list.adapter = object : ArrayAdapter<Material>(this, android.R.layout.simple_list_item_1, visible) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = super.getView(position, convertView, parent) as TextView
                    tv.text = displayMaterial(getItem(position)?.name.orEmpty())
                    tv.textSize = 17f
                    tv.setTextColor(TEXT)
                    tv.setPadding(dp(14), dp(10), dp(14), dp(10))
                    return tv
                }
            }
        }
        bind()
        val dialog = AlertDialog.Builder(this).setTitle("材質を選択").setView(shell).setNegativeButton("閉じる", null).create()
        list.setOnItemClickListener { _, _, position, _ ->
            selectedMaterial = visible[position]
            materialButton.text = "${displayMaterial(selectedMaterial.name)}   ▼"
            invalidatePrice()
            dialog.dismiss()
        }
        search.addTextChangedListener(SimpleTextWatcher { input ->
            val q = input.trim().uppercase(Locale.ROOT).replace("X", "")
            visible = if (q.isEmpty()) MaterialMaster.materials else MaterialMaster.materials.filter {
                it.name.uppercase(Locale.ROOT).replace("X", "").contains(q)
            }
            bind()
        })
        dialog.show()
    }

    private fun calculate(): Boolean = try {
        val input = QuoteInput(
            flute = selectedFlute,
            length = lengthEdit.text.toString().toInt(),
            width = widthEdit.text.toString().toInt(),
            depth = depthEdit.text.toString().toInt(),
            material = selectedMaterial,
            processRate = processEdit.text.toString().toDoubleOrNull() ?: 0.0
        )
        val result = Calculator.calculate(input)
        val lot = lotEdit.text.toString().toIntOrNull() ?: 0
        require(lot > 0) { "ロットを入力してください" }
        latestInput = input
        latestResult = result
        boxView.lengthMm = input.length
        boxView.widthMm = input.width
        boxView.depthMm = input.depth
        developmentView.lengthMm = input.length
        developmentView.widthMm = input.width
        developmentView.depthMm = input.depth
        developmentView.flute = input.flute
        materialSummary.text = "${displayMaterial(input.material.name)}  ${input.flute.label}"
        dimensionSummary.text = "${input.length} × ${input.width} × ${input.depth} mm"
        unitText.text = "${nf(result.unitPrice)} 円 / 個"
        resultCard.visibility = View.VISIBLE
        true
    } catch (e: Exception) {
        Toast.makeText(this, e.message ?: "入力を確認してください", Toast.LENGTH_SHORT).show()
        false
    }

    private fun buildCurrentLine(): QuoteLine? {
        if (!calculate()) return null
        val i = latestInput ?: return null
        val r = latestResult ?: return null
        val lot = lotEdit.text.toString().toIntOrNull() ?: return null
        return QuoteLine(
            product = productEdit.text.toString().trim().ifEmpty { "段ボールケース" },
            material = displayMaterial(i.material.name),
            flute = i.flute.label,
            length = i.length,
            width = i.width,
            depth = i.depth,
            printColors = printColorsEdit.text.toString().toIntOrNull() ?: 0,
            lot = lot,
            unitPrice = r.unitPrice,
            total = r.unitPrice * lot,
            plate = plateEdit.text.toString().toIntOrNull() ?: 0,
            die = dieEdit.text.toString().toIntOrNull() ?: 0,
            note = noteEdit.text.toString().trim()
        )
    }

    private fun addCurrentLine() {
        if (quoteLines.size >= 12) {
            Toast.makeText(this, "見積書は12行までです", Toast.LENGTH_SHORT).show()
            return
        }
        val line = buildCurrentLine() ?: return
        quoteLines.add(line)
        refreshQuoteList()
        Toast.makeText(this, "${quoteLines.size}行目に追加しました", Toast.LENGTH_SHORT).show()
    }

    private fun refreshQuoteList() {
        quoteListText.text = if (quoteLines.isEmpty()) {
            "見積書：0 / 12行\n計算した品目を追加するとここに並びます。"
        } else {
            buildString {
                append("見積書：${quoteLines.size} / 12行")
                quoteLines.forEachIndexed { index, q ->
                    append("\n${index + 1}. ${q.product}  ${q.material} ${q.flute}  ${q.length}×${q.width}×${q.depth}  ${nf(q.unitPrice)}円 × ${nf(q.lot)}")
                }
            }
        }
    }

    private fun requestPdfExport() {
        val lines = if (quoteLines.isNotEmpty()) quoteLines.toList() else listOfNotNull(buildCurrentLine())
        if (lines.isEmpty()) return
        pendingPdfLines = lines
        pendingCustomer = customerEdit.text.toString().trim()
        pendingPerson = personEdit.text.toString().trim()
        val stamp = SimpleDateFormat("yyyyMMdd", Locale.JAPAN).format(Date())
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "御見積書_$stamp.pdf")
        }, REQUEST_PDF)
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PDF && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                QuotePdfRenderer.write(this, uri, pendingCustomer, pendingPerson, pendingPdfLines)
                Toast.makeText(this, "PDFを保存しました", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "PDF保存に失敗しました: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun invalidatePrice() {
        latestInput = null
        latestResult = null
        if (::resultCard.isInitialized) resultCard.visibility = View.GONE
    }

    private fun textEdit(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 16f
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(TEXT)
        setHintTextColor(TEXT_SUB)
        background = fieldBackground()
    }

    private fun numEdit(hintText: String, decimal: Boolean = false) = EditText(this).apply {
        hint = hintText
        textSize = 17f
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(TEXT)
        setHintTextColor(TEXT_SUB)
        background = fieldBackground()
        inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
    }

    private fun actionButton(textValue: String, primary: Boolean) = TextView(this).apply {
        text = textValue
        textSize = 17f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (primary) Color.WHITE else GREEN_DARK)
        background = roundedBg(
            if (primary) GREEN_DARK else Color.WHITE,
            GREEN_DARK,
            if (primary) 0 else 1,
            15f
        )
    }

    private fun sectionLabel(t: String) = label(t, 15, true)
    private fun label(t: String, size: Int, bold: Boolean, color: Int = TEXT) = TextView(this).apply {
        text = t
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun fieldBackground() = roundedBg(Color.WHITE, BORDER, 1, 12f)
    private fun roundedBg(fill: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidth: Int = 0, radius: Float = 12f) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        if (strokeWidth > 0) setStroke(dp(strokeWidth), strokeColor)
    }
    private fun lp(mt: Int = 0, mb: Int = 0, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, if (h > 0) dp(h) else h
    ).apply { topMargin = dp(mt); bottomMargin = dp(mb) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun nf(v: Number) = NumberFormat.getNumberInstance(Locale.JAPAN).format(v)

    companion object {
        const val REQUEST_PDF = 2201
        val BG = Color.rgb(247, 243, 234)
        val TEXT = Color.rgb(31, 37, 33)
        val TEXT_SUB = Color.rgb(103, 109, 105)
        val BORDER = Color.rgb(207, 211, 206)
        val GREEN_DARK = Color.rgb(40, 101, 73)
    }
}
