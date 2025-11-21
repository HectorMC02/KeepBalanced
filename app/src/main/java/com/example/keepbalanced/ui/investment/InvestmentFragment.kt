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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.NumberFormat
import java.util.Locale

@Suppress("DEPRECATION")
class InvestmentFragment : Fragment() {

    private lateinit var viewModel: InvestmentViewModel
    private lateinit var tvTotalInvested: TextView
    private lateinit var lineChart: LineChart
    private lateinit var progressBar: ProgressBar
    private lateinit var chipGroup: ChipGroup

    // Guardamos los datos en memoria para poder filtrar rápido
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
        chipGroup = view.findViewById(R.id.chip_group_filters)

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
        lineChart.legend.isEnabled = false // Usamos nuestros Chips personalizados
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Márgenes para que no se corten los marcadores
        lineChart.extraTopOffset = 20f

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f

        val axisLeft = lineChart.axisLeft
        axisLeft.textColor = colorTexto
        axisLeft.setDrawGridLines(true)
        axisLeft.gridColor = "#20808080".toColorInt() // Rejilla muy sutil
        axisLeft.axisMinimum = 0f

        lineChart.axisRight.isEnabled = false
    }

    private fun setupChipListeners() {
        // Cada vez que se pulse un chip, refrescamos el gráfico
        val listener = View.OnClickListener {
            if (currentData != null) actualizarGrafico(currentData!!)
        }

        view?.findViewById<Chip>(R.id.chip_total)?.setOnClickListener(listener)
        view?.findViewById<Chip>(R.id.chip_fija)?.setOnClickListener(listener)
        view?.findViewById<Chip>(R.id.chip_variable)?.setOnClickListener(listener)
        view?.findViewById<Chip>(R.id.chip_oro)?.setOnClickListener(listener)
    }

    private fun setupObservers() {
        viewModel.totalInvertido.observe(viewLifecycleOwner) { total ->
            val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            tvTotalInvested.text = format.format(total)
        }

        viewModel.chartData.observe(viewLifecycleOwner) { data ->
            currentData = data
            // Configurar el formateador de fecha (solo una vez)
            lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(data.dateLabels)

            // Configurar el marcador flotante (MarkerView)
            val marker = CustomMarkerView(requireContext(), R.layout.custom_marker_view, data.dateLabels)
            marker.chartView = lineChart
            lineChart.marker = marker

            actualizarGrafico(data)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        viewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun actualizarGrafico(data: InvestmentChartData) {
        val dataSets = ArrayList<ILineDataSet>()

        // Comprobar qué chips están activos y añadir sus líneas
        val chipTotal = view?.findViewById<Chip>(R.id.chip_total)
        val chipFija = view?.findViewById<Chip>(R.id.chip_fija)
        val chipVariable = view?.findViewById<Chip>(R.id.chip_variable)
        val chipOro = view?.findViewById<Chip>(R.id.chip_oro)

        // 1. GENERAL (Azul/Primario)
        if (chipTotal?.isChecked == true && data.entriesTotal.isNotEmpty()) {
            val set = crearLineDataSet(data.entriesTotal, "General", "#2196F3".toColorInt(), true)
            dataSets.add(set)
        }

        // 2. RENTA FIJA (Verde Azulado)
        if (chipFija?.isChecked == true && data.entriesRentaFija.isNotEmpty()) {
            val set = crearLineDataSet(data.entriesRentaFija, "Renta Fija", "#009688".toColorInt(), false)
            dataSets.add(set)
        }

        // 3. RENTA VARIABLE (Rosa/Rojo)
        if (chipVariable?.isChecked == true && data.entriesRentaVariable.isNotEmpty()) {
            val set = crearLineDataSet(data.entriesRentaVariable, "Renta Variable", "#E91E63".toColorInt(), false)
            dataSets.add(set)
        }

        // 4. ORO (Amarillo/Dorado)
        if (chipOro?.isChecked == true && data.entriesOro.isNotEmpty()) {
            val set = crearLineDataSet(data.entriesOro, "Oro", "#FFC107".toColorInt(), false)
            dataSets.add(set)
        }

        if (dataSets.isEmpty()) {
            lineChart.clear()
            return
        }

        lineChart.highlightValues(null)

        val lineData = LineData(dataSets)
        lineChart.data = lineData

        // Animación suave al cambiar filtros
        lineChart.animateX(500)
    }

    private fun crearLineDataSet(entries: List<Entry>, label: String, color: Int, isFilled: Boolean): LineDataSet {
        val set = LineDataSet(entries, label)

        set.color = color
        set.setCircleColor(color)
        set.lineWidth = 2.5f
        set.circleRadius = 3f
        set.mode = LineDataSet.Mode.CUBIC_BEZIER // Curva suave

        // --- LO QUE PEDISTE: SIN CRUCETA FEA ---
        set.setDrawHighlightIndicators(false)

        set.setDrawValues(false) // Sin números sobre la línea (usamos el marcador)
        set.setDrawCircles(false) // Ocultar puntos para que sea una línea limpia (se ven al tocar)

        // Si es la general, le ponemos relleno para que destaque más
        if (isFilled) {
            set.setDrawFilled(true)
            set.fillColor = color
            set.fillAlpha = 30
        }

        return set
    }
}