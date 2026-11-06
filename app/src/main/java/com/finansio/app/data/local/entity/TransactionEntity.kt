package com.finansio.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amount: Long,
    val type: TransactionType,
    val categoryId: Int?,
    val categoryName: String,
    val note: String?,
    val date: Long
)
