package com.example.keepbalanced.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.keepbalanced.R
import com.example.keepbalanced.ui.dialog.AddTransactionDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    // Declarar Vistas
    private lateinit var tvBalanceTotal: TextView
    private lateinit var tvIngresosTotal: TextView
    private lateinit var tvGastosTotal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Enlazar Vistas
        tvBalanceTotal = view.findViewById(R.id.tv_balance_total)
        tvIngresosTotal = view.findViewById(R.id.tv_ingresos_total)
        tvGastosTotal = view.findViewById(R.id.tv_gastos_total)
        progressBar = view.findViewById(R.id.progress_bar_home)
        fabAdd = view.findViewById(R.id.fab_add_transaction)

        setupObservers()
        setupListeners()

        return view
    }

    private fun setupListeners() {
        fabAdd.setOnClickListener {
            val dialog = AddTransactionDialog()
            dialog.show(parentFragmentManager, AddTransactionDialog.TAG)
        }
    }

    private fun setupObservers() {
        // Observador para el balance
        homeViewModel.balance.observe(viewLifecycleOwner) { balance ->
            tvBalanceTotal.text = formatCurrency(balance)
            val color = if (balance >= 0) {
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            } else {
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            }
            tvBalanceTotal.setTextColor(color)
        }

        // Observador para ingresos
        homeViewModel.totalIngresos.observe(viewLifecycleOwner) { ingresos ->
            tvIngresosTotal.text = formatCurrency(ingresos)
        }

        // Observador para gastos
        homeViewModel.totalGastos.observe(viewLifecycleOwner) { gastos ->
            tvGastosTotal.text = formatCurrency(gastos)
        }

        // Observador para los gráficos
        homeViewModel.transaccionesMes.observe(viewLifecycleOwner) { transacciones ->
            // TODO: Actualizar los gráficos circulares con esta lista
        }

        // Observador para el estado de carga
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observador de errores
        homeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        return format.format(amount)
    }

    // He quitado el onResume() que teníamos antes porque el SnapshotListener
    // del ViewModel ya hace que los datos se actualicen en tiempo real.
    // No es necesario recargar.
}