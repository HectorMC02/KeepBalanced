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

// --- AQUÍ ESTABA EL ERROR: Faltaban weekTitle y xLabels ---
data class MonthlyChartData(
    val incomeEntries: List<BarEntry>,
    val expenseEntries: List<BarEntry>,
    val weekTitle: String,      // Nuevo campo
    val xLabels: List<String>   // Nuevo campo
)

@Suppress("DEPRECATION")
class MonthlyViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    private val _chartData = MutableLiveData<MonthlyChartData>()
    val chartData: LiveData<MonthlyChartData> = _chartData

    private val _monthlyExpensesMap = MutableLiveData<Map<String, List<Transaction>>>()
    val monthlyExpensesMap: LiveData<Map<String, List<Transaction>>> = _monthlyExpensesMap

    private val _monthlyIncome = MutableLiveData<Double>(0.0)
    val monthlyIncome: LiveData<Double> = _monthlyIncome

    private val _monthlyExpense = MutableLiveData<Double>(0.0)
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    private val _currentMonthText = MutableLiveData<String>()
    val currentMonthText: LiveData<String> = _currentMonthText

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var selectedDate = Calendar.getInstance()

    // Control de Semana (Empieza en 0)
    private var currentWeekIndex = 0
    private var currentMonthTransactions: List<Transaction> = emptyList()

    init {
        if (userId.isNotEmpty()) {
            val today = Calendar.getInstance()
            // Intentamos empezar en la semana actual
            currentWeekIndex = today.get(Calendar.WEEK_OF_MONTH) - 1
            if (currentWeekIndex < 0) currentWeekIndex = 0
            loadDataForSelectedMonth()
        }
    }

    fun previousMonth() {
        selectedDate.add(Calendar.MONTH, -1)
        currentWeekIndex = 0
        loadDataForSelectedMonth()
    }

    fun nextMonth() {
        selectedDate.add(Calendar.MONTH, 1)
        currentWeekIndex = 0
        loadDataForSelectedMonth()
    }

    fun nextWeek() {
        val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        var dayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (dayOfWeekOffset < 0) dayOfWeekOffset = 6

        val nextWeekIndexPosible = currentWeekIndex + 1
        val startGridIndexNextWeek = nextWeekIndexPosible * 7
        val firstDayOfNextWeek = startGridIndexNextWeek - dayOfWeekOffset + 1

        if (firstDayOfNextWeek <= daysInMonth) {
            currentWeekIndex++
            recalculateWeekData()
        }
    }

    fun previousWeek() {
        if (currentWeekIndex > 0) {
            currentWeekIndex--
            recalculateWeekData()
        }
    }

    private fun loadDataForSelectedMonth() {
        _isLoading.value = true

        val fmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val titulo = fmt.format(selectedDate.time)
        _currentMonthText.value = titulo.replaceFirstChar { it.uppercase() }

        val monthIndex = selectedDate.get(Calendar.MONTH) + 1
        val year = selectedDate.get(Calendar.YEAR)

        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("anio", year)
            .whereEqualTo("mes", monthIndex)
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                _isLoading.value = false
                if (snapshots != null) {
                    currentMonthTransactions = snapshots.toObjects(Transaction::class.java)
                    processTotalsAndPie(currentMonthTransactions)
                    validarSemanaActual()
                    recalculateWeekData()
                }
            }
    }

    private fun validarSemanaActual() {
        val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        var dayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (dayOfWeekOffset < 0) dayOfWeekOffset = 6

        val startGridIndex = currentWeekIndex * 7
        val firstDayOfWeek = startGridIndex - dayOfWeekOffset + 1

        if (firstDayOfWeek > daysInMonth) {
            currentWeekIndex = 0
        }
    }

    private fun recalculateWeekData() {
        val incomeEntries = ArrayList<BarEntry>()
        val expenseEntries = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()

        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        var dayOfWeekOffset = cal.get(Calendar.DAY_OF_WEEK) - 2
        if (dayOfWeekOffset < 0) dayOfWeekOffset = 6

        val startGridIndex = currentWeekIndex * 7
        val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)

        var firstRealDayOfWeek = -1
        var lastRealDayOfWeek = -1

        for (i in 0..6) {
            val gridIndex = startGridIndex + i
            val dayOfMonth = gridIndex - dayOfWeekOffset + 1

            if (dayOfMonth in 1..daysInMonth) {
                if (firstRealDayOfWeek == -1) firstRealDayOfWeek = dayOfMonth
                lastRealDayOfWeek = dayOfMonth

                val dayTransactions = currentMonthTransactions.filter {
                    val tCal = Calendar.getInstance()
                    tCal.time = it.fecha!!
                    tCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth
                }

                var dailyIncome = 0.0
                var dailyExpense = 0.0

                val incomeList = ArrayList<Transaction>()
                val expenseList = ArrayList<Transaction>()

                for (t in dayTransactions) {
                    if (t.tipo == "ingreso") {
                        dailyIncome += t.monto
                        incomeList.add(t)
                    } else if (t.tipo == "gasto") {
                        dailyExpense += t.monto
                        expenseList.add(t)
                    }
                }

                // Aquí pasamos los 4 parámetros que espera la clase
                incomeEntries.add(BarEntry(i.toFloat(), dailyIncome.toFloat(), incomeList))
                expenseEntries.add(BarEntry(i.toFloat(), dailyExpense.toFloat(), expenseList))

                xLabels.add(dayOfMonth.toString())
            } else {
                incomeEntries.add(BarEntry(i.toFloat(), 0f))
                expenseEntries.add(BarEntry(i.toFloat(), 0f))
                xLabels.add("-")
            }
        }

        val tituloSemana = if (firstRealDayOfWeek != -1) {
            "Semana ${currentWeekIndex + 1} ($firstRealDayOfWeek - $lastRealDayOfWeek)"
        } else {
            "Semana ${currentWeekIndex + 1}"
        }

        // Construimos el objeto con los 4 campos
        _chartData.value = MonthlyChartData(incomeEntries, expenseEntries, tituloSemana, xLabels)
    }

    private fun processTotalsAndPie(transactions: List<Transaction>) {
        var totalIng = 0.0
        var totalGast = 0.0
        val gastosMap = mutableMapOf<String, ArrayList<Transaction>>()

        for (t in transactions) {
            if (t.tipo == "ingreso") {
                totalIng += t.monto
            } else if (t.tipo == "gasto") {
                totalGast += t.monto
                val cat = t.categoria.ifEmpty { "Otros" }
                if (!gastosMap.containsKey(cat)) {
                    gastosMap[cat] = ArrayList()
                }
                gastosMap[cat]?.add(t)
            }
        }

        _monthlyIncome.value = totalIng
        _monthlyExpense.value = totalGast
        _monthlyExpensesMap.value = gastosMap
    }
}