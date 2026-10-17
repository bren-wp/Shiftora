package com.brendigo.shiftora.domain

import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.SalarySettings
import java.time.DayOfWeek
import java.time.YearMonth

/** Informational working-time fund; individual employers may apply different rules. */
data class MonthlyFundSummary(
    val fundMinutes: Int,
    val defaultFundMinutes: Int,
    val recordedMinutes: Long,
    val actuallyWorkedMinutes: Long,
    val leaveCreditMinutes: Int,
    val remainingMinutes: Long,
    val overFundMinutes: Long
)

object MonthlyFund {
    fun standardMinutes(month: YearMonth): Int =
        (1..month.lengthOfMonth()).count { day ->
            val date = month.atDay(day)
            date.dayOfWeek != DayOfWeek.SATURDAY &&
                date.dayOfWeek != DayOfWeek.SUNDAY &&
                !CroatianHolidays.isHoliday(date)
        } * 480

    fun summary(
        month: YearMonth,
        shifts: List<Shift>,
        settings: SalarySettings,
        overrideMinutes: Int? = null
    ): MonthlyFundSummary {
        require(overrideMinutes == null || overrideMinutes in 0..44640)
        val standard = standardMinutes(month)
        val target = overrideMinutes ?: standard
        val work = WorkCalculator.calculatePeriod(
            shifts, settings, month.atDay(1), month.plusMonths(1).atDay(1)
        )
        // Leave credit is tracked separately from actual hours and payroll.
        // Only working weekdays without public holidays receive 8h of credit.
        val leaveCredit = shifts.asSequence()
            .filter { it.kind.isAbsence && YearMonth.from(it.date) == month }
            .map { it.date }.distinct()
            .count {
                it.dayOfWeek != DayOfWeek.SATURDAY &&
                    it.dayOfWeek != DayOfWeek.SUNDAY &&
                    !CroatianHolidays.isHoliday(it)
            } * 480
        val accounted = work.actualMinutes + leaveCredit
        return MonthlyFundSummary(
            fundMinutes = target, defaultFundMinutes = standard,
            recordedMinutes = work.paidMinutes,
            actuallyWorkedMinutes = work.actualMinutes,
            leaveCreditMinutes = leaveCredit,
            remainingMinutes = (target.toLong() - accounted).coerceAtLeast(0L),
            overFundMinutes = (accounted - target).coerceAtLeast(0L)
        )
    }
}
