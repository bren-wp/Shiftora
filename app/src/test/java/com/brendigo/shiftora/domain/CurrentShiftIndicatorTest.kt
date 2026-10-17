package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CurrentShiftIndicatorTest {
    private val zagreb = ZoneId.of("Europe/Zagreb")
    @Test fun activeMorningHasCorrectRemainingTime() {
        val shift = Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING)
        val indicator = CurrentShiftIndicator.active(listOf(shift),
            Instant.parse("2026-10-14T08:30:00Z"), zagreb)
        assertNotNull(indicator)
        assertEquals(210L, indicator!!.remainingMinutes)
    }
    @Test fun nightShiftRemainsActiveAfterMidnight() {
        val shift = Shift(date = LocalDate.of(2026, 10, 17), kind = ShiftKind.NIGHT)
        val indicator = CurrentShiftIndicator.active(listOf(shift),
            Instant.parse("2026-10-18T01:00:00Z"), zagreb)
        assertNotNull(indicator)
        assertEquals(180L, indicator!!.remainingMinutes)
        assertEquals(shift, indicator.entry)
    }
    @Test fun endIsExclusiveAndOffDoesNotBecomeActive() {
        val date = LocalDate.of(2026, 10, 14)
        val shift = Shift(date = date, kind = ShiftKind.MORNING)
        assertNull(CurrentShiftIndicator.active(listOf(shift),
            Instant.parse("2026-10-14T12:00:00Z"), zagreb))
        assertNull(CurrentShiftIndicator.active(listOf(Shift(date = date, kind = ShiftKind.OFF)),
            Instant.parse("2026-10-14T09:00:00Z"), zagreb))
    }
}
