package com.brendigo.shiftora.data

import java.time.LocalDate

/**
 * Persisted by enum name: retain legacy names for existing installations and JSON backups.
 * Non-working statuses never contribute minutes to worked-time or gross-pay estimates.
 */
enum class ShiftKind(
    val label: String, val code: String, val start: Int, val end: Int,
    val isWork: Boolean = true, val isAbsence: Boolean = false
) {
    MORNING("Jutarnja", "J", 360, 840),
    AFTERNOON("Popodnevna", "P", 840, 1320),
    NIGHT("Noćna", "N", 1320, 360),
    CUSTOM("Prilagođena", "X", 480, 960),
    OFF("Slobodan dan", "SD", 0, 0, isWork = false),
    DAY("Dnevna", "D", 420, 1140),
    STANDBY("Pripravnost", "PR", 0, 0, isWork = false),
    ANNUAL("Godišnji odmor", "GO", 0, 0, isWork = false, isAbsence = true),
    SICK("Bolovanje", "BO", 0, 0, isWork = false, isAbsence = true),
    PAID_LEAVE("Plaćeni dopust", "PD", 0, 0, isWork = false, isAbsence = true);

    companion object {
        /** UI order is deliberately independent from persisted enum values. */
        val calendarOrder = listOf(DAY, NIGHT, MORNING, AFTERNOON, STANDBY,
            ANNUAL, SICK, OFF, PAID_LEAVE, CUSTOM)
    }
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
        require(!kind.isWork || startMinute != endMinute) {
            "Početak i završetak smjene ne mogu biti jednaki."
        }
    }

    val overnight: Boolean get() = kind.isWork && endMinute <= startMinute
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
    /** Unique minutes on a Sunday or public holiday, counted once even if both apply. */
    val specialMinutes: Long = 0,
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
