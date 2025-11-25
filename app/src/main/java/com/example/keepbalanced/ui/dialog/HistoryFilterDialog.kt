package com.example.keepbalanced.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import com.example.keepbalanced.R
import com.example.keepbalanced.model.CategoryConfig
import com.example.keepbalanced.model.HistoryFilter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFilterDialog(
    private val currentFilter: HistoryFilter,
    private val onApply: (HistoryFilter) -> Unit
) : BottomSheetDialogFragment() {

    // Vistas
    private lateinit var etDateFrom: TextInputEditText
    private lateinit var etDateTo: TextInputEditText
    private lateinit var tilDateFrom: TextInputLayout
    private lateinit var tilDateTo: TextInputLayout
    private lateinit var actCategory: AutoCompleteTextView

    // Nuevas vistas de importe
    private lateinit var etAmountMin: TextInputEditText
    private lateinit var etAmountMax: TextInputEditText

    private lateinit var btnApply: Button
    private lateinit var btnReset: Button

    private var selectedDateFrom: Date? = currentFilter.dateFrom
    private var selectedDateTo: Date? = currentFilter.dateTo
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_history_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Enlazar vistas
        etDateFrom = view.findViewById(R.id.et_date_from)
        etDateTo = view.findViewById(R.id.et_date_to)
        tilDateFrom = view.findViewById(R.id.til_date_from)
        tilDateTo = view.findViewById(R.id.til_date_to)
        actCategory = view.findViewById(R.id.act_category_filter)

        // Enlazamos los nuevos campos de texto
        etAmountMin = view.findViewById(R.id.et_amount_min)
        etAmountMax = view.findViewById(R.id.et_amount_max)

        btnApply = view.findViewById(R.id.btn_apply_filters)
        btnReset = view.findViewById(R.id.btn_reset_filters)

        // Configuración inicial
        setupDatePickers()
        setupCategories()
        restoreCurrentState()

        // --- BOTÓN APLICAR ---
        btnApply.setOnClickListener {
            // Leemos el texto y lo convertimos a Double (o null si está vacío)
            val minStr = etAmountMin.text.toString()
            val maxStr = etAmountMax.text.toString()

            val minAmount = if (minStr.isNotEmpty()) minStr.toDoubleOrNull() else null
            val maxAmount = if (maxStr.isNotEmpty()) maxStr.toDoubleOrNull() else null

            val cat = actCategory.text.toString().takeIf { it.isNotEmpty() && it != "Todas" }

            val filter = HistoryFilter(
                dateFrom = selectedDateFrom,
                dateTo = selectedDateTo,
                minAmount = minAmount,
                maxAmount = maxAmount,
                category = cat,
                type = currentFilter.type // Mantenemos el tipo que venía del Home
            )
            onApply(filter)
            dismiss()
        }

        // --- BOTÓN LIMPIAR ---
        btnReset.setOnClickListener {
            // Al limpiar, mantenemos solo el tipo (Gasto/Ingreso) para no romper la navegación
            onApply(HistoryFilter(type = currentFilter.type))
            dismiss()
        }
    }

    private fun setupDatePickers() {
        val showPicker = { isFrom: Boolean ->
            val builder = MaterialDatePicker.Builder.datePicker()
            builder.setTitleText(if (isFrom) "Fecha Inicio" else "Fecha Fin")
            val selection = if (isFrom) selectedDateFrom?.time else selectedDateTo?.time
            if (selection != null) builder.setSelection(selection)

            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { selectionTimestamp ->
                val date = Date(selectionTimestamp)
                if (isFrom) {
                    selectedDateFrom = date
                    etDateFrom.setText(dateFormat.format(date))
                } else {
                    selectedDateTo = date
                    etDateTo.setText(dateFormat.format(date))
                }
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }

        etDateFrom.setOnClickListener { showPicker(true) }
        tilDateFrom.setEndIconOnClickListener { showPicker(true) }
        etDateTo.setOnClickListener { showPicker(false) }
        tilDateTo.setEndIconOnClickListener { showPicker(false) }
    }

    private fun setupCategories() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.activate().addOnCompleteListener {
            val json = remoteConfig.getString("categorias_json")
            if (json.isNotEmpty()) {
                try {
                    val config = Gson().fromJson(json, CategoryConfig::class.java)
                    val list = ArrayList<String>()
                    list.add("Todas")

                    if (currentFilter.type == "gasto" || currentFilter.type == null) {
                        list.addAll(config.gastos.map { it.nombre })
                    }
                    if (currentFilter.type == "ingreso" || currentFilter.type == null) {
                        list.addAll(config.ingresos.map { it.nombre })
                    }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, list.distinct())
                    actCategory.setAdapter(adapter)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun restoreCurrentState() {
        // Restaurar fechas
        selectedDateFrom?.let { etDateFrom.setText(dateFormat.format(it)) }
        selectedDateTo?.let { etDateTo.setText(dateFormat.format(it)) }

        // Restaurar categoría
        actCategory.setText(currentFilter.category ?: "Todas", false)

        // Restaurar importes (si existen)
        // Usamos toInt() si son redondos para que quede más bonito ("50" vs "50.0")
        currentFilter.minAmount?.let {
            etAmountMin.setText(if (it % 1 == 0.0) it.toInt().toString() else it.toString())
        }
        currentFilter.maxAmount?.let {
            etAmountMax.setText(if (it % 1 == 0.0) it.toInt().toString() else it.toString())
        }
    }

    companion object {
        const val TAG = "HistoryFilterDialog"
    }
}