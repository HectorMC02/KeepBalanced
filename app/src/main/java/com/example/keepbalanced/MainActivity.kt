package com.example.keepbalanced

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)

        // Encontrar el NavController
        val navController = findNavController(R.id.nav_host_fragment_container)

        // Configuración de la barra superior (ActionBar) para que muestre los títulos
        // de los fragments (Home, Inversión, etc.)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                // IDs de tu menú
                R.id.navigation_home,
                R.id.navigation_monthly,
                R.id.navigation_yearly,
                R.id.navigation_investment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Conectar la barra de navegación inferior con el NavController
        navView.setupWithNavController(navController)
    }
}