package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class ActiveShift(val entry: Shift, val remainingMinutes: Long)

/** Instant-based state for an active scheduled shift, including night shifts across midnight. */
object CurrentShiftIndicator {
    fun active(
        shifts: List<Shift>,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault()
    ): ActiveShift? {
        return shifts.asSequence().filter { it.kind.isWork }.mapNotNull { shift ->
            val from = ShiftTimeline.from(shift, zone)
            val until = ShiftTimeline.until(shift, zone)
            if (now.isBefore(from) || !now.isBefore(until)) null
            else ActiveShift(shift, (Duration.between(now, until).seconds + 59) / 60)
        }.firstOrNull()
    }
}
