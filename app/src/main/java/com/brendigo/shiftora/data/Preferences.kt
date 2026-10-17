package com.brendigo.shiftora.data

import android.content.Context

class Preferences(context: Context) {
    private val store = context.getSharedPreferences("shiftora_settings", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = store.getBoolean("onboarding_done", false)
        set(value) { store.edit().putBoolean("onboarding_done", value).apply() }

    var darkMode: Boolean
        get() = store.getBoolean("dark_mode", false)
        set(value) { store.edit().putBoolean("dark_mode", value).apply() }

    /** Corrupt, non-finite or out-of-range saved values must never crash startup. */
    private fun number(key: String, fallback: Double, upper: Double): Double =
        store.getString(key, null)?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 0.0..upper } ?: fallback

    fun salarySettings(): SalarySettings {
        val from = store.getInt("night_start_hour", 22).takeIf { it in 0..23 } ?: 22
        val until = store.getInt("night_end_hour", 6).takeIf { it in 0..23 } ?: 6
        return SalarySettings(
            hourlyGross = number("hourly", 8.0, 1_000_000.0),
            nightPercent = number("night", 30.0, 300.0),
            sundayPercent = number("sunday", 50.0, 300.0),
            holidayPercent = number("holiday", 50.0, 300.0),
            overtimePercent = number("overtime", 50.0, 300.0),
            nightStartHour = if (from == until) 22 else from,
            nightEndHour = if (from == until) 6 else until
        )
    }

    fun saveSalary(settings: SalarySettings) {
        store.edit()
            .putString("hourly", settings.hourlyGross.toString())
            .putString("night", settings.nightPercent.toString())
            .putString("sunday", settings.sundayPercent.toString())
            .putString("holiday", settings.holidayPercent.toString())
            .putString("overtime", settings.overtimePercent.toString())
            .putInt("night_start_hour", settings.nightStartHour)
            .putInt("night_end_hour", settings.nightEndHour)
            .apply()
    }
}
