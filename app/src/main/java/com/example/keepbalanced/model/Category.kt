package com.example.keepbalanced.model
data class Category(
    val nombre: String = "",
    val subcategorias: List<String> = emptyList()
)
data class CategoryConfig(
    val gastos: List<Category> = emptyList(),
    val ingresos: List<Category> = emptyList()
)