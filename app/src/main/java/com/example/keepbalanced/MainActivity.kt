package com.example.keepbalanced

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.keepbalanced.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración con ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.bottomNavView

        // --- AQUÍ ESTÁ LA CORRECCIÓN ---
        // 1. En lugar de findNavController(ID), buscamos primero el FRAGMENTO
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment

        // 2. Y obtenemos el controlador DESDE el fragmento
        val navController = navHostFragment.navController

        // Conectar la barra inferior con el controlador
        navView.setupWithNavController(navController)
    }
}