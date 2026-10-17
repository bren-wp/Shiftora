package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyFundTest {
    @Test fun customMonthlyFundOverridesOnlySelectedMonth() {
        val month = YearMonth.of(2026, 10)
        val custom = MonthlyFund.summary(month, emptyList(), SalarySettings(), 160 * 60)
        val standard = MonthlyFund.summary(month, emptyList(), SalarySettings())
        assertEquals(9600, custom.fundMinutes)
        assertEquals(MonthlyFund.standardMinutes(month), standard.fundMinutes)
        assertEquals(9600L, custom.remainingMinutes)
    }

    @Test fun leaveAndStandbyAreNotWorkedHours() {
        val shifts = listOf(
            Shift(date = LocalDate.of(2026, 10, 19), kind = ShiftKind.ANNUAL),
            Shift(date = LocalDate.of(2026, 10, 20), kind = ShiftKind.SICK),
            Shift(date = LocalDate.of(2026, 10, 21), kind = ShiftKind.PAID_LEAVE),
            Shift(date = LocalDate.of(2026, 10, 22), kind = ShiftKind.STANDBY),
            Shift(date = LocalDate.of(2026, 10, 23), kind = ShiftKind.DAY, actual = true)
        )
        val summary = MonthlyFund.summary(YearMonth.of(2026, 10), shifts, SalarySettings(), 9600)
        assertEquals(1440, summary.leaveCreditMinutes)
        assertEquals(720L, summary.actuallyWorkedMinutes)
        assertEquals(720L, summary.recordedMinutes)
        assertEquals(7440L, summary.remainingMinutes)
        val wages = WorkCalculator.calculate(shifts, SalarySettings())
        assertEquals(720L, wages.paidMinutes)
    }

    @Test fun weekendsAndHolidaysDoNotCreditAbsence() {
        val shifts = listOf(
            Shift(date = LocalDate.of(2026, 11, 1), kind = ShiftKind.ANNUAL),
            Shift(date = LocalDate.of(2026, 11, 2), kind = ShiftKind.PAID_LEAVE)
        )
        assertEquals(480, MonthlyFund.summary(YearMonth.of(2026, 11),
            shifts, SalarySettings()).leaveCreditMinutes)
    }
}
