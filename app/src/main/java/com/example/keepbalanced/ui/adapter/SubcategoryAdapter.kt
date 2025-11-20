package com.example.keepbalanced.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.keepbalanced.model.SubcategoryBreakdown
import java.text.NumberFormat
import java.util.Locale

class SubcategoryAdapter(
    private val items: List<SubcategoryBreakdown>
) : RecyclerView.Adapter<SubcategoryAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Usamos el layout simple de Android 'simple_list_item_1' que tiene un text1
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reutilizamos un layout del sistema para no crear otro archivo XML
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val monto = currencyFormat.format(item.monto)

        // FORMATO: "Casa: 90,00 € (90.0%)"
        val texto = "${item.nombre}: $monto (%.1f%%)".format(item.porcentaje)

        holder.text.text = texto
        holder.text.textSize = 16f
        holder.text.setPadding(0, 20, 0, 20) // Un poco de aire
    }

    override fun getItemCount() = items.size
}