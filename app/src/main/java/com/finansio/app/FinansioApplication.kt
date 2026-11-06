package com.finansio.app

import android.app.Application
import com.finansio.app.data.local.AppDatabase
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository

class FinansioApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val aiRepository by lazy { com.finansio.app.data.repository.AiRepository(com.finansio.app.data.remote.RetrofitClient.groqApiService) }
}
