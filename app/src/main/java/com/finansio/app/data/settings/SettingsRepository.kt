package com.finansio.app.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderTime(val hour: Int, val minute: Int) {
    override fun toString() = "%02d.%02d".format(hour, minute)
}

/** Pengaturan pengguna yang disimpan di SharedPreferences. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("finansio_settings", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Pengingat harian: aktif secara bawaan pukul 20.00
    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, true))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(
        ReminderTime(prefs.getInt(KEY_REMINDER_HOUR, 20), prefs.getInt(KEY_REMINDER_MINUTE, 0))
    )
    val reminderTime: StateFlow<ReminderTime> = _reminderTime.asStateFlow()

    // Kunci sidik jari: nonaktif secara bawaan
    private val _lockEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCK_ENABLED, false))
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        _userName.value = trimmed
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        _reminderEnabled.value = enabled
    }

    fun setReminderTime(time: ReminderTime) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, time.hour).putInt(KEY_REMINDER_MINUTE, time.minute).apply()
        _reminderTime.value = time
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _lockEnabled.value = enabled
    }

    private companion object {
        const val KEY_USER_NAME = "user_name"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_LOCK_ENABLED = "lock_enabled"
    }
}
