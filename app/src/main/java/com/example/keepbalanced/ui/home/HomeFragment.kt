package com.example.keepbalanced.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keepbalanced.R
import com.example.keepbalanced.ui.adapter.TransactionsAdapter
import com.example.keepbalanced.ui.dialog.AddTransactionDialog
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var tvBalanceTotal: TextView
    private lateinit var tvIngresosTotal: TextView
    private lateinit var tvGastosTotal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvVerMas: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var adapter: TransactionsAdapter
    private lateinit var pieChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        tvBalanceTotal = view.findViewById(R.id.tv_balance_total)
        tvIngresosTotal = view.findViewById(R.id.tv_ingresos_total)
        tvGastosTotal = view.findViewById(R.id.tv_gastos_total)
        progressBar = view.findViewById(R.id.progress_bar_home)
        fabAdd = view.findViewById(R.id.fab_add_transaction)
        tvVerMas = view.findViewById(R.id.tv_ver_mas)
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions)
        pieChart = view.findViewById(R.id.pie_chart_gastos)

        rvRecentTransactions.layoutManager = LinearLayoutManager(context)
        adapter = TransactionsAdapter()
        rvRecentTransactions.adapter = adapter

        setupPieChartStyle()
        setupObservers()
        setupListeners()
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        // 1. Resolvemos el atributo
        theme.resolveAttribute(attr, typedValue, true)

        // 2. Si el atributo apunta a un recurso (ej: @color/mi_color o un selector),
        // usamos ContextCompat para obtener el color real.
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(requireContext(), typedValue.resourceId)
        }

        // 3. Si es un valor directo (ej: #FFFFFF), devolvemos el dato tal cual.
        return typedValue.data
    }

    private fun setupPieChartStyle() {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)

        pieChart.description.isEnabled = false

        // --- CORRECCIÓN 1: Quitar la leyenda ---
        pieChart.legend.isEnabled = false

        // Estilo Donut
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 45f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)

        // Texto central
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Gastos"
        pieChart.setCenterTextSize(14f)
        pieChart.setCenterTextColor(colorTexto)

        // --- CORRECCIÓN 2: AUMENTAR MÁRGENES ---
        // Esto es vital. Si no damos espacio (offsets), la librería
        // forzará el texto hacia dentro porque no cabe fuera.
        // (Izquierda, Arriba, Derecha, Abajo)
        pieChart.setExtraOffsets(40f, 10f, 40f, 10f)

        // Desactivar etiquetas de entrada (nombres de categoría) DENTRO del gráfico
        // para que no se solapen. Con los colores es suficiente o se puede personalizar más.
        pieChart.setDrawEntryLabels(false)

        pieChart.animateY(1000)
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            val dialog = AddTransactionDialog()
            dialog.show(parentFragmentManager, AddTransactionDialog.TAG)
        }
        tvVerMas.setOnClickListener {
            Toast.makeText(context, "Ver historial completo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        homeViewModel.balance.observe(viewLifecycleOwner) { balance ->
            tvBalanceTotal.text = formatCurrency(balance)
            val colorRes = if (balance >= 0) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            tvBalanceTotal.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }

        homeViewModel.totalIngresos.observe(viewLifecycleOwner) { ingresos ->
            tvIngresosTotal.text = formatCurrency(ingresos)
        }

        homeViewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            tvGastosTotal.text = formatCurrency(gastos)
        }

        homeViewModel.transaccionesMes.observe(viewLifecycleOwner) { transacciones ->
            val ultimasTransacciones = transacciones.take(5)
            adapter.updateList(ultimasTransacciones)
        }

        homeViewModel.gastosPorCategoria.observe(viewLifecycleOwner) { mapaGastos ->
            actualizarGrafico(mapaGastos)
        }

        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
        }

        homeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }
    }

    private fun actualizarGrafico(mapaGastos: Map<String, Double>) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        val entradas = ArrayList<PieEntry>()

        for ((categoria, monto) in mapaGastos) {
            if (monto > 0) {
                entradas.add(PieEntry(monto.toFloat(), categoria))
            }
        }

        if (entradas.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "Sin Gastos"
            pieChart.setCenterTextColor(colorTexto)
            return
        } else {
            pieChart.centerText = "Gastos"
        }

        val dataSet = PieDataSet(entradas, "")
        dataSet.colors = getColoresVariados()

        // --- CORRECCIÓN 3: TEXTO FUERA Y LÍNEAS ---

        // Sacar los valores fuera del círculo
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        // Sacar las etiquetas (si las hubiera) fuera también
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        // Configuración de la línea conectora
        dataSet.valueLinePart1OffsetPercentage = 80f // Empieza casi al borde
        dataSet.valueLinePart1Length = 0.4f // Longitud primer tramo
        dataSet.valueLinePart2Length = 0.4f // Longitud segundo tramo
        dataSet.valueLineWidth = 1.5f // Grosor
        dataSet.valueLineColor = colorTexto // Color dinámico (blanco/negro)

        // Configuración del texto del valor
        dataSet.valueTextColor = colorTexto // Color dinámico
        dataSet.valueTextSize = 12f

        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 5f

        val data = PieData(dataSet)

        // Formateador con Porcentaje (%)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Ocultar si es muy pequeño para limpiar la vista
                if (value < 3f) return ""
                return String.format("%.1f %%", value)
            }
        })

        pieChart.data = data

        // ¡IMPORTANTE! Activar cálculo de porcentajes
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
        colores.add(Color.parseColor("#FF5722"))
        colores.add(Color.parseColor("#607D8B"))
        colores.add(Color.parseColor("#E91E63"))
        colores.add(Color.parseColor("#9C27B0"))
        colores.add(Color.parseColor("#3F51B5"))
        colores.add(Color.parseColor("#009688"))
        colores.add(Color.parseColor("#795548"))
        colores.add(Color.parseColor("#000000"))
        return colores
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        return format.format(amount)
    }
}