package com.example.keepbalanced.ui.monthly

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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
    private lateinit var barChart: BarChart
    private lateinit var progressBar: ProgressBar

    // Vistas Nuevas
    private lateinit var tvMonthlyIncome: TextView
    private lateinit var tvMonthlyExpense: TextView
    private lateinit var pieChart: PieChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_monthly, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MonthlyViewModel::class.java)

        tvCurrentMonth = view.findViewById(R.id.tv_current_month)
        btnPrev = view.findViewById(R.id.btn_prev_month)
        btnNext = view.findViewById(R.id.btn_next_month)
        barChart = view.findViewById(R.id.bar_chart_monthly)
        progressBar = view.findViewById(R.id.progress_bar_monthly)

        tvMonthlyIncome = view.findViewById(R.id.tv_monthly_income)
        tvMonthlyExpense = view.findViewById(R.id.tv_monthly_expense)
        pieChart = view.findViewById(R.id.pie_chart_monthly_breakdown)

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

    // --- ESTILOS DE GRÁFICOS ---

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
        xAxis.labelCount = 6
        xAxis.setCenterAxisLabels(true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = value.toInt().toString()
        }

        val leftAxis = barChart.axisLeft
        leftAxis.textColor = colorTexto
        leftAxis.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
    }

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
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(colorTexto)
        pieChart.setEntryLabelTextSize(11f)
        pieChart.animateY(1000)
    }

    private fun setupListeners() {
        btnPrev.setOnClickListener { viewModel.previousMonth() }
        btnNext.setOnClickListener { viewModel.nextMonth() }
    }

    private fun setupObservers() {
        viewModel.currentMonthText.observe(viewLifecycleOwner) { tvCurrentMonth.text = it }
        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.isVisible = it }

        // Datos Gráfico Barras
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            updateBarChart(data)
        }

        // Datos Textos
        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { tvMonthlyIncome.text = format.format(it) }
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { tvMonthlyExpense.text = format.format(it) }

        // Datos Gráfico Circular
        viewModel.monthlyExpensesMap.observe(viewLifecycleOwner) { mapa ->
            updatePieChart(mapa)
        }
    }

    private fun updateBarChart(data: MonthlyChartData) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
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
        barChart.xAxis.axisMinimum = 1f
        barChart.xAxis.axisMaximum = data.daysInMonth.toFloat() + 1f
        barChart.groupBars(1f, groupSpace, barSpace)
        barChart.invalidate()
        barChart.animateY(1000)
    }

    private fun updatePieChart(mapa: Map<String, Double>) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        val entradas = ArrayList<PieEntry>()

        for ((categoria, monto) in mapa) {
            if (monto > 0) entradas.add(PieEntry(monto.toFloat(), categoria))
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