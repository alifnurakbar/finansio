package com.finansio.app.data.budget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.finansio.app.MainActivity
import com.finansio.app.R
import com.finansio.app.data.repository.AiRepository
import com.finansio.app.data.repository.BudgetLevel
import com.finansio.app.data.repository.BudgetProgress
import com.finansio.app.data.repository.BudgetRepository
import com.finansio.app.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/** Peringatan anggaran yang ditampilkan sebagai banner di Home. */
data class BudgetAlert(
    val categoryId: Int,
    val categoryName: String,
    val percent: Int,
    val message: String,
    val isOver: Boolean
)

/**
 * Memantau anggaran bulan berjalan. Saat pengeluaran sebuah kategori pertama kali mencapai
 * 80% atau 100% dari batasnya, buat saran dengan AI (atau teks biasa bila AI gagal),
 * lalu tampilkan sebagai banner di Home dan notifikasi HP. Setiap level hanya diperingatkan
 * sekali per kategori per bulan.
 */
class BudgetAlertManager(
    private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val aiRepository: AiRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("finansio_budget_alerts", Context.MODE_PRIVATE)

    private val _alerts = MutableStateFlow(loadSavedAlerts())
    val alerts: StateFlow<List<BudgetAlert>> = _alerts.asStateFlow()

    fun start() {
        createNotificationChannel()
        val now = Calendar.getInstance()
        scope.launch {
            budgetRepository.progressForMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
                .collect { list -> list.forEach(::checkThreshold) }
        }
    }

    fun dismiss(alert: BudgetAlert) {
        prefs.edit().putBoolean(dismissedKey(alert.categoryId), true).apply()
        _alerts.update { current -> current.filterNot { it.categoryId == alert.categoryId } }
    }

    private fun checkThreshold(progress: BudgetProgress) {
        val level = progress.level
        if (level == BudgetLevel.SAFE) return
        val levelKey = levelKey(progress.category.id)
        // Sudah pernah diperingatkan untuk level ini (atau lebih tinggi) bulan ini
        if (prefs.getInt(levelKey, 0) >= level.threshold) return
        prefs.edit()
            .putInt(levelKey, level.threshold)
            .putBoolean(dismissedKey(progress.category.id), false)
            .apply()
        scope.launch { createAlert(progress) }
    }

    private suspend fun createAlert(progress: BudgetProgress) {
        val name = progress.category.name
        val isOver = progress.level == BudgetLevel.OVER
        val fallback = if (isOver) {
            "Pengeluaran $name sudah ${progress.percent}% dan melewati anggaran " +
                "${CurrencyFormatter.formatRupiah(progress.limit)}. Coba tahan pengeluaran $name sampai akhir bulan."
        } else {
            "Pengeluaran $name sudah ${progress.percent}%. Sisa anggaran " +
                "${CurrencyFormatter.formatRupiah(progress.remaining)} untuk ${daysLeftInMonth()} hari ke depan."
        }

        val advice = withTimeoutOrNull(30_000) {
            aiRepository.generateBudgetAdvice(
                categoryName = name,
                spent = progress.spent,
                limit = progress.limit,
                percent = progress.percent,
                daysLeft = daysLeftInMonth()
            ).getOrNull()
        }

        val message = advice ?: fallback
        val alert = BudgetAlert(progress.category.id, name, progress.percent, message, isOver)
        prefs.edit()
            .putString(messageKey(alert.categoryId), message)
            .putString(nameKey(alert.categoryId), name)
            .putInt(percentKey(alert.categoryId), progress.percent)
            .apply()

        _alerts.update { current -> current.filterNot { it.categoryId == alert.categoryId } + alert }
        showNotification(alert)
    }

    private fun loadSavedAlerts(): List<BudgetAlert> {
        val suffix = "_${monthKey()}"
        return prefs.all.keys
            .filter { it.startsWith("msg_") && it.endsWith(suffix) }
            .mapNotNull { key ->
                val categoryId = key.removePrefix("msg_").removeSuffix(suffix).toIntOrNull()
                    ?: return@mapNotNull null
                if (prefs.getBoolean(dismissedKey(categoryId), false)) return@mapNotNull null
                val percent = prefs.getInt(percentKey(categoryId), 0)
                BudgetAlert(
                    categoryId = categoryId,
                    categoryName = prefs.getString(nameKey(categoryId), "") ?: "",
                    percent = percent,
                    message = prefs.getString(key, "") ?: "",
                    isOver = percent >= 100
                )
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan Anggaran",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Pemberitahuan saat pengeluaran mendekati atau melewati anggaran" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showNotification(alert: BudgetAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alert.categoryId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (alert.isOver) "Anggaran ${alert.categoryName} terlampaui (${alert.percent}%)"
        else "Anggaran ${alert.categoryName} sudah ${alert.percent}%"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wallet)
            .setContentTitle(title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_ID + alert.categoryId, notification)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut pengguna; banner di Home tetap tampil
        }
    }

    private fun daysLeftInMonth(): Int {
        val cal = Calendar.getInstance()
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1
    }

    private fun monthKey(): String {
        val cal = Calendar.getInstance()
        return "%04d%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    private fun levelKey(categoryId: Int) = "level_${categoryId}_${monthKey()}"
    private fun dismissedKey(categoryId: Int) = "dismissed_${categoryId}_${monthKey()}"
    private fun messageKey(categoryId: Int) = "msg_${categoryId}_${monthKey()}"
    private fun nameKey(categoryId: Int) = "name_${categoryId}_${monthKey()}"
    private fun percentKey(categoryId: Int) = "pct_${categoryId}_${monthKey()}"

    private companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val NOTIFICATION_BASE_ID = 1000
    }
}
