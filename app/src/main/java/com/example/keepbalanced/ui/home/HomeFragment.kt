package com.example.keepbalanced.ui.home

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keepbalanced.R
import com.example.keepbalanced.ui.adapter.TransactionsAdapter
import com.example.keepbalanced.ui.dialog.AddTransactionDialog
import com.example.keepbalanced.ui.dialog.HistoryFilterDialog // Importar Diálogo Filtro
import com.example.keepbalanced.ui.dialog.SubcategoryDetailsDialog
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    // Contenedores
    private lateinit var layoutDashboard: View
    private lateinit var layoutFullHistory: View

    // Vistas Dashboard
    private lateinit var tvBalanceTotal: TextView
    private lateinit var tvIngresosTotal: TextView
    private lateinit var tvGastosTotal: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvVerMas: TextView
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var pieChartGastos: PieChart
    private lateinit var pieChartIngresos: PieChart
    private lateinit var adapterRecent: TransactionsAdapter

    // Vistas Historial Completo
    private lateinit var btnCloseHistory: ImageButton
    private lateinit var btnFilterHistory: ImageButton // Nuevo Botón
    private lateinit var rvFullHistory: RecyclerView
    private lateinit var btnLoadMore: Button
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var adapterFullHistory: TransactionsAdapter

    private lateinit var progressBar: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Enlazar
        layoutDashboard = view.findViewById(R.id.layout_dashboard)
        layoutFullHistory = view.findViewById(R.id.layout_full_history)
        progressBar = view.findViewById(R.id.progress_bar_home)

        tvBalanceTotal = view.findViewById(R.id.tv_balance_total)
        tvIngresosTotal = view.findViewById(R.id.tv_ingresos_total)
        tvGastosTotal = view.findViewById(R.id.tv_gastos_total)
        fabAdd = view.findViewById(R.id.fab_add_transaction)
        tvVerMas = view.findViewById(R.id.tv_ver_mas)
        rvRecentTransactions = view.findViewById(R.id.rv_recent_transactions)
        pieChartGastos = view.findViewById(R.id.pie_chart_gastos)
        pieChartIngresos = view.findViewById(R.id.pie_chart_ingresos)

        btnCloseHistory = view.findViewById(R.id.btn_close_history)
        btnFilterHistory = view.findViewById(R.id.btn_filter_history) // Nuevo Botón
        rvFullHistory = view.findViewById(R.id.rv_full_history)
        btnLoadMore = view.findViewById(R.id.btn_load_more)
        chipGroupFilter = view.findViewById(R.id.chip_group_history_filter)

        // Recyclers
        adapterRecent = TransactionsAdapter()
        rvRecentTransactions.layoutManager = LinearLayoutManager(context)
        rvRecentTransactions.isNestedScrollingEnabled = false
        rvRecentTransactions.adapter = adapterRecent

        adapterFullHistory = TransactionsAdapter()
        rvFullHistory.layoutManager = LinearLayoutManager(context)
        rvFullHistory.isNestedScrollingEnabled = false
        rvFullHistory.adapter = adapterFullHistory

        // Gráficos
        setupPieChartStyle(pieChartGastos, "Gastos")
        setupPieChartStyle(pieChartIngresos, "Ingresos")
        setupChartListener(pieChartGastos, "gasto")
        setupChartListener(pieChartIngresos, "ingreso")

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            val dialog = AddTransactionDialog()
            dialog.show(parentFragmentManager, AddTransactionDialog.TAG)
        }

        // ABRIR HISTORIAL
        tvVerMas.setOnClickListener {
            layoutDashboard.isVisible = false
            layoutFullHistory.isVisible = true
            fabAdd.isVisible = false
            // Cargar primera página si está vacío o refrescar
            homeViewModel.cargarHistorial(reset = true, newFilter = null)
        }

        // CERRAR HISTORIAL
        btnCloseHistory.setOnClickListener {
            layoutFullHistory.isVisible = false
            layoutDashboard.isVisible = true
            fabAdd.isVisible = true
        }

        // BOTÓN FILTRO AVANZADO
        btnFilterHistory.setOnClickListener {
            val current = homeViewModel.getCurrentFilter()
            val dialog = HistoryFilterDialog(current) { newFilter ->
                // Al aplicar filtro, recargamos
                homeViewModel.cargarHistorial(reset = true, newFilter = newFilter)
            }
            dialog.show(parentFragmentManager, HistoryFilterDialog.TAG)
        }

        // CARGAR MÁS
        btnLoadMore.setOnClickListener {
            homeViewModel.cargarHistorial(reset = false, newFilter = null)
        }

        // CHIPS FILTRO RÁPIDO
        chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            val type = when(checkedId) {
                R.id.chip_expense -> "gasto"
                R.id.chip_income -> "ingreso"
                else -> null
            }
            val current = homeViewModel.getCurrentFilter()
            val newFilter = current.copy(type = type) // Solo cambiamos el tipo, mantenemos fechas/cat
            homeViewModel.cargarHistorial(reset = true, newFilter = newFilter)
        }
    }

    private fun setupObservers() {
        // Dashboard Observers
        homeViewModel.balanceGlobal.observe(viewLifecycleOwner) { balance ->
            tvBalanceTotal.text = formatCurrency(balance)
            val colorRes = if (balance >= 0) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            tvBalanceTotal.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        }
        homeViewModel.ingresosMes.observe(viewLifecycleOwner) { tvIngresosTotal.text = formatCurrency(it) }
        homeViewModel.gastosMes.observe(viewLifecycleOwner) { tvGastosTotal.text = formatCurrency(it) }

        homeViewModel.transaccionesMes.observe(viewLifecycleOwner) { transacciones ->
            adapterRecent.updateList(transacciones.take(3))
        }

        homeViewModel.gastosPorCategoria.observe(viewLifecycleOwner) { mapa ->
            actualizarGrafico(pieChartGastos, mapa, "Sin Gastos", "Gastos")
        }
        homeViewModel.ingresosPorCategoria.observe(viewLifecycleOwner) { mapa ->
            actualizarGrafico(pieChartIngresos, mapa, "Sin Ingresos", "Ingresos")
        }

        // Historial Observer
        homeViewModel.fullHistoryList.observe(viewLifecycleOwner) { fullList ->
            adapterFullHistory.updateList(fullList)
        }

        homeViewModel.isEndOfList.observe(viewLifecycleOwner) { isEnd ->
            // Si es el final (true), ocultamos el botón. Si no, lo mostramos.
            btnLoadMore.isVisible = !isEnd
        }
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            // Desactivar botón mientras carga para evitar doble click
            if (!btnLoadMore.isVisible) {
                // Si ya estaba oculto, sigue oculto
            } else {
                btnLoadMore.isEnabled = !isLoading
            }
        }
        homeViewModel.errorMessage.observe(viewLifecycleOwner) { if (it != null) Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // ... (Funciones gráficas: getThemeColor, setupPieChartStyle, etc. IGUAL QUE ANTES) ...

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(attr, typedValue, true)
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(requireContext(), typedValue.resourceId)
        }
        return typedValue.data
    }

    private fun setupPieChartStyle(chart: PieChart, centerText: String) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.isDrawHoleEnabled = true
        chart.holeRadius = 45f
        chart.setHoleColor(Color.TRANSPARENT)
        chart.setTransparentCircleAlpha(0)
        chart.setDrawCenterText(true)
        chart.centerText = centerText
        chart.setCenterTextSize(16f)
        chart.setCenterTextColor(colorTexto)
        chart.setExtraOffsets(40f, 10f, 40f, 10f)
        chart.setDrawEntryLabels(true)
        chart.setEntryLabelColor(colorTexto)
        chart.setEntryLabelTextSize(11f)
        chart.animateY(1000)
    }

    private fun setupChartListener(chart: PieChart, tipoTransaccion: String) {
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e == null) return
                val pieEntry = e as PieEntry
                val categoria = pieEntry.label
                val desglose = homeViewModel.obtenerDesglosePorSubcategoria(categoria, tipoTransaccion)
                if (desglose.isNotEmpty()) {
                    val dialog = SubcategoryDetailsDialog(categoria, desglose, tipoTransaccion)
                    dialog.show(parentFragmentManager, SubcategoryDetailsDialog.TAG)
                }
            }
            override fun onNothingSelected() {}
        })
    }

    private fun actualizarGrafico(chart: PieChart, mapa: Map<String, Double>, emptyText: String, centerText: String) {
        val colorTexto = getThemeColor(android.R.attr.textColorPrimary)
        val entradas = ArrayList<PieEntry>()
        for ((categoria, monto) in mapa) {
            if (monto > 0) entradas.add(PieEntry(monto.toFloat(), categoria))
        }
        if (entradas.isEmpty()) {
            chart.clear()
            chart.centerText = emptyText
            chart.setCenterTextColor(colorTexto)
            return
        } else {
            chart.centerText = centerText
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
                if (value < 1f) return ""
                return String.format("%.1f %%", value)
            }
        })
        chart.data = data
        chart.setUsePercentValues(true)
        chart.setEntryLabelColor(colorTexto)
        chart.invalidate()
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

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        return format.format(amount)
    }
}