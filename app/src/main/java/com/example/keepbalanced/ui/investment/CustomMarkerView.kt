package com.example.keepbalanced.ui.investment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import com.example.keepbalanced.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
@SuppressLint("ViewConstructor")
class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tv_marker_amount)
    private val tvDate: TextView = findViewById(R.id.tv_marker_date)
    private val dotView: View = findViewById(R.id.view_dot)

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

    // Convertimos los 12dp del punto a píxeles reales
    private val dotSizePixels = 12 * context.resources.displayMetrics.density

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) return

        tvContent.text = currencyFormat.format(e.y.toDouble())
        val date = Date(e.x.toLong())
        tvDate.text = dateFormat.format(date)

        val chart = chartView as? LineChart
        if (chart != null) {
            val dataSet = chart.data.getDataSetByIndex(highlight.dataSetIndex)
            if (dataSet != null) {
                dotView.backgroundTintList = ColorStateList.valueOf(dataSet.color)
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // 1. Centrar horizontalmente
        val xOffset = -(width / 2).toFloat()

        // 2. Posición Vertical
        // El punto mide 12dp. Su centro matemático está a 6dp del fondo.
        val dotRadius = dotSizePixels / 2

        // --- CORRECCIÓN VISUAL ---
        // Subimos el marcador unos píxeles extra (ej: 3dp) para compensar el efecto visual.
        // Si sigue pareciendo bajo, aumenta este número (ej: a 4 o 5).
        val visualAdjustment = 1 * context.resources.displayMetrics.density

        // Fórmula:
        // -height        -> Sube all el marcador para que su borde inferior toque la línea.
        // +dotRadius     -> Baja hasta el centro del círculo.
        // -visualAdjustment -> Lo sube un poquito para que quede "clavado" o ligeramente encima.
        val yOffset = -(height.toFloat()) + dotRadius - visualAdjustment

        return MPPointF(xOffset, yOffset)
    }
}