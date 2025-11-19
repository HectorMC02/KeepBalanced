package com.example.keepbalanced.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.keepbalanced.R
import com.example.keepbalanced.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsAdapter(
    private var transactions: List<Transaction> = emptyList()
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    // Formateadores
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "ES"))

    // Actualizar la lista
    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]

        // 1. Categoría y Subcategoría
        val titulo = if (item.subcategoria.isNullOrEmpty()) {
            item.categoria
        } else {
            "${item.categoria} (${item.subcategoria})"
        }
        holder.tvCategory.text = titulo

        // 2. Fecha
        if (item.fecha != null) {
            holder.tvDate.text = dateFormat.format(item.fecha)
        } else {
            holder.tvDate.text = "Sin fecha"
        }

        // 3. Monto y Color
        val montoFormateado = currencyFormat.format(item.monto)
        if (item.tipo == "gasto") {
            holder.tvAmount.text = "-$montoFormateado"
            holder.tvAmount.setTextColor(Color.RED)
        } else {
            holder.tvAmount.text = "+$montoFormateado"
            holder.tvAmount.setTextColor(Color.parseColor("#006400")) // Verde oscuro
        }
    }

    override fun getItemCount(): Int = transactions.size

    // Clase interna para guardar las vistas
    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tv_item_category)
        val tvDate: TextView = view.findViewById(R.id.tv_item_date)
        val tvAmount: TextView = view.findViewById(R.id.tv_item_amount)
    }
}