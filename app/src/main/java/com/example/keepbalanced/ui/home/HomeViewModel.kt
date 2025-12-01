package com.example.keepbalanced.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.HistoryFilter
import com.example.keepbalanced.model.SubcategoryBreakdown
import com.example.keepbalanced.model.Transaction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    // --- DASHBOARD DATA ---
    // Balance GLOBAL (Histórico completo)
    private val _balanceGlobal = MutableLiveData(0.0)
    val balanceGlobal: LiveData<Double> = _balanceGlobal

    // Datos del MES ACTUAL
    private val _ingresosMes = MutableLiveData(0.0)
    val ingresosMes: LiveData<Double> = _ingresosMes
    private val _gastosMes = MutableLiveData(0.0)
    val gastosMes: LiveData<Double> = _gastosMes

    private val _transaccionesMes = MutableLiveData<List<Transaction>>()
    val transaccionesMes: LiveData<List<Transaction>> = _transaccionesMes
    private val _gastosPorCategoria = MutableLiveData<Map<String, Double>>()
    val gastosPorCategoria: LiveData<Map<String, Double>> = _gastosPorCategoria
    private val _ingresosPorCategoria = MutableLiveData<Map<String, Double>>()
    val ingresosPorCategoria: LiveData<Map<String, Double>> = _ingresosPorCategoria

    // --- HISTORIAL COMPLETO DATA ---
    private val _fullHistoryList = MutableLiveData<List<Transaction>>()
    val fullHistoryList: LiveData<List<Transaction>> = _fullHistoryList
    private val _isEndOfList = MutableLiveData<Boolean>()
    val isEndOfList: LiveData<Boolean> = _isEndOfList

    private var currentFilter = HistoryFilter()
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var currentLimit: Long = 10
    private val STEP_SIZE = 10L
    private val currentHistoryList = ArrayList<Transaction>()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        if (userId.isNotEmpty()) {
            cargarBalanceGlobal() // Nueva función
            cargarDatosMesActual()
        }
    }

    // 1. CALCULAR BALANCE HISTÓRICO (Sin filtros de fecha)
    private fun cargarBalanceGlobal() {
        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                if (snapshots != null) {
                    val todas = snapshots.toObjects(Transaction::class.java)
                    var total = 0.0
                    for (t in todas) {
                        if (t.tipo == "ingreso") total += t.monto
                        else if (t.tipo == "gasto") total -= t.monto
                    }
                    _balanceGlobal.value = total
                }
            }
    }

    // 2. CALCULAR DATOS DEL MES (Para gráficos y resumen mensual)
    fun cargarDatosMesActual() {
        _isLoading.value = true
        val calendario = Calendar.getInstance()
        val mesActual = calendario.get(Calendar.MONTH) + 1
        val anioActual = calendario.get(Calendar.YEAR)

        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("anio", anioActual)
            .whereEqualTo("mes", mesActual)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _errorMessage.value = "Error dashboard: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val lista = snapshots.toObjects(Transaction::class.java)
                    _transaccionesMes.value = lista

                    // Calculamos totales SOLO de este mes
                    calcularTotalesMes(lista)
                    agruparPorCategoria(lista, "gasto", _gastosPorCategoria)
                    agruparPorCategoria(lista, "ingreso", _ingresosPorCategoria)
                }
            }
    }

    // ... (cargarHistorial sigue IGUAL) ...
    fun getCurrentFilter() = currentFilter

    fun cargarHistorial(reset: Boolean, newFilter: HistoryFilter? = null) {
        _isLoading.value = true

        if (newFilter != null) {
            if (newFilter != currentFilter) {
                currentFilter = newFilter
                currentLimit = STEP_SIZE
                lastVisibleDocument = null
                currentHistoryList.clear()
                _fullHistoryList.value = emptyList()
                _isEndOfList.value = false
            }
        } else if (reset) {
            currentLimit = STEP_SIZE
            lastVisibleDocument = null
            currentHistoryList.clear()
            _fullHistoryList.value = emptyList()
            _isEndOfList.value = false
        } else {
            currentLimit += STEP_SIZE
        }

        var query = db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .orderBy("fecha", Query.Direction.DESCENDING)

        if (currentFilter.type != null) query = query.whereEqualTo("tipo", currentFilter.type)
        if (currentFilter.category != null) query = query.whereEqualTo("categoria", currentFilter.category)
        if (currentFilter.dateFrom != null) query = query.whereGreaterThanOrEqualTo("fecha", currentFilter.dateFrom!!)
        if (currentFilter.dateTo != null) query = query.whereLessThanOrEqualTo("fecha", currentFilter.dateTo!!)

        query = query.limit(currentLimit)

        query.get().addOnSuccessListener { snapshots ->
            _isLoading.value = false

            if (!snapshots.isEmpty) {
                lastVisibleDocument = snapshots.documents[snapshots.size() - 1]

                var lista = snapshots.toObjects(Transaction::class.java)

                if (currentFilter.minAmount != null) lista = lista.filter { it.monto >= currentFilter.minAmount!! }
                if (currentFilter.maxAmount != null) lista = lista.filter { it.monto <= currentFilter.maxAmount!! }

                _fullHistoryList.value = lista

                if (snapshots.size() < currentLimit) {
                    _isEndOfList.value = true
                } else {
                    _isEndOfList.value = false
                }

            } else {
                if (reset || newFilter != null) _fullHistoryList.value = emptyList()
                _isEndOfList.value = true
            }
        }.addOnFailureListener { e ->
            _isLoading.value = false
            _errorMessage.value = "Error historial: ${e.message}"
        }
    }

    // Auxiliares
    private fun calcularTotalesMes(transacciones: List<Transaction>) {
        var ingresos = 0.0
        var gastos = 0.0
        for (trans in transacciones) {
            if (trans.tipo == "ingreso") ingresos += trans.monto
            else if (trans.tipo == "gasto") gastos += trans.monto
        }
        _ingresosMes.value = ingresos
        _gastosMes.value = gastos
        // Nota: Ya no calculamos el balance aquí, viene de cargarBalanceGlobal()
    }

    private fun agruparPorCategoria(transacciones: List<Transaction>, tipo: String, liveData: MutableLiveData<Map<String, Double>>) {
        val mapa = mutableMapOf<String, Double>()
        for (trans in transacciones) {
            if (trans.tipo == tipo) {
                val categoria = trans.categoria.ifEmpty { "Otros" }
                val montoActual = mapa.getOrDefault(categoria, 0.0)
                mapa[categoria] = montoActual + trans.monto
            }
        }
        liveData.value = mapa
    }

    fun obtenerDesglosePorSubcategoria(nombreCategoria: String, tipoTransaccion: String): List<SubcategoryBreakdown> {
        val transacciones = _transaccionesMes.value ?: emptyList()
        val filtradas = transacciones.filter {
            it.tipo == tipoTransaccion &&
                    (it.categoria == nombreCategoria || (nombreCategoria == "Otros" && it.categoria.isEmpty()))
        }
        val totalCategoria = filtradas.sumOf { it.monto }
        if (totalCategoria == 0.0) return emptyList()
        val agrupadas = filtradas.groupBy { it.subcategoria ?: "Sin subcategoría" }
        val resultado = agrupadas.map { (subcatNombre, listaTransacciones) ->
            val montoSubcat = listaTransacciones.sumOf { it.monto }
            val porcentaje = (montoSubcat / totalCategoria) * 100
            SubcategoryBreakdown(subcatNombre, montoSubcat, porcentaje)
        }
        return resultado.sortedByDescending { it.monto }
    }
}