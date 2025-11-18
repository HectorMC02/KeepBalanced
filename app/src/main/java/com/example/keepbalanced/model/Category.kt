package com.example.keepbalanced.model

/**
 * Data class para deserializar el JSON de categorías desde Firebase Remote Config.
 */

data class Category(
    val nombre: String = "",
    val subcategorias: List<String> = emptyList()
)

data class CategoryConfig(
    val categorias: List<Category> = emptyList()
)