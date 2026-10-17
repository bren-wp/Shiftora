package com.brendigo.shiftora.data

import java.time.LocalDate

enum class ShiftKind(val label: String, val start: Int, val end: Int) {
    MORNING("Jutarnja", 360, 840),
    AFTERNOON("Popodnevna", 840, 1320),
    NIGHT("Noćna", 1320, 360),
    CUSTOM("Ostala", 480, 960),
    OFF("Slobodno", 0, 0)
}

data class Shift(
    val id: Long = 0L,
    val date: LocalDate,
    val kind: ShiftKind,
    val startMinute: Int = kind.start,
    val endMinute: Int = kind.end,
    val breakMinutes: Int = 0,
    val breakAfterMinutes: Int = 240,
    val unpaidBreak: Boolean = false,
    val actual: Boolean = false,
    val overtime: Boolean = false,
    val note: String = ""
) {
    init {
        require(startMinute in 0..1439 && endMinute in 0..1439)
        require(breakMinutes in 0..240)
        require(breakAfterMinutes in 0..1440)
        require(kind == ShiftKind.OFF || startMinute != endMinute) {
            "Početak i završetak smjene ne mogu biti jednaki."
        }
    }

    val overnight: Boolean get() = kind != ShiftKind.OFF && endMinute <= startMinute
}

data class SalarySettings(
    val hourlyGross: Double = 8.0,
    val nightPercent: Double = 30.0,
    val sundayPercent: Double = 50.0,
    val holidayPercent: Double = 50.0,
    val overtimePercent: Double = 50.0,
    val nightStartHour: Int = 22,
    val nightEndHour: Int = 6
) {
    init {
        require(hourlyGross.isFinite() && hourlyGross in 0.0..1_000_000.0)
        require(listOf(nightPercent, sundayPercent, holidayPercent, overtimePercent).all { it in 0.0..300.0 })
        require(nightStartHour in 0..23 && nightEndHour in 0..23 && nightStartHour != nightEndHour)
    }
}

data class WorkTotals(
    val paidMinutes: Long = 0,
    val actualMinutes: Long = 0,
    val plannedMinutes: Long = 0,
    val overtimeMinutes: Long = 0,
    val nightMinutes: Long = 0,
    val sundayMinutes: Long = 0,
    val holidayMinutes: Long = 0,
    val morningMinutes: Long = 0,
    val afternoonMinutes: Long = 0,
    val nightShiftMinutes: Long = 0,
    /** Exclusive visual categories: add up to paidMinutes without double-counting overlapping premiums. */
    val chartRegularMinutes: Long = 0,
    val chartOvertimeMinutes: Long = 0,
    val chartNightMinutes: Long = 0,
    val chartSpecialMinutes: Long = 0,
    val grossCents: Long = 0,
    val baseCents: Long = 0,
    val premiumCents: Long = 0
)
