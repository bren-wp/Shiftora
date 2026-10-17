package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.WorkTotals
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.LocalDate

/** One-minute accounting avoids rounding error, handles local DST and overlapping premiums. */
object WorkCalculator {
    fun durationMinutes(shift: Shift, zone: ZoneId = ZoneId.systemDefault()): Long {
        if (shift.kind == ShiftKind.OFF) return 0L
        val start = shift.date.atStartOfDay().plusMinutes(shift.startMinute.toLong()).atZone(zone)
        val endDate = if (shift.overnight) shift.date.plusDays(1) else shift.date
        val end = endDate.atStartOfDay().plusMinutes(shift.endMinute.toLong()).atZone(zone)
        return Duration.between(start.toInstant(), end.toInstant()).toMinutes().coerceAtLeast(0)
    }

    /**
     * Aggregate all stored time, regardless of calendar boundary.
     * The report-specific overload below must be used for month/week/year summaries.
     */
    fun calculate(
        shifts: List<Shift>,
        settings: SalarySettings,
        zone: ZoneId = ZoneId.systemDefault()
    ): WorkTotals = calculateInternal(shifts, settings, zone, null, null)

    /**
     * Allocates every actual worked minute to the LOCAL calendar day on which it occurred.
     * This prevents a night shift on the last day of a month from being paid entirely
     * in the old month. Input must contain ALL shifts, including the prior day's shift.
     */
    fun calculatePeriod(
        shifts: List<Shift>,
        settings: SalarySettings,
        startInclusive: LocalDate,
        endExclusive: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): WorkTotals {
        require(endExclusive.isAfter(startInclusive)) { "Empty or reversed reporting period" }
        return calculateInternal(shifts, settings, zone, startInclusive, endExclusive)
    }

    private fun calculateInternal(
        shifts: List<Shift>,
        settings: SalarySettings,
        zone: ZoneId,
        startInclusive: LocalDate?,
        endExclusive: LocalDate?
    ): WorkTotals {
        var paid = 0L
        var actual = 0L
        var planned = 0L
        var night = 0L
        var sunday = 0L
        var holiday = 0L
        var overtime = 0L
        var morning = 0L
        var afternoon = 0L
        var nightShifts = 0L
        var chartRegular = 0L
        var chartOvertime = 0L
        var chartNight = 0L
        var chartSpecial = 0L
        for (shift in shifts) {
            if (shift.kind == ShiftKind.OFF) continue
            // A shift spans at most two local dates; skip irrelevant periods early.
            if (startInclusive != null && shift.date.isBefore(startInclusive.minusDays(1))) continue
            if (endExclusive != null && !shift.date.isBefore(endExclusive)) continue
            val start = shift.date.atStartOfDay().plusMinutes(shift.startMinute.toLong()).atZone(zone)
            val total = durationMinutes(shift, zone)
            val breakEnd = (shift.breakAfterMinutes.toLong() + shift.breakMinutes).coerceAtMost(total)
            for (minute in 0 until total) {
                if (shift.unpaidBreak && minute >= shift.breakAfterMinutes && minute < breakEnd) continue
                val local = start.toInstant().plusSeconds(minute * 60).atZone(zone)
                val date = local.toLocalDate()
                if (startInclusive != null && date.isBefore(startInclusive)) continue
                if (endExclusive != null && !date.isBefore(endExclusive)) continue
                val hour = local.hour
                paid++
                if (shift.actual) actual++ else planned++
                val isNight = isNightHour(hour, settings.nightStartHour, settings.nightEndHour)
                val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
                val isHoliday = CroatianHolidays.isHoliday(date)
                if (shift.overtime) overtime++
                if (isNight) night++
                if (isSunday) sunday++
                if (isHoliday) holiday++
                // Each paid minute appears in exactly one donut segment.
                // Actual premium counters above intentionally overlap when legally applicable.
                when {
                    shift.overtime -> chartOvertime++
                    isSunday || isHoliday -> chartSpecial++
                    isNight -> chartNight++
                    else -> chartRegular++
                }
                when (shift.kind) {
                    ShiftKind.MORNING -> morning++
                    ShiftKind.AFTERNOON -> afternoon++
                    ShiftKind.NIGHT -> nightShifts++
                    else -> Unit
                }
            }
        }
        val hourly = BigDecimal.valueOf(settings.hourlyGross)
        fun cents(minutes: Long, percent: Double = 100.0): Long =
            hourly.multiply(BigDecimal.valueOf(minutes))
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal("60"), 8, RoundingMode.HALF_UP)
                .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP).longValueExact()

        val base = cents(paid)
        val premiums = cents(night, settings.nightPercent) +
            cents(sunday, settings.sundayPercent) +
            cents(holiday, settings.holidayPercent) +
            cents(overtime, settings.overtimePercent)
        return WorkTotals(
            paidMinutes = paid, actualMinutes = actual, plannedMinutes = planned,
            overtimeMinutes = overtime, nightMinutes = night,
            sundayMinutes = sunday, holidayMinutes = holiday,
            morningMinutes = morning, afternoonMinutes = afternoon,
            nightShiftMinutes = nightShifts,
            chartRegularMinutes = chartRegular,
            chartOvertimeMinutes = chartOvertime,
            chartNightMinutes = chartNight,
            chartSpecialMinutes = chartSpecial,
            grossCents = base + premiums, baseCents = base, premiumCents = premiums
        )
    }

    fun isNightHour(hour: Int, start: Int, end: Int): Boolean =
        if (start == end) true else if (start < end) hour in start until end
        else hour >= start || hour < end
}
