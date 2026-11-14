package com.finansio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabel `categories`: nama, jenis (pemasukan/pengeluaran), dan warna kategori. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: TransactionType,
    val colorHex: String
)
