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
import com.example.keepbalanced.model.CategoryConfig
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
    private lateinit var tilFecha: TextInputLayout
    private lateinit var etFecha: TextInputEditText

    // Variables
    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Guardamos la configuración completa aquí
    private var configActual: CategoryConfig? = null

    // La lista que se está mostrando actualmente
    private var listaCategoriasActual: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(AddTransactionViewModel::class.java)

        // Enlazar vistas
        tilMonto = view.findViewById(R.id.til_monto)
        etMonto = view.findViewById(R.id.et_monto)
        actTipo = view.findViewById(R.id.act_tipo)
        actCategoria = view.findViewById(R.id.act_categoria)
        tilSubcategoria = view.findViewById(R.id.til_subcategoria)
        actSubcategoria = view.findViewById(R.id.act_subcategoria)
        btnGuardar = view.findViewById(R.id.btn_guardar)
        progressBar = view.findViewById(R.id.progress_bar_dialog)
        tilFecha = view.findViewById(R.id.til_fecha)
        etFecha = view.findViewById(R.id.et_fecha)

        setupSpinners()
        setupDatePicker()
        setupObservers()
        setupListeners()
    }

    private fun setupSpinners() {
        // Configuramos el selector de Tipo (Gasto / Ingreso)
        val tipos = listOf("Gasto", "Ingreso")
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        actTipo.setAdapter(tipoAdapter)
        actTipo.setText("Gasto", false) // Por defecto Gasto
    }

    private fun setupDatePicker() {
        etFecha.setText(dateFormat.format(selectedDate))
        val showPicker = {
            val builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar Fecha")
                .setSelection(selectedDate.time)
            val datePicker = builder.build()
            datePicker.addOnPositiveButtonClickListener { timestamp ->
                selectedDate = Date(timestamp)
                etFecha.setText(dateFormat.format(selectedDate))
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
        etFecha.setOnClickListener { showPicker() }
        tilFecha.setEndIconOnClickListener { showPicker() }
    }

    private fun setupObservers() {
        // Cuando recibimos la configuración de Firebase
        viewModel.categoryConfig.observe(viewLifecycleOwner) { config ->
            configActual = config
            // Cargamos la lista correspondiente a lo que esté seleccionado (Gasto o Ingreso)
            cargarCategoriasSegunTipo()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            btnGuardar.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        }

        viewModel.dismissDialog.observe(viewLifecycleOwner) { shouldDismiss ->
            if (shouldDismiss) dismiss()
        }
    }

    private fun setupListeners() {
        // --- CAMBIO IMPORTANTE: Listener para el Tipo (Gasto/Ingreso) ---
        actTipo.setOnItemClickListener { _, _, _, _ ->
            // Si cambiamos de Gasto a Ingreso (o viceversa), recargamos las categorías
            cargarCategoriasSegunTipo()
        }

        // Listener para la Categoría
        actCategoria.setOnItemClickListener { parent, _, position, _ ->
            val nombreCategoriaSeleccionada = parent.getItemAtPosition(position) as String
            val categoria = listaCategoriasActual.find { it.nombre == nombreCategoriaSeleccionada }
            actualizarSubcategorias(categoria)
        }

        btnGuardar.setOnClickListener { validarYGuardar() }
    }

    /**
     * Decide qué lista mostrar (Gastos o Ingresos) basándose en el selector de Tipo.
     */
    private fun cargarCategoriasSegunTipo() {
        val config = configActual ?: return // Si aún no ha cargado, salimos
        val tipoSeleccionado = actTipo.text.toString() // "Gasto" o "Ingreso"

        // Elegimos la lista correcta
        listaCategoriasActual = if (tipoSeleccionado == "Ingreso") {
            config.ingresos
        } else {
            config.gastos
        }

        // Actualizamos el desplegable de Categorías
        val nombres = listaCategoriasActual.map { it.nombre }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombres)
        actCategoria.setAdapter(adapter)

        // Limpiamos la selección actual porque ya no es válida
        actCategoria.setText("", false)

        // Ocultamos subcategorías al cambiar de tipo
        tilSubcategoria.visibility = View.GONE
        actSubcategoria.setText(null)
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

    private fun validarYGuardar() {
        val tipo = actTipo.text.toString()
        val montoStr = etMonto.text.toString()
        val categoria = actCategoria.text.toString()
        val subcategoria = if (tilSubcategoria.isVisible) actSubcategoria.text.toString().takeIf { it.isNotEmpty() } else null

        if (montoStr.isEmpty()) {
            tilMonto.error = "Monto obligatorio"
            return
        }
        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            tilMonto.error = "Monto inválido"
            return
        }
        if (categoria.isEmpty()) {
            Toast.makeText(requireContext(), "Selecciona categoría", Toast.LENGTH_SHORT).show()
            return
        }

        tilMonto.error = null
        viewModel.saveTransaction(tipo, monto, categoria, subcategoria, selectedDate)
    }

    companion object {
        const val TAG = "AddTransactionDialog"
    }
}