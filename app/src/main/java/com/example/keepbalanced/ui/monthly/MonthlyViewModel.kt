package com.example.keepbalanced.ui.monthly

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

    private val _chartData = MutableLiveData<MonthlyChartData>()
    val chartData: LiveData<MonthlyChartData> = _chartData

    private val _currentMonthText = MutableLiveData<String>()
    val currentMonthText: LiveData<String> = _currentMonthText

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Calendario interno para saber qué mes estamos viendo (empieza en Hoy)
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

        // 1. Formatear título (Ej: "Noviembre 2025")
        val fmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val titulo = fmt.format(selectedDate.time)
        _currentMonthText.value = titulo.replaceFirstChar { it.uppercase() }

        // 2. Filtros para Firebase
        val monthIndex = selectedDate.get(Calendar.MONTH) + 1 // Enero es 1 en tu BD
        val year = selectedDate.get(Calendar.YEAR)
        val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)

        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("anio", year)
            .whereEqualTo("mes", monthIndex)
            .addSnapshotListener { snapshots, _ ->
                _isLoading.value = false
                if (snapshots != null) {
                    val transactions = snapshots.toObjects(Transaction::class.java)
                    processDataForChart(transactions, daysInMonth)
                }
            }
    }

    private fun processDataForChart(transactions: List<Transaction>, daysInMonth: Int) {
        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()

        // Agrupamos las transacciones por el día del mes (1, 2, 3...)
        val grouped = transactions.groupBy {
            val cal = Calendar.getInstance()
            if (it.fecha != null) {
                cal.time = it.fecha
                cal.get(Calendar.DAY_OF_MONTH)
            } else {
                -1
            }
        }

        // Recorremos TODOS los días del mes para rellenar huecos con 0
        for (day in 1..daysInMonth) {
            var dailyIncome = 0.0
            var dailyExpense = 0.0

            val dayTransactions = grouped[day] ?: emptyList()

            for (t in dayTransactions) {
                if (t.tipo == "ingreso") dailyIncome += t.monto
                else if (t.tipo == "gasto") dailyExpense += t.monto
            }

            // Eje X = Día del mes
            incomeEntries.add(BarEntry(day.toFloat(), dailyIncome.toFloat()))
            expenseEntries.add(BarEntry(day.toFloat(), dailyExpense.toFloat()))
        }

        _chartData.value = MonthlyChartData(incomeEntries, expenseEntries, daysInMonth)
    }
}