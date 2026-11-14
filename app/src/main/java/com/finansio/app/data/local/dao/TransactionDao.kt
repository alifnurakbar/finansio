package com.finansio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Int): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :start AND :end")
    fun getTotalByTypeAndDateRange(type: TransactionType, start: Long, end: Long): Flow<Long?>

    @Query("""
        SELECT categoryName, SUM(amount) as totalAmount, 
        (SELECT colorHex FROM categories WHERE id = categoryId LIMIT 1) as colorHex 
        FROM transactions 
        WHERE type = :type AND date BETWEEN :start AND :end 
        GROUP BY categoryName 
        ORDER BY totalAmount DESC
    """)
    fun getExpenseByCategoryInRange(type: TransactionType, start: Long, end: Long): Flow<List<CategorySummary>>

    /** Total pengeluaran per kategori (berdasarkan id) dalam rentang tanggal, untuk menghitung progres anggaran. */
    @Query("""
        SELECT categoryId, SUM(amount) AS total
        FROM transactions
        WHERE type = 'EXPENSE' AND categoryId IS NOT NULL AND date BETWEEN :start AND :end
        GROUP BY categoryId
    """)
    fun getExpenseTotalsByCategoryId(start: Long, end: Long): Flow<List<CategoryIdTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionEntity)

    @Update
    fun update(transaction: TransactionEntity)

    @Delete
    fun delete(transaction: TransactionEntity)
}
