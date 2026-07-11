package com.example.neusoft_hospital.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateExt {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val weekDayFormat = SimpleDateFormat("EEEE", Locale.CHINA)

    fun today() = dateFormat.format(Date())
    fun nowTime() = timeFormat.format(Date())
    fun nowDateTime() = dateTimeFormat.format(Date())
    fun weekDay(date: String = today()) = try {
        weekDayFormat.format(dateFormat.parse(date)!!)
    } catch (e: Exception) { "" }

    fun formatTime(hour: Int, minute: Int) = String.format(Locale.CHINA, "%02d:%02d", hour, minute)

    fun addDays(date: String, days: Int): String {
        return try {
            val d = dateFormat.parse(date)!!
            val cal = java.util.Calendar.getInstance().apply { time = d; add(java.util.Calendar.DAY_OF_MONTH, days) }
            dateFormat.format(cal.time)
        } catch (e: Exception) { date }
    }

    fun isBeforeOrEqual(a: String, b: String): Boolean {
        return try { dateFormat.parse(a)!! <= dateFormat.parse(b)!! } catch (e: Exception) { false }
    }
}
