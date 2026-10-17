package com.brendigo.shiftora.data

import android.content.Context
import java.time.YearMonth

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
        runCatching { store.getString(key, null) }.getOrNull()?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it in 0.0..upper } ?: fallback

    fun salarySettings(): SalarySettings {
        val from = runCatching { store.getInt("night_start_hour", 22) }
            .getOrNull()?.takeIf { it in 0..23 } ?: 22
        val until = runCatching { store.getInt("night_end_hour", 6) }
            .getOrNull()?.takeIf { it in 0..23 } ?: 6
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

    fun shiftColors(): Map<ShiftKind, Int> = ShiftKind.entries.mapNotNull { kind ->
        val key = "color_${kind.name}"
        val value = runCatching {
            if (store.contains(key)) store.getInt(key, 0) else null
        }.getOrNull()
        value?.let { kind to (it or 0xFF000000.toInt()) }
    }.toMap()

    fun saveShiftColor(kind: ShiftKind, argb: Int?) {
        store.edit().apply {
            if (argb == null) remove("color_${kind.name}")
            else putInt("color_${kind.name}", argb or 0xFF000000.toInt())
        }.apply()
    }

    fun replaceShiftColors(colors: Map<ShiftKind, Int>) {
        store.edit().apply {
            ShiftKind.entries.forEach { remove("color_${it.name}") }
            colors.forEach { (kind, argb) ->
                putInt("color_${kind.name}", argb or 0xFF000000.toInt())
            }
        }.commit()
    }

    /** Per-month optional custom fund in minutes (0..744 hours). */
    fun monthlyFundOverrides(): Map<String, Int> =
        store.all.mapNotNull { (key, value) ->
            if (!key.startsWith("fund_") || value !is Int || value !in 0..44640) null
            else {
                val month = key.removePrefix("fund_")
                if (runCatching { YearMonth.parse(month) }.isSuccess) month to value else null
            }
        }.toMap()

    fun replaceMonthlyFunds(funds: Map<String, Int>) {
        store.edit().apply {
            store.all.keys.filter { it.startsWith("fund_") }.forEach { remove(it) }
            funds.forEach { (month, value) -> putInt("fund_$month", value) }
        }.commit()
    }

    fun saveMonthlyFund(month: YearMonth, minutes: Int?) {
        require(minutes == null || minutes in 0..44640)
        store.edit().apply {
            if (minutes == null) remove("fund_$month")
            else putInt("fund_$month", minutes)
        }.apply()
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
