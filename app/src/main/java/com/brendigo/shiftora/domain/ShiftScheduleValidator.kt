package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import java.time.Instant
import java.time.ZoneId

/**
 * Guard against accidentally counting two overlapping shift entries twice.
 * Intervals are half-open: a shift ending at 14:00 and one starting at 14:00
 * do not overlap. Previous-day night shifts are accounted for.
 */
object ShiftScheduleValidator {
    private data class Interval(val from: Instant, val to: Instant)

    private fun interval(shift: Shift, zone: ZoneId): Interval {
        val start = shift.date.atStartOfDay().plusMinutes(shift.startMinute.toLong()).atZone(zone).toInstant()
        val endDate = if (shift.overnight) shift.date.plusDays(1) else shift.date
        val end = endDate.atStartOfDay().plusMinutes(shift.endMinute.toLong()).atZone(zone).toInstant()
        require(end.isAfter(start)) { "Završetak smjene mora biti nakon početka." }
        return Interval(start, end)
    }

    fun overlaps(a: Shift, b: Shift, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (a.kind == ShiftKind.OFF || b.kind == ShiftKind.OFF) return false
        val left = interval(a, zone)
        val right = interval(b, zone)
        return left.from.isBefore(right.to) && right.from.isBefore(left.to)
    }

    fun validateBreak(shift: Shift, zone: ZoneId = ZoneId.systemDefault()) {
        if (shift.kind == ShiftKind.OFF || shift.breakMinutes == 0) return
        val elapsed = WorkCalculator.durationMinutes(shift, zone)
        require(shift.breakAfterMinutes.toLong() + shift.breakMinutes <= elapsed) {
            "Pauza mora završiti prije završetka smjene."
        }
    }

    fun validateInsertion(existing: List<Shift>, candidate: Shift, zone: ZoneId = ZoneId.systemDefault()) {
        validateBreak(candidate, zone)
        require(existing.none {
            it.id != candidate.id && it.date == candidate.date &&
                (it.kind == ShiftKind.OFF || candidate.kind == ShiftKind.OFF)
        }) {
            "Slobodan dan ne može se kombinirati s drugom smjenom. Uredi postojeći unos."
        }
        require(existing.none { it.id != candidate.id && overlaps(it, candidate, zone) }) {
            "Smjena se preklapa s postojećom smjenom. Provjeri početak i završetak."
        }
    }

    fun validateAll(entries: List<Shift>, zone: ZoneId = ZoneId.systemDefault()) {
        entries.forEach { validateBreak(it, zone) }
        val grouped = entries.groupBy { it.date }
        require(grouped.values.none { day -> day.size > 1 && day.any { it.kind == ShiftKind.OFF } }) {
            "Slobodan dan i radne smjene ne smiju biti uneseni na isti datum."
        }
        val intervals = entries.filter { it.kind != ShiftKind.OFF }.map { interval(it, zone) }.sortedBy { it.from }
        for (index in 1 until intervals.size) {
            require(!intervals[index].from.isBefore(intervals[index - 1].to)) {
                "Raspored sadrži preklapajuće smjene."
            }
        }
    }
}
