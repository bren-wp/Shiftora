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
}
