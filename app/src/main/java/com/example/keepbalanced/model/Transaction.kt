package com.example.keepbalanced.model
import java.util.Date

data class Transaction(
    val usuarioId: String = "",
    val tipo: String = "gasto",
    val monto: Double = 0.0,
    val categoria: String = "",
    val subcategoria: String? = null,

    val fecha: Date? = null,

    val mes: Int = 0,
    val anio: Int = 0
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "gasto", 0.0, "", null, null, 0, 0)
}