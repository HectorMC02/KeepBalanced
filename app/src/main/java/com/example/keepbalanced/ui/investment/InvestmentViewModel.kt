package com.example.keepbalanced.ui.investment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.PortfolioDistribution // Asegúrate de tener esta clase creada o defínela abajo
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.data.Entry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.Date

data class InvestmentChartData(
    val entriesTotal: List<Entry>,
    val entriesRentaFija: List<Entry>,
    val entriesRentaVariable: List<Entry>,
    val entriesOro: List<Entry>
)

enum class TimeRange {
    ONE_MONTH, SIX_MONTHS, ONE_YEAR, ALL
}

class InvestmentViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    // --- LIVE DATA PARA GRÁFICO DE LÍNEA ---
    private val _chartData = MutableLiveData<InvestmentChartData>()
    val chartData: LiveData<InvestmentChartData> = _chartData

    private val _totalInvertido = MutableLiveData<Double>(0.0)
    val totalInvertido: LiveData<Double> = _totalInvertido

    // --- LIVE DATA PARA GRÁFICO CIRCULAR (DISTRIBUCIÓN) ---
    // Esta es la variable que te faltaba
    private val _portfolioDistribution = MutableLiveData<PortfolioDistribution>()
    val portfolioDistribution: LiveData<PortfolioDistribution> = _portfolioDistribution

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var allTransactions: List<Transaction> = emptyList()

    init {
        if (userId.isNotEmpty()) {
            cargarHistorialInversiones()
        }
    }

    fun cargarHistorialInversiones() {
        _isLoading.value = true
        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("tipo", "gasto")
            .whereEqualTo("categoria", "Inversión")
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _errorMessage.value = "Error: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val rawList = snapshots.toObjects(Transaction::class.java)
                    allTransactions = rawList.sortedBy { it.fecha }

                    // 1. Calcular gráfico de línea (con filtros de tiempo)
                    filterDataByRange(TimeRange.ALL)

                    // 2. Calcular gráfico circular (siempre histórico completo)
                    calculateCurrentDistribution()
                }
            }
    }

    /**
     * Calcula los totales históricos para el gráfico de tarta (Meta vs Realidad).
     */
    private fun calculateCurrentDistribution() {
        var sumFija = 0.0
        var sumVariable = 0.0
        var sumOro = 0.0

        for (t in allTransactions) {
            val sub = t.subcategoria ?: ""
            if (sub.contains("Fija", true)) sumFija += t.monto
            if (sub.contains("Variable", true)) sumVariable += t.monto
            if (sub.contains("Oro", true)) sumOro += t.monto
        }

        val total = sumFija + sumVariable + sumOro

        _portfolioDistribution.value = PortfolioDistribution(
            totalFija = sumFija,
            totalVariable = sumVariable,
            totalOro = sumOro,
            totalGeneral = total
        )
    }

    /**
     * Lógica del gráfico de línea (muestreo diario).
     */
    fun filterDataByRange(range: TimeRange) {
        if (allTransactions.isEmpty()) {
            // Si no hay datos, enviamos vacío para limpiar gráfica
            _chartData.value = InvestmentChartData(emptyList(), emptyList(), emptyList(), emptyList())
            return
        }

        val cal = Calendar.getInstance()
        val endDate = truncateTime(cal.time)

        val startDate: Long = when (range) {
            TimeRange.ONE_MONTH -> { cal.add(Calendar.MONTH, -1); truncateTime(cal.time) }
            TimeRange.SIX_MONTHS -> { cal.add(Calendar.MONTH, -6); truncateTime(cal.time) }
            TimeRange.ONE_YEAR -> { cal.add(Calendar.YEAR, -1); truncateTime(cal.time) }
            TimeRange.ALL -> {
                val primeraFecha = allTransactions.firstOrNull()?.fecha
                if (primeraFecha != null) truncateTime(primeraFecha) else endDate
            }
        }

        var sumTotal = 0.0
        var sumFija = 0.0
        var sumVariable = 0.0
        var sumOro = 0.0

        // 1. Acumulado Previo
        val transaccionesPrevias = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) < startDate }
        for (t in transaccionesPrevias) {
            val sub = t.subcategoria ?: ""
            sumTotal += t.monto
            if (sub.contains("Fija", true)) sumFija += t.monto
            if (sub.contains("Variable", true)) sumVariable += t.monto
            if (sub.contains("Oro", true)) sumOro += t.monto
        }

        // 2. Agrupar por día las visibles
        val transaccionesEnRango = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) >= startDate }
        val groupedByDay = transaccionesEnRango.groupBy { truncateTime(it.fecha!!) }

        val entriesTotal = ArrayList<Entry>()
        val entriesFija = ArrayList<Entry>()
        val entriesVariable = ArrayList<Entry>()
        val entriesOro = ArrayList<Entry>()

        // 3. Bucle diario
        cal.timeInMillis = startDate
        while (cal.timeInMillis <= endDate) {
            val currentDayTimestamp = cal.timeInMillis
            val movimientosHoy = groupedByDay[currentDayTimestamp]

            if (movimientosHoy != null) {
                for (t in movimientosHoy) {
                    val sub = t.subcategoria ?: ""
                    sumTotal += t.monto
                    if (sub.contains("Fija", true)) sumFija += t.monto
                    else if (sub.contains("Variable", true)) sumVariable += t.monto
                    else if (sub.contains("Oro", true)) sumOro += t.monto
                }
            }

            val xValue = currentDayTimestamp.toFloat()

            entriesTotal.add(Entry(xValue, sumTotal.toFloat()))
            if (sumFija > 0) entriesFija.add(Entry(xValue, sumFija.toFloat()))
            if (sumVariable > 0) entriesVariable.add(Entry(xValue, sumVariable.toFloat()))
            if (sumOro > 0) entriesOro.add(Entry(xValue, sumOro.toFloat()))

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        _totalInvertido.value = sumTotal

        val data = InvestmentChartData(entriesTotal, entriesFija, entriesVariable, entriesOro)
        _chartData.value = data
    }

    private fun truncateTime(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}