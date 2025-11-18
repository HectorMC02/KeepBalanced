package com.example.keepbalanced.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.example.keepbalanced.model.Category
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
// --- Imports nuevos ---
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTransactionDialog : BottomSheetDialogFragment() {

    private lateinit var viewModel: AddTransactionViewModel

    // Vistas
    private lateinit var tilMonto: TextInputLayout
    private lateinit var etMonto: TextInputEditText
    private lateinit var actTipo: AutoCompleteTextView
    private lateinit var actCategoria: AutoCompleteTextView
    private lateinit var tilSubcategoria: TextInputLayout
    private lateinit var actSubcategoria: AutoCompleteTextView
    private lateinit var btnGuardar: Button
    private lateinit var progressBar: ProgressBar

    // --- Vistas nuevas para la fecha ---
    private lateinit var tilFecha: TextInputLayout
    private lateinit var etFecha: TextInputEditText

    // --- Variables para manejar la fecha ---
    private var selectedDate: Date = Date() // Por defecto, hoy
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    private var listaCategorias: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Solo inflamos la vista
        return inflater.inflate(R.layout.dialog_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AddTransactionViewModel::class.java)

        // Enlazamos las vistas
        tilMonto = view.findViewById(R.id.til_monto)
        etMonto = view.findViewById(R.id.et_monto)
        actTipo = view.findViewById(R.id.act_tipo)
        actCategoria = view.findViewById(R.id.act_categoria)
        tilSubcategoria = view.findViewById(R.id.til_subcategoria)
        actSubcategoria = view.findViewById(R.id.act_subcategoria)
        btnGuardar = view.findViewById(R.id.btn_guardar)
        progressBar = view.findViewById(R.id.progress_bar_dialog)

        // --- Enlazamos la nueva vista de fecha ---
        tilFecha = view.findViewById(R.id.til_fecha)
        etFecha = view.findViewById(R.id.et_fecha)

        // Ejecutamos la configuración
        setupSpinners()
        setupDatePicker() // <-- Llamamos al nuevo método
        setupObservers()
        setupListeners()
    }

    private fun setupSpinners() {
        val tipos = listOf("Gasto", "Ingreso")
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        actTipo.setAdapter(tipoAdapter)
        actTipo.setText("Gasto", false)
    }

    /**
     * Nuevo método para configurar el selector de fecha
     */
    private fun setupDatePicker() {
        // 1. Poner la fecha de hoy por defecto en el campo de texto
        etFecha.setText(dateFormat.format(selectedDate))

        // 2. Poner listeners para abrir el diálogo de calendario
        etFecha.setOnClickListener { showDatePicker() }
        tilFecha.setEndIconOnClickListener { showDatePicker() }
    }

    /**
     * Nuevo método que crea y muestra el MaterialDatePicker
     */
    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar Fecha")
            .setSelection(selectedDate.time) // Selecciona la fecha actual por defecto

        val datePicker = builder.build()

        // Listener para cuando el usuario pulsa "Aceptar"
        datePicker.addOnPositiveButtonClickListener { timestamp ->
            // El 'timestamp' está en UTC. Lo ajustamos a la zona local.
            // (Nota: 'Date(timestamp)' suele manejar la zona local bien)
            selectedDate = Date(timestamp)
            etFecha.setText(dateFormat.format(selectedDate))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }

    private fun setupObservers() {
        // ... (los observadores de categoryConfig, isLoading, errorMessage, dismissDialog
        // siguen exactamente igual que antes) ...
        viewModel.categoryConfig.observe(viewLifecycleOwner) { config ->
            listaCategorias = config.categorias
            val nombresCategorias = listaCategorias.map { it.nombre }
            val categoriaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresCategorias)
            actCategoria.setAdapter(categoriaAdapter)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            btnGuardar.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.dismissDialog.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) {
                dismiss()
            }
        }
    }

    private fun setupListeners() {
        actCategoria.setOnItemClickListener { parent, _, position, _ ->
            val nombreCategoriaSeleccionada = parent.getItemAtPosition(position) as String
            val categoria = listaCategorias.find { it.nombre == nombreCategoriaSeleccionada }
            actualizarSubcategorias(categoria)
        }

        btnGuardar.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun actualizarSubcategorias(categoria: Category?) {
        if (categoria != null && categoria.subcategorias.isNotEmpty()) {
            tilSubcategoria.visibility = View.VISIBLE
            val subcatAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoria.subcategorias)
            actSubcategoria.setAdapter(subcatAdapter)
            actSubcategoria.setText("", false)
        } else {
            tilSubcategoria.visibility = View.GONE
            actSubcategoria.setText(null)
        }
    }

    /**
     * Valida los campos y llama al ViewModel para guardar.
     */
    private fun validarYGuardar() {
        val tipo = actTipo.text.toString()
        val montoStr = etMonto.text.toString()
        val categoria = actCategoria.text.toString()
        val subcategoria = if (tilSubcategoria.isVisible) actSubcategoria.text.toString().takeIf { it.isNotEmpty() } else null

        // La variable 'selectedDate' ya la tenemos guardada

        // Validación
        if (montoStr.isEmpty()) {
            tilMonto.error = "El monto es obligatorio"
            return
        }

        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            tilMonto.error = "Monto no válido"
            return
        }

        if (categoria.isEmpty()) {
            Toast.makeText(requireContext(), "Debes seleccionar una categoría", Toast.LENGTH_SHORT).show()
            return
        }

        tilMonto.error = null

        // --- CAMBIO AQUÍ ---
        // Pasamos la 'selectedDate' al ViewModel
        viewModel.saveTransaction(tipo, monto, categoria, subcategoria, selectedDate)
    }

    companion object {
        const val TAG = "AddTransactionDialog"
    }
}