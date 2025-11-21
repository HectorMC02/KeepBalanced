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

// Clase auxiliar para enviar los 4 grupos de datos al fragmento
data class InvestmentChartData(
    val entriesTotal: List<Entry>,
    val entriesRentaFija: List<Entry>,
    val entriesRentaVariable: List<Entry>,
    val entriesOro: List<Entry>,
    val dateLabels: List<String>
)

@Suppress("DEPRECATION")
class InvestmentViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser?.uid ?: ""

    // Ahora enviamos un objeto complejo con todas las líneas
    private val _chartData = MutableLiveData<InvestmentChartData>()
    val chartData: LiveData<InvestmentChartData> = _chartData

    private val _totalInvertido = MutableLiveData<Double>(0.0)
    val totalInvertido: LiveData<Double> = _totalInvertido

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
        db.collection("transacciones")
            .whereEqualTo("usuarioId", userId)
            .whereEqualTo("tipo", "gasto")
            .whereEqualTo("categoria", "Inversión") // Solo traemos inversiones
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    _errorMessage.value = "Error: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    procesarDatos(snapshots.toObjects(Transaction::class.java))
                }
            }
    }

    private fun procesarDatos(lista: List<Transaction>) {
        val entriesTotal = ArrayList<Entry>()
        val entriesFija = ArrayList<Entry>()
        val entriesVariable = ArrayList<Entry>()
        val entriesOro = ArrayList<Entry>()
        val labels = ArrayList<String>()

        val dateFormat = SimpleDateFormat("dd MMM yy", Locale("es", "ES"))

        // Acumuladores
        var sumTotal = 0.0
        var sumFija = 0.0
        var sumVariable = 0.0
        var sumOro = 0.0
        var index = 0f

        for (t in lista) {
            val sub = t.subcategoria ?: ""

            // Sumar al acumulador correspondiente
            if (sub.contains("Fija", ignoreCase = true)) sumFija += t.monto
            else if (sub.contains("Variable", ignoreCase = true)) sumVariable += t.monto
            else if (sub.contains("Oro", ignoreCase = true)) sumOro += t.monto

            // El total siempre suma
            sumTotal += t.monto

            // Añadir puntos (Solo añadimos punto si el valor > 0 para no ensuciar el inicio)
            entriesTotal.add(Entry(index, sumTotal.toFloat()))
            if (sumFija > 0) entriesFija.add(Entry(index, sumFija.toFloat()))
            if (sumVariable > 0) entriesVariable.add(Entry(index, sumVariable.toFloat()))
            if (sumOro > 0) entriesOro.add(Entry(index, sumOro.toFloat()))

            // Etiqueta de fecha
            val fechaTexto = if (t.fecha != null) dateFormat.format(t.fecha) else ""
            labels.add(fechaTexto)

            index++
        }

        _totalInvertido.value = sumTotal

        // Enviamos el paquete completo
        _chartData.value = InvestmentChartData(
            entriesTotal, entriesFija, entriesVariable, entriesOro, labels
        )
    }
}