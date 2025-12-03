package com.example.keepbalanced.ui.monthly

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.Locale

@Suppress("DEPRECATION")
class MonthlyFragment : Fragment() {

    private lateinit var viewModel: MonthlyViewModel

    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    private lateinit var tvCurrentWeek: TextView
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var progressBar: ProgressBar
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvMonthlyExpense: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_monthly, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MonthlyViewModel::class.java)

        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        btnPrev = view.findViewById(R.id.btn_prev_month)
        btnNext = view.findViewById(R.id.btn_next_month)

        tvCurrentWeek = view.findViewById(R.id.tv_current_week)
        btnPrevWeek = view.findViewById(R.id.btn_prev_week)
        btnNextWeek = view.findViewById(R.id.btn_next_week)

        barChart = view.findViewById(R.id.bar_chart_monthly)
        pieChart = view.findViewById(R.id.pie_chart_monthly_breakdown)
        progressBar = view.findViewById(R.id.progress_bar_monthly)

        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)

        setupBarChartStyle()
        setupPieChartStyle()
        setupListeners()
        setupObservers()
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(attr, typedValue, true)
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(requireContext(), typedValue.resourceId)
        }
        return typedValue.data
    }

    private fun setupBarChartStyle() {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        barChart.description.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setScaleEnabled(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawGridBackground(false)
        barChart.legend.textColor = colorTexto

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f
        xAxis.labelCount = 7
        xAxis.setCenterAxisLabels(true)

        val leftAxis = barChart.axisLeft
        leftAxis.textColor = colorTexto
        leftAxis.axisMinimum = 0f

        barChart.axisRight.isEnabled = false

        // Marcador para el gráfico de BARRAS (usa el que ya tenías)
        val marker = MonthlyMarkerView(requireContext(), R.layout.custom_marker_monthly)
        marker.chartView = barChart
        barChart.marker = marker
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPieChartStyle() {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 45f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)

        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Gastos"
        pieChart.setCenterTextSize(14f)
        pieChart.setCenterTextColor(colorTexto)

        pieChart.setExtraOffsets(40f, 10f, 40f, 10f)
        pieChart.setDrawEntryLabels(false)

        // --- AQUÍ ESTÁ LA MAGIA DEL TOUCH ---
        pieChart.isRotationEnabled = false // 1. Bloqueamos rotación

        // 2. Activamos el marcador circular NUEVO
        // Reutilizamos el layout 'custom_marker_monthly' porque sirve igual (caja con texto)
        val marker = MonthlyPieMarkerView(requireContext(), R.layout.custom_marker_monthly)
        marker.chartView = pieChart
        pieChart.marker = marker

        // 3. Listener TÁCTIL para el efecto "Arrastrar dedo"
        pieChart.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            // Si el usuario toca o arrastra (MOVE)
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                // Preguntamos al gráfico: "¿Qué ángulo es este punto X,Y?"
                val angle = pieChart.getAngleForPoint(event.x, event.y)

                // "¿Qué índice del dataset corresponde a este ángulo?"
                val index = pieChart.getIndexForAngle(angle)

                // Resaltamos ese índice manualmente
                if (index >= 0) {
                    pieChart.highlightValue(index.toFloat(), 0, false) // false = no llamar listener de select
                }
            }
            // Dejamos que la librería siga procesando (para dibujar el marcador)
            false
        }

        pieChart.animateY(1000)
    }

    private fun setupListeners() {
        btnPrev.setOnClickListener { viewModel.previousMonth() }
        btnNext.setOnClickListener { viewModel.nextMonth() }
        btnPrevWeek.setOnClickListener { viewModel.previousWeek() }
        btnNextWeek.setOnClickListener { viewModel.nextWeek() }
    }

    private fun setupObservers() {
        viewModel.currentMonthText.observe(viewLifecycleOwner) { tvCurrentMonth.text = it }
        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.isVisible = it }

        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { tvMonthlyIncome.text = format.format(it) }
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { tvMonthlyExpense.text = format.format(it) }

        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            updateBarChart(data)
            tvCurrentWeek.text = data.weekTitle
        }

        viewModel.monthlyExpensesMap.observe(viewLifecycleOwner) { mapa ->
            updatePieChart(mapa)
        }
    }

    private fun updateBarChart(data: MonthlyChartData) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(data.xLabels)

        val setIngresos = BarDataSet(data.incomeEntries, "Ingresos")
        setIngresos.color = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        setIngresos.valueTextColor = colorTexto
        setIngresos.valueTextSize = 9f

        val setGastos = BarDataSet(data.expenseEntries, "Gastos")
        setGastos.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        setGastos.valueTextColor = colorTexto
        setGastos.valueTextSize = 9f

        val groupSpace = 0.08f
        val barSpace = 0.03f
        val barWidth = 0.43f

        val barData = BarData(setIngresos, setGastos)
        barData.barWidth = barWidth
        barData.setValueFormatter(object : ValueFormatter() {
            @SuppressLint("DefaultLocale")
            override fun getFormattedValue(value: Float): String = if (value > 0) String.format("%.0f", value) else ""
        })

        barChart.data = barData
        barChart.xAxis.axisMinimum = 0f
        barChart.xAxis.axisMaximum = 7f
        barChart.groupBars(0f, groupSpace, barSpace)
        barChart.invalidate()
        barChart.animateY(500)
    }

    private fun updatePieChart(mapa: Map<String, List<Transaction>>) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        val entradas = ArrayList<PieEntry>()

        for ((categoria, lista) in mapa) {
            val montoTotal = lista.sumOf { it.monto }
            if (montoTotal > 0) {
                // --- CAMBIO: Pasamos la LISTA en el 3er parámetro (Data) ---
                entradas.add(PieEntry(montoTotal.toFloat(), categoria, lista))
            }
        }

        if (entradas.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "Sin Gastos"
            return
        } else {
            pieChart.centerText = "Gastos"
        }

        val dataSet = PieDataSet(entradas, "")
        dataSet.colors = getColoresVariados()

        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.5f
        dataSet.valueLineWidth = 1.5f
        dataSet.valueLineColor = colorTexto
        dataSet.valueTextColor = colorTexto
        dataSet.valueTextSize = 12f
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            @SuppressLint("DefaultLocale")
            override fun getFormattedValue(value: Float): String {
                if (value < 2f) return ""
                return String.format("%.1f %%", value)
            }
        })

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelColor(colorTexto)
        pieChart.invalidate()
    }

    private fun getColoresVariados(): List<Int> {
        val colores = ArrayList<Int>()
        colores.addAll(ColorTemplate.MATERIAL_COLORS.toList())
        colores.addAll(ColorTemplate.VORDIPLOM_COLORS.toList())
        colores.addAll(ColorTemplate.JOYFUL_COLORS.toList())
        colores.addAll(ColorTemplate.COLORFUL_COLORS.toList())
        colores.addAll(ColorTemplate.LIBERTY_COLORS.toList())
        colores.addAll(ColorTemplate.PASTEL_COLORS.toList())
        colores.add("#FF5722".toColorInt())
        colores.add("#607D8B".toColorInt())
        colores.add("#E91E63".toColorInt())
        colores.add("#9C27B0".toColorInt())
        colores.add("#3F51B5".toColorInt())
        colores.add("#009688".toColorInt())
        colores.add("#795548".toColorInt())
        colores.add("#000000".toColorInt())
        return colores
    }
}