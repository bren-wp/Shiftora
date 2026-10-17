package com.brendigo.shiftora.domain

import java.time.LocalDate

/** National non-working public holidays in Croatia, including movable feasts. */
object CroatianHolidays {
    fun easter(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    fun isHoliday(date: LocalDate): Boolean {
        val fixed = setOf(
            1 to 1, 1 to 6, 5 to 1, 5 to 30, 6 to 22,
            8 to 5, 8 to 15, 11 to 1, 11 to 18, 12 to 25, 12 to 26
        )
        val easter = easter(date.year)
        return (date.monthValue to date.dayOfMonth) in fixed ||
            date == easter || date == easter.plusDays(1) || date == easter.plusDays(60)
    }
}
