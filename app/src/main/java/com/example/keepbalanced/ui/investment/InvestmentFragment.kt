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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.NumberFormat
import java.util.Locale

class InvestmentFragment : Fragment() {

    private lateinit var viewModel: InvestmentViewModel
    private lateinit var tvTotalInvested: TextView
    private lateinit var lineChart: LineChart
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_investment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(InvestmentViewModel::class.java)

        tvTotalInvested = view.findViewById(R.id.tv_total_invested)
        lineChart = view.findViewById(R.id.line_chart_investment)
        progressBar = view.findViewById(R.id.progress_bar_investment)

        setupLineChartStyle()
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

        // Configuración general
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false // No hace falta leyenda, solo hay una línea
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        // Márgenes para que no se corten los números
        lineChart.extraBottomOffset = 10f
        lineChart.extraLeftOffset = 10f

        // Eje X (Fechas)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false) // Quitar rejilla vertical
        xAxis.textColor = colorTexto
        xAxis.granularity = 1f // Mostrar solo valores enteros

        // Eje Y (Dinero) - Izquierda
        val axisLeft = lineChart.axisLeft
        axisLeft.textColor = colorTexto
        axisLeft.setDrawGridLines(true) // Rejilla horizontal sí, ayuda a ver niveles
        axisLeft.gridColor = Color.parseColor("#40808080") // Gris muy transparente
        axisLeft.axisMinimum = 0f // Empezar siempre en 0

        // Eje Y - Derecha (Desactivar)
        lineChart.axisRight.isEnabled = false
    }

    private fun setupObservers() {
        viewModel.totalInvertido.observe(viewLifecycleOwner) { total ->
            val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
            tvTotalInvested.text = format.format(total)
        }

        // Observamos los datos Y las etiquetas de fecha a la vez
        viewModel.chartData.observe(viewLifecycleOwner) { entries ->
            val labels = viewModel.dateLabels.value ?: emptyList()
            actualizarGrafico(entries, labels)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }

        viewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun actualizarGrafico(entries: List<Entry>, labels: List<String>) {
        if (entries.isEmpty()) {
            lineChart.clear()
            return
        }

        // Configurar el formateador de fechas en el Eje X
        // Usamos las etiquetas que preparamos en el ViewModel
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val dataSet = LineDataSet(entries, "Capital Invertido")

        // Estilo de la Línea
        val colorPrimario = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary)

        dataSet.color = colorPrimario
        dataSet.lineWidth = 3f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Línea curva suave

        // Estilo de los Puntos
        dataSet.setDrawCircles(false) // No dibujar círculos en cada punto (limpio)
        dataSet.setDrawValues(false) // No escribir el valor en cada punto (limpio)

        // Efecto de Relleno (Sombreado debajo de la línea)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = colorPrimario
        dataSet.fillAlpha = 50 // Transparencia

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // Animación
        lineChart.animateX(1500)
    }
}