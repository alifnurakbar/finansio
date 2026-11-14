package com.finansio.app.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pengaturan pengguna yang disimpan di SharedPreferences. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("finansio_settings", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString(KEY_USER_NAME, trimmed).apply()
        _userName.value = trimmed
    }

    private companion object {
        const val KEY_USER_NAME = "user_name"
    }
}
