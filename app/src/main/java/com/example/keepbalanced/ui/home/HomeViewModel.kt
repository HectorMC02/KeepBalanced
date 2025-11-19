package com.example.keepbalanced.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.Transaction
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import java.util.Calendar

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    private val _totalIngresos = MutableLiveData<Double>(0.0)
    val totalIngresos: LiveData<Double> = _totalIngresos

    private val _totalGastos = MutableLiveData<Double>(0.0)
    val totalGastos: LiveData<Double> = _totalGastos

    private val _balance = MutableLiveData<Double>(0.0)
    val balance: LiveData<Double> = _balance

    private val _transaccionesMes = MutableLiveData<List<Transaction>>()
    val transaccionesMes: LiveData<List<Transaction>> = _transaccionesMes

    // --- NUEVO: Datos para el gráfico (Mapa: Categoría -> Total) ---
    private val _gastosPorCategoria = MutableLiveData<Map<String, Double>>()
    val gastosPorCategoria: LiveData<Map<String, Double>> = _gastosPorCategoria

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        if (userId.isNotEmpty()) {
            cargarDatosMesActual()
        }
    }

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
                    _errorMessage.value = "Error al cargar datos: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val listaTransacciones = snapshots.toObjects(Transaction::class.java)
                    _transaccionesMes.value = listaTransacciones
                    calcularTotales(listaTransacciones)
                    // --- NUEVO: Calcular agrupación para gráfico ---
                    agruparGastosPorCategoria(listaTransacciones)
                }
            }
    }

    private fun calcularTotales(transacciones: List<Transaction>) {
        var ingresos = 0.0
        var gastos = 0.0

        for (trans in transacciones) {
            if (trans.tipo == "ingreso") {
                ingresos += trans.monto
            } else if (trans.tipo == "gasto") {
                gastos += trans.monto
            }
        }

        _totalIngresos.value = ingresos
        _totalGastos.value = gastos
        _balance.value = ingresos - gastos
    }

    /**
     * Agrupa las transacciones de tipo "gasto" por categoría y suma sus montos.
     */
    private fun agruparGastosPorCategoria(transacciones: List<Transaction>) {
        val mapa = mutableMapOf<String, Double>()

        for (trans in transacciones) {
            if (trans.tipo == "gasto") {
                val categoria = trans.categoria.ifEmpty { "Otros" }
                val montoActual = mapa.getOrDefault(categoria, 0.0)
                mapa[categoria] = montoActual + trans.monto
            }
        }
        _gastosPorCategoria.value = mapa
    }
}