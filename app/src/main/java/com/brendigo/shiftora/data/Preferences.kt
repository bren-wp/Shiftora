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

    private fun number(key: String, fallback: Double): Double =
        store.getString(key, null)?.toDoubleOrNull() ?: fallback

    fun salarySettings(): SalarySettings = SalarySettings(
        hourlyGross = number("hourly", 8.0).coerceAtLeast(0.0),
        nightPercent = number("night", 30.0).coerceIn(0.0, 300.0),
        sundayPercent = number("sunday", 50.0).coerceIn(0.0, 300.0),
        holidayPercent = number("holiday", 50.0).coerceIn(0.0, 300.0),
        overtimePercent = number("overtime", 50.0).coerceIn(0.0, 300.0)
    )

    fun saveSalary(settings: SalarySettings) {
        store.edit()
            .putString("hourly", settings.hourlyGross.toString())
            .putString("night", settings.nightPercent.toString())
            .putString("sunday", settings.sundayPercent.toString())
            .putString("holiday", settings.holidayPercent.toString())
            .putString("overtime", settings.overtimePercent.toString())
            .apply()
    }
}
