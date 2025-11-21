package com.example.keepbalanced

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat // <-- Importante
import androidx.core.view.WindowInsetsCompat // <-- Importante
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.keepbalanced.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Esto activa el dibujo detrás de las barras (lo que causa el problema visual
        // si no se gestiona el padding después)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- AQUÍ ESTÁ LA SOLUCIÓN ---
        // Este bloque detecta cuánto miden las barras del sistema (arriba y abajo)
        // y empuja el contenido de tu app hacia dentro para que no toque el notch.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Aplicamos padding (relleno) según el tamaño de las barras
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            // Nota: Puse 0 abajo porque la BottomBar ya gestiona su propio espacio,
            // pero si ves que la barra de abajo se corta, cambia el 0 por 'systemBars.bottom'
            insets
        }

        val navView: BottomNavigationView = binding.bottomNavView

        // 1. Buscamos el fragmento contenedor
        // Usamos 'findFragmentById' porque con FragmentContainerView es la forma segura
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment

        // 2. Obtenemos el controlador
        val navController = navHostFragment.navController

        // 3. Conectamos la barra de abajo
        navView.setupWithNavController(navController)
    }
}