@file:Suppress("SameParameterValue")

package com.example.keepbalanced.ui.investment

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
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

    // Vistas Generales
    private lateinit var tvTotalInvested: TextView
    private lateinit var progressBar: ProgressBar

    // Vistas Gráfico de Línea
    private lateinit var lineChart: LineChart
    private lateinit var chipGroupTime: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup

    // Vistas Gráficos Circulares (Objetivos)
    private lateinit var pieChartTarget: PieChart    // Fondo (Meta)
    private lateinit var pieChartPortfolio: PieChart // Frente (Real)

    // Datos temporales para filtrado
    private var currentData: InvestmentChartData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_investment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(InvestmentViewModel::class.java)

        // Enlazar Vistas
        tvTotalInvested = view.findViewById(R.id.tv_total_invested)
        progressBar = view.findViewById(R.id.progress_bar_investment)

        lineChart = view.findViewById(R.id.line_chart_investment)
        chipGroupTime = view.findViewById(R.id.chip_group_time)
        chipGroupCategories = view.findViewById(R.id.chip_group_filters)

        pieChartTarget = view.findViewById(R.id.pie_chart_target)
        pieChartPortfolio = view.findViewById(R.id.pie_chart_portfolio)

        // Configuración Inicial de Gráficos
        setupLineChartStyle()

        // Configurar los dos gráficos circulares (Meta y Realidad)
        setupPieChartStyle(pieChartTarget, "") // Fondo (sin texto)
        setupPieChartStyle(pieChartPortfolio, "Actual") // Frente (con texto)

        // Cargar los datos estáticos de la Meta (70/15/15)
        cargarDatosMeta()

        setupChipListeners()
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
    /**
     * Coge un color, lo oscurece por un factor y le aplica transparencia.
     * @param color El color base.
     * @param factor Oscuridad (0.0 a 1.0). Ej: 0.8 es un 20% más oscuro.
     * @param alpha Transparencia (0 a 255). Ej: 100 es semitransparente.
     */
    private fun darkenAndTransparentColor(color: Int, factor: Float = 0.7f, alpha: Int = 100): Int {
        val a = alpha // Alpha fijo
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r, g, b)
    }


    // =========================================================
    //              CONFIGURACIÓN GRÁFICO DE LÍNEA
    // =========================================================

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

        // Quitar márgenes laterales para que la línea llegue al borde
        xAxis.spaceMax = 0f
        xAxis.spaceMin = 0f

        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("MMM yy", Locale("es", "ES"))
            override fun getFormattedValue(value: Float): String {
                return dateFormat.format(Date(value.toLong()))
            }
        }
        xAxis.setLabelCount(5, true)

        val axisLeft = lineChart.axisLeft
        axisLeft.textColor = colorTexto
        axisLeft.setDrawGridLines(true)
        axisLeft.gridColor = "#20808080".toColorInt()
        axisLeft.axisMinimum = 0f
        axisLeft.spaceTop = 30f // Aire arriba para el marcador

        lineChart.axisRight.isEnabled = false
    }

    // =========================================================
    //              CONFIGURACIÓN GRÁFICOS CIRCULARES
    // =========================================================


    private fun setupPieChartStyle(chart: PieChart, centerText: String) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.isDrawHoleEnabled = true
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setTransparentCircleAlpha(0)

        // Ambos gráficos alineados a las 12:00
        chart.isRotationEnabled = false
        chart.rotationAngle = 270f

        if (centerText.isNotEmpty()) {
            // --- GRÁFICO REAL (ABAJO) ---
            chart.holeRadius = 50f
            chart.setDrawCenterText(true)
            chart.centerText = centerText
            chart.setCenterTextColor(colorTexto)
            chart.setCenterTextSize(12f)

            // Etiquetas fuera
            chart.setExtraOffsets(30f, 10f, 30f, 10f)
            chart.setDrawEntryLabels(true)

            // IMPORTANTE: Este sí recibe toques
            chart.setTouchEnabled(true)

        } else {
            // --- GRÁFICO META (ARRIBA) ---
            chart.holeRadius = 45f // Mismo radio para que encajen perfecto
            chart.setDrawCenterText(false)
            chart.setDrawEntryLabels(false)

            // IMPORTANTE: Desactivar toques para poder tocar el de abajo
            chart.setTouchEnabled(false)
        }

        chart.animateY(1000)
    }
    /**
     * Carga los datos fijos del objetivo (Target) en el gráfico de fondo.
     */

    private fun cargarDatosMeta() {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Definimos los colores base
        val baseTeal = "#009688".toColorInt()
        val basePink = "#E91E63".toColorInt()
        val baseAmber = "#FFC107".toColorInt()

        // 1. Fija (15%)
        entries.add(PieEntry(15f, ""))
        // Usamos la función para hacerlo OSCURO y SEMITRANSPARENTE
        colors.add(darkenAndTransparentColor(baseTeal))

        // 2. Variable (70%)
        entries.add(PieEntry(70f, ""))
        colors.add(darkenAndTransparentColor(basePink))

        // 3. Oro (15%)
        entries.add(PieEntry(15f, ""))
        colors.add(darkenAndTransparentColor(baseAmber))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false)
        dataSet.sliceSpace = 0f
        dataSet.selectionShift = 0f // Importante: que no salte al tocar

        val data = PieData(dataSet)
        pieChartTarget.data = data
        pieChartTarget.invalidate()
    }
    // =========================================================
    //              LISTENERS Y OBSERVERS
    // =========================================================

    private fun setupChipListeners() {
        // Listener para filtrar el gráfico de línea por CATEGORÍA
        val catListener = View.OnClickListener {
            if (currentData != null) actualizarGraficoLineal(currentData!!)
        }
        view?.findViewById<Chip>(R.id.chip_total)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_fija)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_variable)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_oro)?.setOnClickListener(catListener)

        // Listener para filtrar el gráfico de línea por TIEMPO
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

        // Observador para el gráfico de LÍNEA
        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            currentData = data
            // Configurar el marcador con la lógica de posición corregida
            val marker = InvestmentMarkerView(requireContext(), R.layout.custom_marker_view)
            marker.chartView = lineChart
            lineChart.marker = marker

            actualizarGraficoLineal(data)
        }

        // Observador para el gráfico CIRCULAR (Portfolio vs Target)
        viewModel.portfolioDistribution.observe(viewLifecycleOwner) { dist ->
            actualizarGraficoCircular(dist)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun actualizarGraficoLineal(data: InvestmentChartData) {
        val dataSets = ArrayList<ILineDataSet>()

        val chipTotal = view?.findViewById<Chip>(R.id.chip_total)
        val chipFija = view?.findViewById<Chip>(R.id.chip_fija)
        val chipVariable = view?.findViewById<Chip>(R.id.chip_variable)
        val chipOro = view?.findViewById<Chip>(R.id.chip_oro)

        if (chipTotal?.isChecked == true && data.entriesTotal.isNotEmpty()) {
            dataSets.add(crearLineDataSet(data.entriesTotal, "General", "#2196F3".toColorInt(), true))
        }
        if (chipFija?.isChecked == true && data.entriesRentaFija.isNotEmpty()) {
            dataSets.add(crearLineDataSet(data.entriesRentaFija, "Renta Fija", "#009688".toColorInt(), false))
        }
        if (chipVariable?.isChecked == true && data.entriesRentaVariable.isNotEmpty()) {
            dataSets.add(crearLineDataSet(data.entriesRentaVariable, "Renta Variable", "#E91E63".toColorInt(), false))
        }
        if (chipOro?.isChecked == true && data.entriesOro.isNotEmpty()) {
            dataSets.add(crearLineDataSet(data.entriesOro, "Oro", "#FFC107".toColorInt(), false))
        }

        if (dataSets.isEmpty()) {
            lineChart.clear()
            return
        }

        lineChart.highlightValues(null) // Limpiar selección para evitar crash

        val lineData = LineData(dataSets)
        lineChart.data = lineData

        // Ajustar límite derecho del Eje X al último dato real
        val xMax = lineData.xMax
        val xMin = lineData.xMin
        lineChart.xAxis.axisMaximum = xMax
        lineChart.xAxis.axisMinimum = xMin

        lineChart.animateX(500)
    }

    private fun crearLineDataSet(entries: List<Entry>, label: String, color: Int, isFilled: Boolean): LineDataSet {
        val set = LineDataSet(entries, label)
        set.color = color
        set.setCircleColor(color)
        set.lineWidth = 2.5f
        set.circleRadius = 3f

        // Usar Bezier Horizontal para evitar bucles raros
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER

        set.setDrawHighlightIndicators(false)
        set.setDrawValues(false)
        set.setDrawCircles(false) // Puntos ocultos (se ven con el marcador)

        if (isFilled) {
            set.setDrawFilled(true)
            set.fillColor = color
            set.fillAlpha = 30
        }
        return set
    }

    private fun actualizarGraficoCircular(dist: PortfolioDistribution) {
        if (dist.totalGeneral == 0.0) return

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        val pctFija = (dist.totalFija / dist.totalGeneral) * 100
        val pctVariable = (dist.totalVariable / dist.totalGeneral) * 100
        val pctOro = (dist.totalOro / dist.totalGeneral) * 100

        // Añadimos en el MISMO ORDEN que la Meta: Fija -> Variable -> Oro

        // 1. Fija (Teal)
        entries.add(PieEntry(dist.totalFija.toFloat(), "Fija\n%.1f%%".format(pctFija)))
        colors.add("#009688".toColorInt())

        // 2. Variable (Pink)
        entries.add(PieEntry(dist.totalVariable.toFloat(), "Variable\n%.1f%%".format(pctVariable)))
        colors.add("#E91E63".toColorInt())

        // 3. Oro (Amber)
        entries.add(PieEntry(dist.totalOro.toFloat(), "Oro\n%.1f%%".format(pctOro)))
        colors.add("#FFC107".toColorInt())

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors

        // Etiquetas fuera
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
            override fun getFormattedValue(value: Float): String {
                return "" // Ocultamos valor numérico aquí, ya va en el label
            }
        })

        pieChartPortfolio.data = data
        pieChartPortfolio.setEntryLabelColor(colorTexto)
        pieChartPortfolio.setEntryLabelTextSize(11f)

        pieChartPortfolio.invalidate()
    }
}