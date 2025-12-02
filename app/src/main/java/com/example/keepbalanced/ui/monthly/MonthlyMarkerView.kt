package com.example.keepbalanced.ui.monthly

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.example.keepbalanced.R
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
@SuppressLint("ViewConstructor")
class MonthlyMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tv_marker_date)
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        // 1. Obtener la lista de transacciones que guardamos en el ViewModel
        // El campo 'data' puede contener cualquier objeto
        @Suppress("UNCHECKED_CAST")
        val transactions = e.data as? List<Transaction>

        if (transactions.isNullOrEmpty()) {
            tvContent.text = "Sin movimientos"
        } else {
            // 2. Construir el texto (Top 3-4 movimientos)
            val sb = StringBuilder()

            // Ordenamos por monto (mayor a menor) para ver lo importante primero
            val sortedList = transactions.sortedByDescending { it.monto }

            // Cogemos fecha del primero para el título
            val date = sortedList.first().fecha
            if (date != null) tvDate.text = dateFormat.format(date)

            for (t in sortedList) {
                val categoria = t.categoria.ifEmpty { "General" }
                val monto = currencyFormat.format(t.monto)

                // Formato: "Supermercado: 50,00 €"
                sb.append("$categoria: $monto\n")
            }
            tvContent.text = sb.toString().trim()
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Centrar horizontalmente y poner encima de la barra
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}