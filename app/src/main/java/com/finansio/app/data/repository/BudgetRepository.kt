package com.finansio.app.data.repository

import com.finansio.app.data.local.dao.BudgetDao
import com.finansio.app.data.local.dao.CategoryDao
import com.finansio.app.data.local.dao.TransactionDao
import com.finansio.app.data.local.entity.BudgetEntity
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionType
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/** Status satu anggaran pada bulan tertentu. */
data class BudgetProgress(
    val category: CategoryEntity,
    val limit: Long,
    val spent: Long
) {
    /** Persentase terpakai (bisa lebih dari 100). */
    val percent: Int get() = if (limit <= 0) 0 else ((spent * 100) / limit).toInt()
    val remaining: Long get() = limit - spent
    val level: BudgetLevel
        get() = when {
            percent >= 100 -> BudgetLevel.OVER
            percent >= 80 -> BudgetLevel.WARNING
            else -> BudgetLevel.SAFE
        }
}

/** Hijau < 80%, kuning 80-99%, merah >= 100%. */
enum class BudgetLevel(val threshold: Int) { SAFE(0), WARNING(80), OVER(100) }

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {
    fun progressForMonth(year: Int, month: Int): Flow<List<BudgetProgress>> = combine(
        budgetDao.getAll(),
        categoryDao.getByType(TransactionType.EXPENSE),
        transactionDao.getExpenseTotalsByCategoryId(
            DateUtils.getStartOfMonth(year, month),
            DateUtils.getEndOfMonth(year, month)
        )
    ) { budgets, categories, totals ->
        val spentByCategory = totals.associate { it.categoryId to it.total }
        budgets.mapNotNull { budget ->
            // Anggaran milik kategori yang sudah dihapus diabaikan
            val category = categories.find { it.id == budget.categoryId } ?: return@mapNotNull null
            BudgetProgress(category, budget.monthlyLimit, spentByCategory[category.id] ?: 0L)
        }.sortedByDescending { it.percent }
    }

    suspend fun setBudget(categoryId: Int, monthlyLimit: Long) = withContext(Dispatchers.IO) {
        budgetDao.upsert(BudgetEntity(categoryId, monthlyLimit))
    }

    suspend fun deleteBudget(categoryId: Int) = withContext(Dispatchers.IO) {
        budgetDao.deleteByCategory(categoryId)
    }
}
