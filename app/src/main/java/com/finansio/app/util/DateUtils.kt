package com.finansio.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pembantu tanggal: awal/akhir bulan dan minggu, serta format tanggal Bahasa Indonesia. */
object DateUtils {
    val localeID = Locale("in", "ID")

    fun getStartOfMonth(): Long = getStartOfMonth(
        Calendar.getInstance(localeID).get(Calendar.YEAR),
        Calendar.getInstance(localeID).get(Calendar.MONTH)
    )

    fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance(localeID)
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfMonth(): Long = getEndOfMonth(
        Calendar.getInstance(localeID).get(Calendar.YEAR),
        Calendar.getInstance(localeID).get(Calendar.MONTH)
    )

    fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance(localeID)
        calendar.set(year, month, 1, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return calendar.timeInMillis
    }

    fun formatMonthYear(year: Int, month: Int): String {
        val calendar = Calendar.getInstance(localeID)
        calendar.set(year, month, 1)
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", localeID)
        return sdf.format(calendar.time)
    }

    fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance(localeID)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfWeek(): Long {
        val calendar = Calendar.getInstance(localeID)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun formatDisplayDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", localeID)
        return sdf.format(Date(timestamp))
    }

    fun getMonthYearLabel(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", localeID)
        return sdf.format(Date())
    }

    fun getMonthLabel(monthOffset: Int): String {
        val calendar = Calendar.getInstance(localeID)
        calendar.add(Calendar.MONTH, monthOffset)
        val sdf = SimpleDateFormat("MMMM yyyy", localeID)
        return sdf.format(calendar.time)
    }
}
