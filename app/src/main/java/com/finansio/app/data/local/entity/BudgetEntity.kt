package com.finansio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Batas pengeluaran bulanan untuk satu kategori pengeluaran.
 * Satu kategori hanya punya satu anggaran, dan anggaran ini berlaku otomatis setiap bulan.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val categoryId: Int,
    val monthlyLimit: Long
)
