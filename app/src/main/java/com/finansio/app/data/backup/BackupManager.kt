package com.finansio.app.data.backup

import android.content.Context
import android.net.Uri
import com.finansio.app.data.local.AppDatabase
import com.finansio.app.data.local.entity.BudgetEntity
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.local.entity.TransactionEntity
import com.finansio.app.data.settings.SettingsRepository
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Isi file backup Finansio (format JSON). */
data class BackupFile(
    val app: String = APP_ID,
    val formatVersion: Int = FORMAT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val userName: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList()
) {
    companion object {
        const val APP_ID = "Finansio"
        const val FORMAT_VERSION = 1
    }
}

/**
 * Backup = semua kategori, transaksi, anggaran, dan nama pengguna ditulis ke satu file JSON
 * yang lokasinya dipilih pengguna (memori HP atau Google Drive lewat pemilih file Android).
 * Restore = MENGGANTI seluruh data dengan isi file, dalam satu transaksi database.
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun backupTo(uri: Uri): BackupFile = withContext(Dispatchers.IO) {
        val backup = BackupFile(
            userName = settingsRepository.userName.value,
            categories = database.categoryDao().getAllOnce(),
            transactions = database.transactionDao().getAllOnce(),
            budgets = database.budgetDao().getAllOnce()
        )
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            gson.toJson(backup, it)
        } ?: throw IllegalStateException("Tidak bisa menulis file backup.")
        backup
    }

    /** Baca dan periksa file tanpa mengubah data apa pun. */
    suspend fun readBackup(uri: Uri): BackupFile = withContext(Dispatchers.IO) {
        val backup = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                gson.fromJson(it, BackupFile::class.java)
            }
        } catch (e: JsonParseException) {
            null
        } ?: throw IllegalArgumentException("File ini bukan file backup Finansio.")
        if (backup.app != BackupFile.APP_ID) throw IllegalArgumentException("File ini bukan file backup Finansio.")
        if (backup.formatVersion > BackupFile.FORMAT_VERSION) {
            throw IllegalArgumentException("File backup dibuat oleh versi Finansio yang lebih baru. Perbarui aplikasi dulu.")
        }
        backup
    }

    /** Ganti seluruh data dengan isi backup. Semua atau tidak sama sekali. */
    suspend fun restore(backup: BackupFile) = withContext(Dispatchers.IO) {
        database.runInTransaction {
            database.budgetDao().deleteAll()
            database.transactionDao().deleteAll()
            database.categoryDao().deleteAll()
            database.categoryDao().insertAll(backup.categories.orEmpty())
            database.transactionDao().insertAll(backup.transactions.orEmpty())
            database.budgetDao().insertAll(backup.budgets.orEmpty())
        }
        backup.userName?.let { settingsRepository.setUserName(it) }
    }
}
