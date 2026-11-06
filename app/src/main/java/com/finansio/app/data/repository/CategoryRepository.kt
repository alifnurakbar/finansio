package com.finansio.app.data.repository

import com.finansio.app.data.local.dao.CategoryDao
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()
    fun getByType(type: TransactionType): Flow<List<CategoryEntity>> = categoryDao.getByType(type)
    suspend fun insert(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.insert(category) }
    suspend fun update(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.update(category) }
    suspend fun delete(category: CategoryEntity) = withContext(Dispatchers.IO) { categoryDao.delete(category) }
}
