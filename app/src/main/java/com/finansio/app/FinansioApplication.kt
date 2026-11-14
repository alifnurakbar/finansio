package com.finansio.app

import android.app.Application
import com.finansio.app.data.budget.BudgetAlertManager
import com.finansio.app.data.local.AppDatabase
import com.finansio.app.data.remote.RetrofitClient
import com.finansio.app.data.repository.AiRepository
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.data.settings.SettingsRepository

class FinansioApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val aiRepository by lazy { AiRepository(RetrofitClient.groqApiService) }
    val budgetRepository by lazy {
        BudgetRepository(database.budgetDao(), database.categoryDao(), database.transactionDao())
    }
    val settingsRepository by lazy { SettingsRepository(this) }
    val budgetAlertManager by lazy { BudgetAlertManager(this, budgetRepository, aiRepository) }

    override fun onCreate() {
        super.onCreate()
        budgetAlertManager.start()
    }
}
