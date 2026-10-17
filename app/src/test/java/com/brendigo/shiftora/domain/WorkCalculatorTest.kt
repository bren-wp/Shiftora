package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkCalculatorTest {
    private val zagreb = ZoneId.of("Europe/Zagreb")

    @Test fun morningShiftHasEightHours() {
        val summary = WorkCalculator.calculate(
            listOf(Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING)),
            SalarySettings(hourlyGross = 10.0), zagreb
        )
        assertEquals(480L, summary.paidMinutes)
        assertEquals(8000L, summary.grossCents)
        assertEquals(0L, summary.nightMinutes)
    }

    @Test fun unpaidBreakIsRemoved() {
        val s = Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING, unpaidBreak = true, breakMinutes = 30)
        val result = WorkCalculator.calculate(listOf(s), SalarySettings(hourlyGross = 10.0), zagreb)
        assertEquals(450L, result.paidMinutes)
        assertEquals(7500L, result.grossCents)
    }

    @Test fun sundayNightPremiumsMayOverlapWithoutDuplicatingPaidHours() {
        val s = Shift(date = LocalDate.of(2026, 10, 18), kind = ShiftKind.NIGHT, actual = true)
        val result = WorkCalculator.calculate(listOf(s), SalarySettings(hourlyGross = 10.0, nightPercent = 30.0, sundayPercent = 50.0, holidayPercent = 0.0), zagreb)
        assertEquals(480L, result.paidMinutes)
        assertEquals(480L, result.nightMinutes)
        assertEquals(120L, result.sundayMinutes)
        assertEquals(0L, result.plannedMinutes)
        assertEquals(8_000L + 2_400L + 1_000L, result.grossCents)
    }

    @Test fun transitionToSummerTimeHasSevenElapsedHours() {
        val s = Shift(date = LocalDate.of(2026, 3, 28), kind = ShiftKind.NIGHT)
        assertEquals(420L, WorkCalculator.durationMinutes(s, zagreb))
    }

    @Test fun transitionToWinterTimeHasNineElapsedHours() {
        val s = Shift(date = LocalDate.of(2026, 10, 24), kind = ShiftKind.NIGHT)
        assertEquals(540L, WorkCalculator.durationMinutes(s, zagreb))
    }

    @Test fun movableHolidaysAreCorrect() {
        assertEquals(LocalDate.of(2026, 4, 5), CroatianHolidays.easter(2026))
        assertTrue(CroatianHolidays.isHoliday(LocalDate.of(2026, 6, 4)))
    }

    @Test fun nightShiftIsSplitCorrectlyAcrossMonthlyBoundary() {
        val entry = Shift(date = LocalDate.of(2026, 10, 31), kind = ShiftKind.NIGHT)
        val settings = SalarySettings(
            hourlyGross = 10.0, nightPercent = 0.0,
            sundayPercent = 50.0, holidayPercent = 30.0, overtimePercent = 0.0
        )
        val all = listOf(entry)
        val october = WorkCalculator.calculatePeriod(
            all, settings, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), zagreb
        )
        val november = WorkCalculator.calculatePeriod(
            all, settings, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), zagreb
        )
        assertEquals(120L, october.paidMinutes)
        assertEquals(720L, october.sundayMinutes + november.sundayMinutes + november.holidayMinutes)
        assertEquals(0L, october.holidayMinutes)
        assertEquals(360L, november.paidMinutes)
        assertEquals(360L, november.sundayMinutes)
        assertEquals(360L, november.holidayMinutes)
        assertEquals(2000L, october.grossCents)
        assertEquals(6000L + 3000L + 1800L, november.grossCents)
        assertEquals(480L, october.paidMinutes + november.paidMinutes)
    }

    @Test fun priorDayShiftIsIncludedWhenWeekStarts() {
        val sunday = Shift(date = LocalDate.of(2026, 10, 18), kind = ShiftKind.NIGHT)
        val monday = LocalDate.of(2026, 10, 19)
        val week = WorkCalculator.calculatePeriod(
            listOf(sunday), SalarySettings(hourlyGross = 10.0, nightPercent = 0.0,
                sundayPercent = 0.0, holidayPercent = 0.0, overtimePercent = 0.0),
            monday, monday.plusDays(7), zagreb
        )
        assertEquals(360L, week.paidMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsReversedReportingRange() {
        WorkCalculator.calculatePeriod(emptyList(), SalarySettings(),
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), zagreb)
    }
    @Test fun chartIsExclusiveEvenWhenPremiumsOverlap() {
        val shift = Shift(date = LocalDate.of(2026, 10, 18), kind = ShiftKind.NIGHT,
            actual = true, overtime = false)
        val totals = WorkCalculator.calculate(listOf(shift),
            SalarySettings(hourlyGross = 10.0), zagreb)
        assertEquals(480L, totals.paidMinutes)
        assertEquals(480L, totals.nightMinutes)
        assertEquals(120L, totals.sundayMinutes)
        assertEquals(120L, totals.chartSpecialMinutes)
        assertEquals(360L, totals.chartNightMinutes)
        assertEquals(0L, totals.chartRegularMinutes)
        assertEquals(totals.paidMinutes,
            totals.chartRegularMinutes + totals.chartOvertimeMinutes +
                totals.chartNightMinutes + totals.chartSpecialMinutes)
    }

    @Test fun overtimeHasPriorityInDonutButNightPremiumStillApplies() {
        val shift = Shift(date = LocalDate.of(2026, 10, 18), kind = ShiftKind.NIGHT,
            overtime = true)
        val totals = WorkCalculator.calculate(listOf(shift), SalarySettings(), zagreb)
        assertEquals(480L, totals.chartOvertimeMinutes)
        assertEquals(0L, totals.chartNightMinutes)
        assertEquals(480L, totals.nightMinutes)
        assertEquals(120L, totals.sundayMinutes)
    }

    @Test fun regularWeekdayIsAllRegularInDonut() {
        val summary = WorkCalculator.calculate(
            listOf(Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING)),
            SalarySettings(), zagreb)
        assertEquals(480L, summary.chartRegularMinutes)
        assertEquals(0L, summary.chartOvertimeMinutes + summary.chartNightMinutes + summary.chartSpecialMinutes)
    }

    @Test fun chartRespectsUnpaidBreaks() {
        val shift = Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING,
            breakMinutes = 30, unpaidBreak = true)
        val totals = WorkCalculator.calculate(listOf(shift), SalarySettings(), zagreb)
        assertEquals(450L, totals.chartRegularMinutes)
        assertEquals(totals.paidMinutes, totals.chartRegularMinutes)
    }

}
