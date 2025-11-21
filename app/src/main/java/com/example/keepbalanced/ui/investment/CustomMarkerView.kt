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
import java.util.Locale

@Suppress("DEPRECATION")
@SuppressLint("ViewConstructor")
class CustomMarkerView : MarkerView {

    private val labels: List<String>

    constructor(context: Context, layoutResource: Int, labels: List<String>) : super(
        context,
        layoutResource
    ) {
        this.labels = labels
        this.tvContent = findViewById(R.id.tv_marker_amount)
        this.tvDate = findViewById(R.id.tv_marker_date)
        this.dotView = findViewById(R.id.view_dot)
        this.format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    }

    private val tvContent: TextView
    private val tvDate: TextView
    private val dotView: View   // El punto

    private val format: NumberFormat

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null || highlight == null) return

        // 1. Poner el texto (Dinero y Fecha)
        tvContent.text = format.format(e.y.toDouble())
        val index = e.x.toInt()
        if (index >= 0 && index < labels.size) {
            tvDate.text = labels[index]
        }

        // 2. CAMBIAR EL COLOR DEL PUNTO
        // Obtenemos el gráfico para acceder a sus datos
        val chart = chartView as? LineChart
        if (chart != null) {
            // Buscamos el conjunto de datos (la línea) que se ha tocado
            val dataSet = chart.data.getDataSetByIndex(highlight.dataSetIndex)
            if (dataSet != null) {
                // Obtenemos el color de esa línea
                val colorLinea = dataSet.color

                // Teñimos nuestro punto de ese color
                dotView.backgroundTintList = ColorStateList.valueOf(colorLinea)
            }
        }

        super.refreshContent(e, highlight)
    }

    // 3. AJUSTAR POSICIÓN (CENTRADO EXACTO)
    override fun getOffset(): MPPointF {
        // Queremos que el centro del PUNTO (que está abajo del all) coincida con el dedo.

        // Centramos horizontalmente: -(mitad del ancho total)
        val xOffset = -(width / 2).toFloat()

        // Centramos verticalmente:
        // Subimos toda la altura de la vista (-height)
        // Pero bajamos la mitad de la altura del punto para que el centro del punto quede en la línea.
        // El punto mide 12dp aprox, así que ajustamos la mitad.
        val yOffset = -(height).toFloat() + (dotView.height / 2)

        return MPPointF(xOffset, yOffset)
    }
}