package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ShiftScheduleValidatorTest {
    private val zagreb = ZoneId.of("Europe/Zagreb")

    @Test fun contiguousShiftsAreAllowed() {
        val day = LocalDate.of(2026, 10, 17)
        val first = Shift(date = day, kind = ShiftKind.MORNING)
        val second = Shift(date = day, kind = ShiftKind.AFTERNOON)
        assertFalse(ShiftScheduleValidator.overlaps(first, second, zagreb))
        ShiftScheduleValidator.validateAll(listOf(first, second), zagreb)
    }

    @Test fun duplicateAndPartialOverlapRejected() {
        val day = LocalDate.of(2026, 10, 17)
        val first = Shift(date = day, kind = ShiftKind.MORNING)
        val repeated = first.copy(id = 0L)
        assertTrue(ShiftScheduleValidator.overlaps(first, repeated, zagreb))
        try {
            ShiftScheduleValidator.validateInsertion(listOf(first.copy(id = 11)), repeated, zagreb)
            fail("Overlapping shifts accepted")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun detectsOverlapAcrossMidnight() {
        val saturday = LocalDate.of(2026, 10, 17)
        val first = Shift(date = saturday, kind = ShiftKind.NIGHT)
        val second = Shift(date = saturday.plusDays(1), kind = ShiftKind.CUSTOM, startMinute = 300, endMinute = 720)
        assertTrue(ShiftScheduleValidator.overlaps(first, second, zagreb))
    }

    @Test fun dayOffDoesNotCountAsWorkingInterval() {
        val day = LocalDate.of(2026, 10, 17)
        assertFalse(ShiftScheduleValidator.overlaps(
            Shift(date = day, kind = ShiftKind.OFF),
            Shift(date = day, kind = ShiftKind.MORNING), zagreb
        ))
    }

    @Test fun editingTheSameEntryIsAllowed() {
        val day = LocalDate.of(2026, 10, 17)
        val existing = Shift(id = 55, date = day, kind = ShiftKind.MORNING)
        ShiftScheduleValidator.validateInsertion(listOf(existing), existing.copy(note = "Promjena"), zagreb)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsBreakLongerThanShift() {
        val entry = Shift(date = LocalDate.of(2026, 10, 17), kind = ShiftKind.MORNING,
            breakAfterMinutes = 470, breakMinutes = 30)
        ShiftScheduleValidator.validateBreak(entry, zagreb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEqualStartAndEnd() {
        Shift(date = LocalDate.of(2026, 10, 17), kind = ShiftKind.CUSTOM,
            startMinute = 480, endMinute = 480)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNightPremiumSpanningIdenticalHours() {
        com.brendigo.shiftora.data.SalarySettings(nightStartHour = 22, nightEndHour = 22)
    }
    @Test(expected = IllegalArgumentException::class)
    fun forbidsDayOffAndWorkingShiftOnSameDate() {
        val day = LocalDate.of(2026, 10, 19)
        val off = Shift(id = 1, date = day, kind = ShiftKind.OFF)
        val working = Shift(date = day, kind = ShiftKind.MORNING)
        ShiftScheduleValidator.validateInsertion(listOf(off), working, zagreb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun forbidsConflictingDayOffDuringBackupRestore() {
        val day = LocalDate.of(2026, 10, 19)
        ShiftScheduleValidator.validateAll(listOf(
            Shift(date = day, kind = ShiftKind.OFF),
            Shift(date = day, kind = ShiftKind.AFTERNOON)
        ), zagreb)
    }

    @Test fun standbyCanCoexistWithWorkOnSameDay() {
        val day = LocalDate.of(2026, 10, 20)
        val working = Shift(id = 7, date = day, kind = ShiftKind.MORNING)
        val standby = Shift(date = day, kind = ShiftKind.STANDBY,
            startMinute = 840, endMinute = 1200)
        ShiftScheduleValidator.validateInsertion(listOf(working), standby, zagreb)
        ShiftScheduleValidator.validateAll(listOf(working, standby), zagreb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun annualLeaveAndStandbyCannotShareSameDay() {
        val day = LocalDate.of(2026, 10, 20)
        ShiftScheduleValidator.validateAll(listOf(
            Shift(date = day, kind = ShiftKind.ANNUAL),
            Shift(date = day, kind = ShiftKind.STANDBY)
        ), zagreb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun paidLeaveCannotBeAddedOverExistingShift() {
        val day = LocalDate.of(2026, 10, 21)
        ShiftScheduleValidator.validateInsertion(
            listOf(Shift(id = 17, date = day, kind = ShiftKind.DAY)),
            Shift(date = day, kind = ShiftKind.PAID_LEAVE), zagreb
        )
    }

}
