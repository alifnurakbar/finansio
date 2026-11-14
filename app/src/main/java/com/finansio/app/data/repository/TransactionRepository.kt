package com.finansio.app.data.repository

import com.finansio.app.data.local.dao.CategorySummary
import com.finansio.app.data.local.dao.TransactionDao
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Repository transaksi: penghubung ViewModel dengan TransactionDao (daftar, total per periode, total per kategori). */
class TransactionRepository(private val transactionDao: TransactionDao) {
    fun getAll(): Flow<List<TransactionEntity>> = transactionDao.getAll()
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(limit)
    fun getByType(type: TransactionType): Flow<List<TransactionEntity>> = transactionDao.getByType(type)
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>> = transactionDao.getByDateRange(start, end)
    suspend fun getById(id: Int): TransactionEntity? = withContext(Dispatchers.IO) { transactionDao.getById(id) }
    fun getTotalByTypeAndDateRange(type: TransactionType, start: Long, end: Long): Flow<Long?> = transactionDao.getTotalByTypeAndDateRange(type, start, end)
    fun getExpenseByCategoryInRange(start: Long, end: Long): Flow<List<CategorySummary>> = transactionDao.getExpenseByCategoryInRange(TransactionType.EXPENSE, start, end)
    fun getTotalsByCategoryInRange(type: TransactionType, start: Long, end: Long): Flow<List<CategorySummary>> = transactionDao.getExpenseByCategoryInRange(type, start, end)
    suspend fun insert(transaction: TransactionEntity) = withContext(Dispatchers.IO) { transactionDao.insert(transaction) }
    suspend fun update(transaction: TransactionEntity) = withContext(Dispatchers.IO) { transactionDao.update(transaction) }
    suspend fun delete(transaction: TransactionEntity) = withContext(Dispatchers.IO) { transactionDao.delete(transaction) }
}
