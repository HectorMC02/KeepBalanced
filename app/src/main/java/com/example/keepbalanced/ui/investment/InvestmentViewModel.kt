package com.example.keepbalanced.ui.investment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
                    // Ordenamos por fecha para procesar cronológicamente
                    allTransactions = rawList.sortedBy { it.fecha }
                    filterDataByRange(TimeRange.ALL)
                }
            }
    }

    fun filterDataByRange(range: TimeRange) {
        // Si no hay transacciones, enviamos listas vacías y salimos
        if (allTransactions.isEmpty()) {
            _chartData.value = InvestmentChartData(emptyList(), emptyList(), emptyList(), emptyList())
            return
        }

        // 1. Definir el rango de fechas (Inicio -> Fin)
        val cal = Calendar.getInstance()
        // "Fin" es hoy (reseteando hora para que sea medianoche)
        val endDate = truncateTime(cal.time)

        // "Inicio" depende del filtro
        val startDate: Long = when (range) {
            TimeRange.ONE_MONTH -> { cal.add(Calendar.MONTH, -1); truncateTime(cal.time) }
            TimeRange.SIX_MONTHS -> { cal.add(Calendar.MONTH, -6); truncateTime(cal.time) }
            TimeRange.ONE_YEAR -> { cal.add(Calendar.YEAR, -1); truncateTime(cal.time) }
            TimeRange.ALL -> {
                // Si es "all", empezamos en la fecha de la PRIMERA transacción
                val primeraFecha = allTransactions.firstOrNull()?.fecha
                if (primeraFecha != null) truncateTime(primeraFecha) else endDate
            }
        }

        // 2. Calcular los SALDOS INICIALES (Acumulado antes de la fecha de inicio)
        // Esto es vital para que si pides "1 Mes", la gráfica empiece ya con el dinero que tenías acumulado.
        var sumTotal = 0.0
        var sumFija = 0.0
        var sumVariable = 0.0
        var sumOro = 0.0

        // Sumamos all lo ocurrido ANTES del día de inicio
        val transaccionesPrevias = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) < startDate }
        for (t in transaccionesPrevias) {
            val sub = t.subcategoria ?: ""
            sumTotal += t.monto
            if (sub.contains("Fija", true)) sumFija += t.monto
            if (sub.contains("Variable", true)) sumVariable += t.monto
            if (sub.contains("Oro", true)) sumOro += t.monto
        }

        // 3. Preparar agrupación por días para búsqueda rápida
        // Filtramos solo las transacciones dentro del rango (>= startDate)
        val transaccionesEnRango = allTransactions.filter { it.fecha != null && truncateTime(it.fecha) >= startDate }
        val groupedByDay = transaccionesEnRango.groupBy { truncateTime(it.fecha!!) }

        // 4. EL BUCLE MAESTRO: Generar un punto por CADA DÍA
        val entriesTotal = ArrayList<Entry>()
        val entriesFija = ArrayList<Entry>()
        val entriesVariable = ArrayList<Entry>()
        val entriesOro = ArrayList<Entry>()

        // Ponemos el calendario en el día de inicio
        cal.timeInMillis = startDate

        // Mientras no pasemos de hoy...
        while (cal.timeInMillis <= endDate) {
            val currentDayTimestamp = cal.timeInMillis

            // ¿Hubo movimientos este día?
            val movimientosHoy = groupedByDay[currentDayTimestamp]

            if (movimientosHoy != null) {
                // Si hubo movimientos, actualizamos los acumuladores
                for (t in movimientosHoy) {
                    val sub = t.subcategoria ?: ""
                    sumTotal += t.monto
                    if (sub.contains("Fija", true)) sumFija += t.monto
                    else if (sub.contains("Variable", true)) sumVariable += t.monto
                    else if (sub.contains("Oro", true)) sumOro += t.monto
                }
            }

            // --- AQUÍ ESTÁ LA CLAVE ---
            // Generamos un punto para ESTE día con el valor actual (haya cambiado o no)
            val xValue = currentDayTimestamp.toFloat()

            entriesTotal.add(Entry(xValue, sumTotal.toFloat()))

            // Para las específicas, solo pintamos si hay dinero invertido (para que no salga una línea en 0 molestando)
            // O si prefieres ver la línea en 0, quita el 'if (sum > 0)'
            if (sumFija > 0) entriesFija.add(Entry(xValue, sumFija.toFloat()))
            if (sumVariable > 0) entriesVariable.add(Entry(xValue, sumVariable.toFloat()))
            if (sumOro > 0) entriesOro.add(Entry(xValue, sumOro.toFloat()))

            // Avanzamos al día siguiente
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Actualizamos el total global (siempre es el último valor calculado)
        _totalInvertido.value = sumTotal

        val data = InvestmentChartData(entriesTotal, entriesFija, entriesVariable, entriesOro)
        _chartData.value = data
    }

    /**
     * Trunca una fecha a las 00:00:00 para poder comparar días
     */
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