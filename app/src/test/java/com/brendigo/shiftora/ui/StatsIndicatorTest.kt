package com.brendigo.shiftora.ui

import org.junit.Assert.*
import org.junit.Test

class StatsIndicatorTest {
    @Test fun zeroBaselineDoesNotInventIncrease() {
        assertNull(percentageChange(120L, 0L))
        assertEquals(0.0, percentageChange(0L, 0L)!!, 0.0001)
    }
    @Test fun relativeChangeIsSignedAndBasedOnPreviousPeriod() {
        assertEquals(25.0, percentageChange(100L, 80L)!!, 0.0001)
        assertEquals(-25.0, percentageChange(60L, 80L)!!, 0.0001)
        assertEquals(0.0, percentageChange(80L, 80L)!!, 0.0001)
    }
}
