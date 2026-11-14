package com.finansio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finansio.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    fun deleteByCategory(categoryId: Int)
}
