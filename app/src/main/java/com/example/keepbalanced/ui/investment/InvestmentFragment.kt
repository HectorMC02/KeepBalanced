package com.example.keepbalanced.ui.investment

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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
    private lateinit var lineChart: LineChart
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroupTime: ChipGroup
    private lateinit var chipGroupCategories: ChipGroup

    private var currentData: InvestmentChartData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_investment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(InvestmentViewModel::class.java)

        tvTotalInvested = view.findViewById(R.id.tv_total_invested)
        lineChart = view.findViewById(R.id.line_chart_investment)
        progressBar = view.findViewById(R.id.progress_bar_investment)
        chipGroupTime = view.findViewById(R.id.chip_group_time)
        chipGroupCategories = view.findViewById(R.id.chip_group_filters)

        setupLineChartStyle()
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

    private fun setupLineChartStyle() {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Esto daba margen al CONTENEDOR, pero no al Eje Y interno
        lineChart.extraTopOffset = 20f

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f

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

        // --- ¡¡ESTA ES LA SOLUCIÓN!! ---
        // Le decimos que añada un 30% de espacio extra por encima del valor máximo.
        // Así el punto más alto nunca tocará el borde y el marcador cabrá siempre.
        axisLeft.spaceTop = 30f
        // -------------------------------

        lineChart.axisRight.isEnabled = false
    }

    private fun setupChipListeners() {
        // Listener para el gráfico (categorías)
        val catListener = View.OnClickListener {
            if (currentData != null) actualizarGrafico(currentData!!)
        }
        view?.findViewById<Chip>(R.id.chip_total)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_fija)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_variable)?.setOnClickListener(catListener)
        view?.findViewById<Chip>(R.id.chip_oro)?.setOnClickListener(catListener)

        // Listener para el TIEMPO
        chipGroupTime.setOnCheckedChangeListener { _, checkedId ->
            val range = when (checkedId) {
                R.id.chip_1m -> TimeRange.ONE_MONTH
                R.id.chip_6m -> TimeRange.SIX_MONTHS
                R.id.chip_1y -> TimeRange.ONE_YEAR
                else -> TimeRange.ALL // chip_all o null
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

            // Configurar marcador flotante (ahora necesita convertir la fecha internamente)
            // Nota: Para simplificar, el marcador leerá el timestamp X y lo formateará él mismo
            val marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view)
            marker.chartView = lineChart
            lineChart.marker = marker

            actualizarGrafico(data)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun actualizarGrafico(data: InvestmentChartData) {
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

        lineChart.highlightValues(null) // Prevenir crash

        val lineData = LineData(dataSets)
        lineChart.data = lineData
        lineChart.animateX(500)
    }

    private fun crearLineDataSet(entries: List<Entry>, label: String, color: Int, isFilled: Boolean): LineDataSet {
        val set = LineDataSet(entries, label)
        set.color = color
        set.setCircleColor(color)
        set.lineWidth = 2.5f
        set.circleRadius = 3f
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.cubicIntensity = 0.1f
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
}