package com.example.keepbalanced.model

import java.util.Date

data class HistoryFilter(
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val category: String? = null, // null significa "Todas"
    val type: String? = null      // "gasto" o "ingreso" (viene de los chips del Home)
)