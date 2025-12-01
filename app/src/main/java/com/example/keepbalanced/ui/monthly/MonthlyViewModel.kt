package com.example.keepbalanced.ui.monthly

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyChartData(
    val incomeEntries: List<BarEntry>,
    val expenseEntries: List<BarEntry>,
    val daysInMonth: Int
)

@Suppress("DEPRECATION")
class MonthlyViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    // Datos Gráfico Barras
    private val _chartData = MutableLiveData<MonthlyChartData>()
    val chartData: LiveData<MonthlyChartData> = _chartData

    // Datos Gráfico Circular (Gastos del mes)
    private val _monthlyExpensesMap = MutableLiveData<Map<String, Double>>()
    val monthlyExpensesMap: LiveData<Map<String, Double>> = _monthlyExpensesMap

    // Totales Numéricos
    private val _monthlyIncome = MutableLiveData(0.0)
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    private val _monthlyExpense = MutableLiveData(0.0)
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    private val _currentMonthText = MutableLiveData<String>()
    val currentMonthText: LiveData<String> = _currentMonthText

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var selectedDate = Calendar.getInstance()

    init {
        if (userId.isNotEmpty()) {
            loadDataForSelectedMonth()
        }
    }

    fun previousMonth() {
        selectedDate.add(Calendar.MONTH, -1)
        loadDataForSelectedMonth()
    }

    fun nextMonth() {
        selectedDate.add(Calendar.MONTH, 1)
        loadDataForSelectedMonth()
    }

    private fun loadDataForSelectedMonth() {
        _isLoading.value = true

        val fmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val titulo = fmt.format(selectedDate.time)
        _currentMonthText.value = titulo.replaceFirstChar { it.uppercase() }

        val monthIndex = selectedDate.get(Calendar.MONTH) + 1
        val year = selectedDate.get(Calendar.YEAR)
        val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)

        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("anio", year)
            .whereEqualTo("mes", monthIndex)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                _isLoading.value = false
                if (snapshots != null) {
                    val transactions = snapshots.toObjects(Transaction::class.java)

                    processBarChartData(transactions, daysInMonth)
                    processTotalsAndPie(transactions)
                }
            }
    }

    private fun processBarChartData(transactions: List<Transaction>, daysInMonth: Int) {
        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()

        val grouped = transactions.groupBy {
            val cal = Calendar.getInstance()
            cal.time = it.fecha!!
            cal.get(Calendar.DAY_OF_MONTH)
        }

        for (day in 1..daysInMonth) {
            var dailyIncome = 0.0
            var dailyExpense = 0.0

            val dayTransactions = grouped[day] ?: emptyList()
            for (t in dayTransactions) {
                if (t.tipo == "ingreso") dailyIncome += t.monto
                else if (t.tipo == "gasto") dailyExpense += t.monto
            }

            incomeEntries.add(BarEntry(day.toFloat(), dailyIncome.toFloat()))
            expenseEntries.add(BarEntry(day.toFloat(), dailyExpense.toFloat()))
        }

        _chartData.value = MonthlyChartData(incomeEntries, expenseEntries, daysInMonth)
    }

    private fun processTotalsAndPie(transactions: List<Transaction>) {
        var totalIng = 0.0
        var totalGast = 0.0
        val gastosMap = mutableMapOf<String, Double>()

        for (t in transactions) {
            if (t.tipo == "ingreso") {
                totalIng += t.monto
            } else if (t.tipo == "gasto") {
                totalGast += t.monto
                // Agrupar para el gráfico circular
                val cat = t.categoria.ifEmpty { "Otros" }
                gastosMap[cat] = gastosMap.getOrDefault(cat, 0.0) + t.monto
            }
        }

        _monthlyIncome.value = totalIng
        _monthlyExpense.value = totalGast
        _monthlyExpensesMap.value = gastosMap
    }
}