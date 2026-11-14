package com.finansio.app.data.local.dao

/** Hasil query total per nama kategori beserta warnanya, dipakai untuk grafik dan ringkasan. */
data class CategorySummary(
    val categoryName: String,
    val totalAmount: Long,
    val colorHex: String?
)
