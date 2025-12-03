package com.example.keepbalanced.ui.investment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.PortfolioDistribution
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

    private val _chartData = MutableLiveData<InvestmentChartData>()
    val chartData: LiveData<InvestmentChartData> = _chartData

    private val _totalInvertido = MutableLiveData<Double>(0.0)
    val totalInvertido: LiveData<Double> = _totalInvertido

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
                    filterDataByRange(TimeRange.ALL)
                }
            }
    }

    fun filterDataByRange(range: TimeRange) {
        if (allTransactions.isEmpty()) {
            _chartData.value = InvestmentChartData(emptyList(), emptyList(), emptyList(), emptyList())
            _portfolioDistribution.value = PortfolioDistribution(0.0,0.0,0.0,0.0)
            return
        }

        val cal = Calendar.getInstance()
        // Fecha Fin: Hoy
        val endDate = truncateTime(cal.time)

        // --- LÓGICA DE FECHAS CAMBIADA A MESES NATURALES ---
        val startDate: Long = when (range) {
            TimeRange.ONE_MONTH -> {
                // Desde el día 1 del mes ACTUAL
                cal.set(Calendar.DAY_OF_MONTH, 1)
                truncateTime(cal.time)
            }
            TimeRange.SIX_MONTHS -> {
                // Desde el día 1 de hace 5 meses (Total 6 meses inc. el actual)
                cal.add(Calendar.MONTH, -5)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                truncateTime(cal.time)
            }
            TimeRange.ONE_YEAR -> {
                // Desde el día 1 de hace 11 meses (Total 1 año inc. el actual)
                cal.add(Calendar.MONTH, -11)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                truncateTime(cal.time)
            }
            TimeRange.ALL -> {
                val primeraFecha = allTransactions.firstOrNull()?.fecha
                if (primeraFecha != null) truncateTime(primeraFecha) else endDate
            }
        }

        // Variables acumuladores
        var sumTotal = 0.0
        var sumFija = 0.0
        var sumVariable = 0.0
        var sumOro = 0.0

        // 1. ACUMULADO PREVIO (Lo que tenías ANTES de este periodo)
        // Esto sirve para que la línea empiece en la altura correcta,
        // pero NO se cuenta para el gráfico circular (que solo quiere el flujo del periodo).
        val transaccionesPrevias = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) < startDate }
        for (t in transaccionesPrevias) {
            val sub = t.subcategoria ?: ""
            sumTotal += t.monto
            if (sub.contains("Fija", true)) sumFija += t.monto
            if (sub.contains("Variable", true)) sumVariable += t.monto
            if (sub.contains("Oro", true)) sumOro += t.monto
        }

        // 2. TRANSACCIONES DEL PERIODO (Lo que se mostrará en el circular y moverá la línea)
        val transaccionesEnRango = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) >= startDate }

        // Preparamos datos para gráfico de línea (agrupado por días)
        val groupedByDay = transaccionesEnRango.groupBy { truncateTime(it.fecha!!) }

        val entriesTotal = ArrayList<Entry>()
        val entriesFija = ArrayList<Entry>()
        val entriesVariable = ArrayList<Entry>()
        val entriesOro = ArrayList<Entry>()

        // 3. BUCLE DIARIO PARA LA LÍNEA
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
        _chartData.value = InvestmentChartData(entriesTotal, entriesFija, entriesVariable, entriesOro)

        // =================================================================
        // 4. CÁLCULO DEL GRÁFICO CIRCULAR (SOLO FLUJO DEL PERIODO)
        // =================================================================
        // Aquí sumamos SOLAMENTE las transacciones que han ocurrido dentro del rango de fechas.
        // Ignoramos el 'sumFija' acumulado arriba porque ese incluye el pasado.

        var pieFija = 0.0
        var pieVariable = 0.0
        var pieOro = 0.0

        for (t in transaccionesEnRango) {
            val sub = t.subcategoria ?: ""
            if (sub.contains("Fija", true)) pieFija += t.monto
            if (sub.contains("Variable", true)) pieVariable += t.monto
            if (sub.contains("Oro", true)) pieOro += t.monto
        }

        val pieTotal = pieFija + pieVariable + pieOro

        _portfolioDistribution.value = PortfolioDistribution(
            totalFija = pieFija,
            totalVariable = pieVariable,
            totalOro = pieOro,
            totalGeneral = pieTotal
        )
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