package com.finansio.app.data.local.dao

/** Hasil query total pengeluaran per id kategori, dipakai untuk menghitung progres anggaran. */
data class CategoryIdTotal(
    val categoryId: Int,
    val total: Long
)
