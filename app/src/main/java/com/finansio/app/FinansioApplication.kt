package com.finansio.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.finansio.app.data.budget.BudgetAlertManager
import com.finansio.app.data.local.AppDatabase
import com.finansio.app.data.remote.RetrofitClient
import com.finansio.app.data.repository.AiRepository
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.data.repository.CategoryRepository
import com.finansio.app.data.repository.TransactionRepository
import com.finansio.app.data.settings.SettingsRepository

/** Titik awal aplikasi. Membuat database, repository, dan manager satu kali (manual dependency injection), lalu menjalankan pemantau peringatan anggaran dan menjadwalkan pengingat harian. */
class FinansioApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    // Log HTTP lengkap hanya untuk build debug. APK release tidak debuggable, jadi lognya mati.
    private val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val aiRepository by lazy { AiRepository(RetrofitClient.createGroqApiService(httpLogging = isDebuggable)) }
    val budgetRepository by lazy {
        BudgetRepository(database.budgetDao(), database.categoryDao(), database.transactionDao())
    }
    val settingsRepository by lazy { SettingsRepository(this) }
    val budgetAlertManager by lazy { BudgetAlertManager(this, budgetRepository, aiRepository) }
    val reportExporter by lazy {
        com.finansio.app.data.export.ReportExporter(this, transactionRepository, budgetRepository)
    }
    val backupManager by lazy { com.finansio.app.data.backup.BackupManager(this, database, settingsRepository) }

    override fun onCreate() {
        super.onCreate()
        budgetAlertManager.start()
        com.finansio.app.data.reminder.DailyReminder.createChannel(this)
        com.finansio.app.data.reminder.DailyReminder.reschedule(this)
    }
}
