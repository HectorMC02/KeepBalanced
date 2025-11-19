package com.example.keepbalanced.model

data class Category(
    val nombre: String = "",
    val subcategorias: List<String> = emptyList()
)

// Actualizamos la configuración para reflejar el nuevo JSON
data class CategoryConfig(
    val gastos: List<Category> = emptyList(),
    val ingresos: List<Category> = emptyList()
)