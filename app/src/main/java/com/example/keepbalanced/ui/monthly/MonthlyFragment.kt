@file:Suppress("SameParameterValue")

package com.example.keepbalanced.ui.monthly

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import androidx.core.graphics.toColorInt

class MonthlyFragment : Fragment() {

    private lateinit var viewModel: MonthlyViewModel
    private lateinit var tvCurrentMonth: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var barChart: BarChart
    private lateinit var progressBar: ProgressBar

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

        setupBarChartStyle()
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

        // Leyenda
        barChart.legend.textColor = colorTexto
        barChart.legend.textSize = 12f

        // Eje X (Días del mes)
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f
        // Mostramos etiquetas cada 5 días para no saturar, pero se dibujan todos los días
        xAxis.labelCount = 6
        xAxis.setCenterAxisLabels(true) // IMPORTANTE para centrar el grupo de barras en el día
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Convertir 1.0 -> "1", 2.0 -> "2"
                return value.toInt().toString()
            }
        }

        // Eje Y (Dinero)
        val leftAxis = barChart.axisLeft
        leftAxis.textColor = colorTexto
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = "#20808080".toColorInt()
        leftAxis.axisMinimum = 0f // Empezar en 0

        barChart.axisRight.isEnabled = false
    }

    private fun setupListeners() {
        btnPrev.setOnClickListener { viewModel.previousMonth() }
        btnNext.setOnClickListener { viewModel.nextMonth() }
    }

    private fun setupObservers() {
        viewModel.currentMonthText.observe(viewLifecycleOwner) { tvCurrentMonth.text = it }

        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            updateBarChart(data)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.isVisible = it }
    }

    private fun updateBarChart(data: MonthlyChartData) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        // 1. Crear Datasets (Ingreso = Verde, Gasto = Rojo)
        val setIngresos = BarDataSet(data.incomeEntries, "Ingresos")
        setIngresos.color = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        setIngresos.valueTextColor = colorTexto
        setIngresos.valueTextSize = 9f

        val setGastos = BarDataSet(data.expenseEntries, "Gastos")
        setGastos.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        setGastos.valueTextColor = colorTexto
        setGastos.valueTextSize = 9f

        // 2. Configuración de Agrupación (Matemáticas para que encajen)
        // El ancho total de un grupo (ingreso + gasto + espacios) debe ser 1.0
        val groupSpace = 0.08f
        val barSpace = 0.03f
        val barWidth = 0.43f
        // (0.03 + 0.43) * 2 + 0.08 = 1.00 -> Correcto

        val barData = BarData(setIngresos, setGastos)
        barData.barWidth = barWidth

        // Ocultar valores si son 0 para limpiar el gráfico
        barData.setValueFormatter(object : ValueFormatter() {
            @SuppressLint("DefaultLocale")
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) String.format("%.0f", value) else ""
            }
        })

        barChart.data = barData

        // 3. Aplicar agrupación
        // Definimos el rango del Eje X desde el día 1 hasta el último día del mes
        barChart.xAxis.axisMinimum = 1f
        barChart.xAxis.axisMaximum = data.daysInMonth.toFloat() + 1f

        // Esta función agrupa las barras automáticamente
        barChart.groupBars(1f, groupSpace, barSpace)

        barChart.invalidate()
        barChart.animateY(1000)
    }
}