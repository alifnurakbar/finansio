package com.finansio.app.data.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.finansio.app.FinansioApplication
import com.finansio.app.MainActivity
import com.finansio.app.R
import com.finansio.app.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Pengingat harian untuk mencatat transaksi. Memakai AlarmManager (tanpa library tambahan):
 * setiap kali alarm berbunyi, alarm berikutnya dijadwalkan untuk besok di jam yang sama.
 */
object DailyReminder {
    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 2000
    private const val REQUEST_CODE = 2001

    /** Jadwalkan ulang (atau batalkan) sesuai pengaturan saat ini. */
    fun reschedule(context: Context) {
        val app = context.applicationContext as FinansioApplication
        val settings = app.settingsRepository
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = alarmIntent(context)
        alarmManager.cancel(pendingIntent)
        if (!settings.reminderEnabled.value) return

        val time = settings.reminderTime.value
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        // Tidak butuh izin "exact alarm"; Android boleh menunda beberapa menit untuk hemat baterai
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Pengingat Harian", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Pengingat untuk mencatat transaksi setiap hari" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    internal fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val openApp = PendingIntent.getActivity(
            context, NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wallet)
            .setContentTitle("Sudah catat pengeluaran hari ini?")
            .setContentText("Luangkan 1 menit untuk mencatat transaksimu di Finansio.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) { }
    }
}

/** Dipanggil AlarmManager pada jam pengingat. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as FinansioApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Lewati pengingat jika hari ini sudah ada transaksi yang dicatat
                val todayStart = Calendar.getInstance(DateUtils.localeID).apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val todayEnd = todayStart + 24L * 60 * 60 * 1000 - 1
                val hasTransactionToday = app.transactionRepository.getByDateRange(todayStart, todayEnd).first().isNotEmpty()
                if (!hasTransactionToday) DailyReminder.showNotification(context)
            } finally {
                DailyReminder.reschedule(context) // jadwalkan untuk besok
                pending.finish()
            }
        }
    }
}

/** Alarm hilang saat HP dinyalakan ulang, jadi jadwalkan kembali setelah booting. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) DailyReminder.reschedule(context)
    }
}
