package com.example.keepbalanced.ui.investment

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.example.keepbalanced.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.util.Locale

@Suppress("DEPRECATION")
@SuppressLint("ViewConstructor")
class InvestmentPieMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvTitle: TextView = findViewById(R.id.tv_marker_date)
    private val tvContent: TextView = findViewById(R.id.tv_marker_content)

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val pieEntry = e as? PieEntry ?: return

        // En el gráfico de inversión, el label contiene "Oro\n(Meta 15%)"
        // Limpiamos el salto de línea para el título del bocadillo
        val labelLimpio = pieEntry.label.split("\n")[0]

        tvTitle.text = labelLimpio

        // Mostramos el valor monetario
        val monto = currencyFormat.format(pieEntry.value)
        tvContent.text = "Total: $monto"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Centrar encima del dedo
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}