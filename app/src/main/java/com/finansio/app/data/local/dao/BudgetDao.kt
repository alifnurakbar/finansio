package com.finansio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finansio.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/** Akses tabel `budgets`: membaca, menyimpan, dan menghapus anggaran bulanan per kategori. */
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(budget: BudgetEntity)

    // --- Untuk backup & restore ---
    @Query("SELECT * FROM budgets")
    fun getAllOnce(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    fun deleteByCategory(categoryId: Int)
}
