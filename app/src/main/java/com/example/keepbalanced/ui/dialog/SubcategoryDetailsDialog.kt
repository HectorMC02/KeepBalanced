package com.example.keepbalanced.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat // Necesario para los colores
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.keepbalanced.R
import com.example.keepbalanced.model.SubcategoryBreakdown
import com.example.keepbalanced.ui.adapter.SubcategoryAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

class SubcategoryDetailsDialog(
    private val categoryName: String,
    private val items: List<SubcategoryBreakdown>,
    private val transactionType: String // --- NUEVO PARÁMETRO ---
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_subcategory_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tv_dialog_category_title)
        val tvTotal = view.findViewById<TextView>(R.id.tv_dialog_total_amount)
        val rvList = view.findViewById<RecyclerView>(R.id.rv_subcategories)

        tvTitle.text = categoryName

        val total = items.sumOf { it.monto }
        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        val totalFormateado = format.format(total)

        // --- LÓGICA DE COLOR Y SIGNO ---
        if (transactionType == "ingreso") {
            // Verde y signo +
            tvTotal.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            tvTotal.text = "+$totalFormateado"
        } else {
            // Rojo y signo - (Gasto por defecto)
            tvTotal.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            tvTotal.text = "-$totalFormateado"
        }

        rvList.layoutManager = LinearLayoutManager(context)
        rvList.adapter = SubcategoryAdapter(items)
    }

    companion object {
        const val TAG = "SubcategoryDetailsDialog"
    }
}