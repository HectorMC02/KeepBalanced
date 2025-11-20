package com.example.keepbalanced.ui.investment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.data.Entry
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale

class InvestmentViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    // Datos para el gráfico de línea (X = Timestamp, Y = Dinero Acumulado)
    private val _chartData = MutableLiveData<List<Entry>>()
    val chartData: LiveData<List<Entry>> = _chartData

    // Total acumulado actual (número grande arriba)
    private val _totalInvertido = MutableLiveData<Double>(0.0)
    val totalInvertido: LiveData<Double> = _totalInvertido

    // Lista de etiquetas para el eje X (Fechas en texto)
    private val _dateLabels = MutableLiveData<List<String>>()
    val dateLabels: LiveData<List<String>> = _dateLabels

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        if (userId.isNotEmpty()) {
            cargarHistorialInversiones()
        }
    }

    fun cargarHistorialInversiones() {
        _isLoading.value = true

        // Pedimos TODAS las transacciones de tipo 'gasto' ordenadas por fecha (antiguas primero)
        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("tipo", "gasto")
            .orderBy("fecha", Query.Direction.ASCENDING) // Importante: De viejo a nuevo
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    // Nota: Si falla, puede ser porque falta un índice compuesto en Firebase.
                    // Revisa el Logcat si no carga.
                    _errorMessage.value = "Error al cargar inversiones: ${e.message}"
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val todasLasTransacciones = snapshots.toObjects(Transaction::class.java)
                    procesarDatosInversion(todasLasTransacciones)
                }
            }
    }

    private fun procesarDatosInversion(lista: List<Transaction>) {
        // 1. Filtramos solo las que sean de la categoría "Inversión"
        // (Asegúrate de que el nombre coincide con tu JSON, mayúsculas incluidas)
        val inversiones = lista.filter { it.categoria == "Inversión" }

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val dateFormat = SimpleDateFormat("dd MMM yy", Locale("es", "ES"))

        var sumaAcumulada = 0.0
        var index = 0f

        for (transaccion in inversiones) {
            // Sumamos al acumulado
            sumaAcumulada += transaccion.monto

            // Creamos el punto para el gráfico
            // Eje X: Simplemente un índice (0, 1, 2...)
            // Eje Y: La suma acumulada hasta ese momento
            entries.add(Entry(index, sumaAcumulada.toFloat()))

            // Guardamos la fecha bonita para luego ponérsela al eje X
            val fechaTexto = if (transaccion.fecha != null) dateFormat.format(transaccion.fecha) else ""
            labels.add(fechaTexto)

            index++
        }

        _totalInvertido.value = sumaAcumulada
        _dateLabels.value = labels
        _chartData.value = entries
    }
}