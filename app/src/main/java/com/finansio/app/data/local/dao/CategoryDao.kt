package com.finansio.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

/** Akses tabel `categories`: membaca, menambah, mengubah, dan menghapus kategori pemasukan/pengeluaran. */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: CategoryEntity)

    // --- Untuk backup & restore ---
    @Query("SELECT * FROM categories")
    fun getAllOnce(): List<CategoryEntity>

    @Query("DELETE FROM categories")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(categories: List<CategoryEntity>)

    @Update
    fun update(category: CategoryEntity)

    @Delete
    fun delete(category: CategoryEntity)
}
