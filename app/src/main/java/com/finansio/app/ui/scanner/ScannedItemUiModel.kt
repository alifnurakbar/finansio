package com.finansio.app.ui.scanner

import com.finansio.app.data.local.entity.TransactionType
import java.util.UUID

/** Satu item hasil scan/suara yang siap ditampilkan dan diedit sebelum disimpan. */
data class ScannedItemUiModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Long,
    val quantity: Int = 1,
    val categoryId: Int? = null,
    val categoryName: String,
    val note: String? = null,
    val date: Long,
    val type: TransactionType = TransactionType.EXPENSE,
    /** True bila AI tidak menemukan harganya. Ditampilkan sebagai peringatan sampai pengguna mengisi harga. */
    val priceMissing: Boolean = false
)
