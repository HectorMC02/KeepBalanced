package com.example.keepbalanced.ui.monthly

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.example.keepbalanced.R
import com.example.keepbalanced.model.Transaction
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.util.Locale

@Suppress("DEPRECATION")
@SuppressLint("ViewConstructor")
class MonthlyPieMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    // Reutilizamos el layout del sistema.
    // tvTitle es el texto pequeño de arriba (originalmente la fecha)
    // tvContent es el texto grande de abajo
    private val tvTitle: TextView = findViewById(R.id.tv_marker_date)
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val pieEntry = e as? PieEntry ?: return

        @Suppress("UNCHECKED_CAST")
        val transactions = pieEntry.data as? List<Transaction>

        if (transactions.isNullOrEmpty()) {
            tvTitle.text = pieEntry.label
            tvContent.text = "Sin detalles"
        } else {
            // 1. CALCULAR EL TOTAL DE LA CATEGORÍA
            val totalCategoria = transactions.sumOf { it.monto }

            // 2. PONERLO EN EL TÍTULO (Ej: "Comida: 150,00 €")
            tvTitle.text = "${pieEntry.label}: ${currencyFormat.format(totalCategoria)}"

            // 3. Desglose por subcategorías (Igual que antes)
            val grouped = transactions.groupBy { it.subcategoria ?: "General" }

            val breakdown = grouped.map { (subcat, list) ->
                subcat to list.sumOf { it.monto }
            }.sortedByDescending { it.second }

            val sb = StringBuilder()
            for ((subcat, amount) in breakdown) {
                sb.append("$subcat: ${currencyFormat.format(amount)}\n")
            }

            tvContent.text = sb.toString().trim()
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}