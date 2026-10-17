package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import java.time.Instant
import java.time.ZoneId

/** Single time-boundary implementation for calculation, active indicators and validation. */
internal object ShiftTimeline {
    fun from(shift: Shift, zone: ZoneId): Instant =
        shift.date.atStartOfDay().plusMinutes(shift.startMinute.toLong()).atZone(zone).toInstant()

    fun until(shift: Shift, zone: ZoneId): Instant {
        val endDate = if (shift.overnight) shift.date.plusDays(1) else shift.date
        return endDate.atStartOfDay().plusMinutes(shift.endMinute.toLong())
            .atZone(zone).toInstant()
    }
}
