package com.example.keepbalanced.ui.investment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.example.keepbalanced.model.PortfolioDistribution
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class InvestmentFragment : Fragment() {

    private lateinit var viewModel: InvestmentViewModel

    private lateinit var tvTotalInvested: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var lineChart: LineChart
    private lateinit var chipGroupTime: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup

    private lateinit var pieChartTarget: PieChart
    private lateinit var pieChartPortfolio: PieChart

    private var currentData: InvestmentChartData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_investment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(InvestmentViewModel::class.java)

        tvTotalInvested = view.findViewById(R.id.tv_total_invested)
        progressBar = view.findViewById(R.id.progress_bar_investment)

        lineChart = view.findViewById(R.id.line_chart_investment)
        chipGroupTime = view.findViewById(R.id.chip_group_time)
        chipGroupCategories = view.findViewById(R.id.chip_group_filters)

        pieChartTarget = view.findViewById(R.id.pie_chart_target)
        pieChartPortfolio = view.findViewById(R.id.pie_chart_portfolio)

        setupLineChartStyle()

        // 1. Configurar Gráfico REAL (Fondo, grande)
        setupPieChartStyle(pieChartPortfolio, "Actual")

        // 2. Configurar Gráfico META (Frente, pequeño)
        setupPieChartStyle(pieChartTarget, "")

        cargarDatosMeta()

        setupChipListeners()
        setupObservers()
    }

    // ... getThemeColor ...
    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(attr, typedValue, true)
        if (typedValue.resourceId != 0) return ContextCompat.getColor(requireContext(), typedValue.resourceId)
        return typedValue.data
    }

    // ... setupLineChartStyle (MANTENER IGUAL QUE ANTES) ...
    private fun setupLineChartStyle() {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.extraTopOffset = 20f
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f
        xAxis.spaceMax = 0f
        xAxis.spaceMin = 0f
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MMM yy", Locale("es", "ES"))
            override fun getFormattedValue(value: Float): String = dateFormat.format(Date(value.toLong()))
        }
        xAxis.setLabelCount(5, true)
        val axisLeft = lineChart.axisLeft
        axisLeft.textColor = colorTexto
        axisLeft.setDrawGridLines(true)
        axisLeft.gridColor = "#20808080".toColorInt()
        axisLeft.axisMinimum = 0f
        axisLeft.spaceTop = 30f
        lineChart.axisRight.isEnabled = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPieChartStyle(chart: PieChart, centerText: String) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.isDrawHoleEnabled = true
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setTransparentCircleAlpha(0)

        chart.isRotationEnabled = false
        chart.rotationAngle = 270f

        if (centerText.isNotEmpty()) {
            // --- GRÁFICO REAL (PORTFOLIO) ---
            chart.holeRadius = 50f
            chart.setDrawCenterText(true)
            chart.centerText = centerText
            chart.setCenterTextColor(colorTexto)
            chart.setCenterTextSize(12f)

            chart.setExtraOffsets(30f, 10f, 30f, 10f)
            chart.setDrawEntryLabels(false)
            chart.setTouchEnabled(true)

            val marker = InvestmentPieMarkerView(requireContext(), R.layout.custom_marker_monthly)
            marker.chartView = chart
            chart.marker = marker

            chart.setOnTouchListener { view, event ->
                view.parent.requestDisallowInterceptTouchEvent(true)
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    val angle = chart.getAngleForPoint(event.x, event.y)
                    val index = chart.getIndexForAngle(angle)
                    if (index >= 0) chart.highlightValue(index.toFloat(), 0, false)
                }
                false
            }
        } else {
            // --- GRÁFICO META (TARGET) ---
            chart.holeRadius = 45f
            chart.setDrawCenterText(false)
            chart.setDrawEntryLabels(false)
            chart.setTouchEnabled(false)
        }

        chart.animateY(1000)
    }

    private fun cargarDatosMeta() {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val context = requireContext()

        // 1. Fija (15%)
        entries.add(PieEntry(15f, ""))
        colors.add(darkenAndTransparentColor(ContextCompat.getColor(context, R.color.inv_fija)))

        // 2. Variable (70%)
        entries.add(PieEntry(70f, ""))
        colors.add(darkenAndTransparentColor(ContextCompat.getColor(context, R.color.inv_variable)))

        // 3. Oro (15%)
        entries.add(PieEntry(15f, ""))
        colors.add(darkenAndTransparentColor(ContextCompat.getColor(context, R.color.inv_oro)))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false)
        dataSet.sliceSpace = 0f
        dataSet.selectionShift = 0f
        val data = PieData(dataSet)
        pieChartTarget.data = data
        pieChartTarget.invalidate()
    }

    private fun actualizarGraficoCircular(dist: PortfolioDistribution) {
        if (dist.totalGeneral == 0.0) {
            pieChartPortfolio.clear()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        val context = requireContext()


        // Añadimos en el orden: Fija -> Variable -> Oro
        entries.add(PieEntry(dist.totalFija.toFloat(), "Fija"))
        colors.add(ContextCompat.getColor(context, R.color.inv_fija))

        entries.add(PieEntry(dist.totalVariable.toFloat(), "Variable"))
        colors.add(ContextCompat.getColor(context, R.color.inv_variable))

        entries.add(PieEntry(dist.totalOro.toFloat(), "Oro"))
        colors.add(ContextCompat.getColor(context, R.color.inv_oro))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineWidth = 1f
        dataSet.valueLineColor = colorTexto
        dataSet.valueTextColor = colorTexto
        dataSet.valueTextSize = 11f
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)

        data.setValueFormatter(object : ValueFormatter() {
            @SuppressLint("DefaultLocale")
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1f%%", value)
            }
        })

        pieChartPortfolio.data = data
        pieChartPortfolio.setUsePercentValues(true) // Activar cálculo automático
        pieChartPortfolio.setEntryLabelColor(colorTexto)
        pieChartPortfolio.setEntryLabelTextSize(11f)

        pieChartPortfolio.invalidate()
    }

    // ... (Listeners, Observers y Line Chart Logic - IGUAL QUE ANTES) ...
    private fun setupChipListeners() {
        val catListener = View.OnClickListener {
            if (currentData != null) actualizarGraficoLineal(currentData!!)
        }
        view?.findViewById<Chip>(R.id.chip_total)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_fija)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_variable)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_oro)?.setOnClickListener(catListener)

        chipGroupTime.setOnCheckedChangeListener { _, checkedId ->
            val range = when (checkedId) {
                R.id.chip_1m -> TimeRange.ONE_MONTH
                R.id.chip_6m -> TimeRange.SIX_MONTHS
                R.id.chip_1y -> TimeRange.ONE_YEAR
                else -> TimeRange.ALL
            }
            viewModel.filterDataByRange(range)
        }
    }

    private fun setupObservers() {
        viewModel.totalInvertido.observe(viewLifecycleOwner) { total ->
            val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            tvTotalInvested.text = format.format(total)
        }
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            currentData = data
            val marker = InvestmentMarkerView(requireContext(), R.layout.custom_marker_view)
            marker.chartView = lineChart
            lineChart.marker = marker
            actualizarGraficoLineal(data)
        }
        viewModel.portfolioDistribution.observe(viewLifecycleOwner) { dist ->
            actualizarGraficoCircular(dist)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> progressBar.isVisible = isLoading }
        viewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun actualizarGraficoLineal(data: InvestmentChartData) {
        val dataSets = ArrayList<ILineDataSet>()
        val context = requireContext()
        val colorGeneral = ContextCompat.getColor(context, R.color.inv_general)
        val colorFija = ContextCompat.getColor(context, R.color.inv_fija)
        val colorVariable = ContextCompat.getColor(context, R.color.inv_variable)
        val colorOro = ContextCompat.getColor(context, R.color.inv_oro)
        val chipTotal = view?.findViewById<Chip>(R.id.chip_total)
        val chipFija = view?.findViewById<Chip>(R.id.chip_fija)
        val chipVariable = view?.findViewById<Chip>(R.id.chip_variable)
        val chipOro = view?.findViewById<Chip>(R.id.chip_oro)

        if (chipTotal?.isChecked == true && data.entriesTotal.isNotEmpty()) dataSets.add(crearLineDataSet(data.entriesTotal, "General", colorGeneral, true))
        if (chipFija?.isChecked == true && data.entriesRentaFija.isNotEmpty()) dataSets.add(crearLineDataSet(data.entriesRentaFija, "Renta Fija", colorFija, false))
        if (chipVariable?.isChecked == true && data.entriesRentaVariable.isNotEmpty()) dataSets.add(crearLineDataSet(data.entriesRentaVariable, "Renta Variable", colorVariable, false))
        if (chipOro?.isChecked == true && data.entriesOro.isNotEmpty()) dataSets.add(crearLineDataSet(data.entriesOro, "Oro", colorOro, false))

        if (dataSets.isEmpty()) {
            lineChart.clear()
            return
        }
        lineChart.highlightValues(null)
        val lineData = LineData(dataSets)
        lineChart.data = lineData
        lineChart.xAxis.axisMaximum = lineData.xMax
        lineChart.xAxis.axisMinimum = lineData.xMin
        lineChart.animateX(500)
    }

    private fun crearLineDataSet(entries: List<Entry>, label: String, color: Int, isFilled: Boolean): LineDataSet {
        val set = LineDataSet(entries, label)
        set.color = color
        set.setCircleColor(color)
        set.lineWidth = 2.5f
        set.circleRadius = 3f
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawHighlightIndicators(false)
        set.setDrawValues(false)
        set.setDrawCircles(false)
        if (isFilled) {
            set.setDrawFilled(true)
            set.fillColor = color
            set.fillAlpha = 30
        }
        return set
    }

    private fun darkenAndTransparentColor(color: Int, factor: Float = 0.7f, alpha: Int = 100): Int {
        val a = alpha
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r, g, b)
    }
}