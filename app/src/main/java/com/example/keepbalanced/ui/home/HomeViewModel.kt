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
    private val _totalIngresos = MutableLiveData(0.0)
    val totalIngresos: LiveData<Double> = _totalIngresos
    private val _totalGastos = MutableLiveData(0.0)
    val totalGastos: LiveData<Double> = _totalGastos
    private val _balance = MutableLiveData(0.0)
    val balance: LiveData<Double> = _balance
    private val _transaccionesMes = MutableLiveData<List<Transaction>>()
    val transaccionesMes: LiveData<List<Transaction>> = _transaccionesMes
    private val _gastosPorCategoria = MutableLiveData<Map<String, Double>>()
    val gastosPorCategoria: LiveData<Map<String, Double>> = _gastosPorCategoria
    private val _ingresosPorCategoria = MutableLiveData<Map<String, Double>>()
    val ingresosPorCategoria: LiveData<Map<String, Double>> = _ingresosPorCategoria

    // --- HISTORIAL COMPLETO DATA ---
    private val _fullHistoryList = MutableLiveData<List<Transaction>>()
    val fullHistoryList: LiveData<List<Transaction>> = _fullHistoryList

    // Variable para controlar si hemos llegado al final
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
            cargarDatosDashboard()
        }
    }

    fun cargarDatosDashboard() {
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
                    calcularTotales(lista)
                    agruparPorCategoria(lista, "gasto", _gastosPorCategoria)
                    agruparPorCategoria(lista, "ingreso", _ingresosPorCategoria)
                }
            }
    }

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
                // Al cambiar filtro, asumimos que hay datos (hasta que se demuestre lo contrario)
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

        // Pedimos EXACTAMENTE el límite actual.
        query = query.limit(currentLimit)

        query.get().addOnSuccessListener { snapshots ->
            _isLoading.value = false

            if (!snapshots.isEmpty) {
                lastVisibleDocument = snapshots.documents[snapshots.size() - 1]

                var lista = snapshots.toObjects(Transaction::class.java)

                // Filtro local de importe
                if (currentFilter.minAmount != null) lista = lista.filter { it.monto >= currentFilter.minAmount!! }
                if (currentFilter.maxAmount != null) lista = lista.filter { it.monto <= currentFilter.maxAmount!! }

                _fullHistoryList.value = lista

                // --- LÓGICA FIN DE LISTA ---
                // Si Firebase devuelve MENOS documentos de los que pedimos en el límite,
                // significa que ya no hay más en la base de datos.
                if (snapshots.size() < currentLimit) {
                    _isEndOfList.value = true
                } else {
                    _isEndOfList.value = false
                }

            } else {
                if (reset || newFilter != null) _fullHistoryList.value = emptyList()
                // Si la consulta devuelve 0, obviamente es el final
                _isEndOfList.value = true
            }
        }.addOnFailureListener { e ->
            _isLoading.value = false
            _errorMessage.value = "Error historial: ${e.message}"
        }
    }

    // ... (resto de funciones auxiliares IGUALES) ...
    private fun calcularTotales(transacciones: List<Transaction>) {
        var ingresos = 0.0
        var gastos = 0.0
        for (trans in transacciones) {
            if (trans.tipo == "ingreso") ingresos += trans.monto
            else if (trans.tipo == "gasto") gastos += trans.monto
        }
        _totalIngresos.value = ingresos
        _totalGastos.value = gastos
        _balance.value = ingresos - gastos
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