package com.brendigo.shiftora

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.ui.CalendarScreen
import com.brendigo.shiftora.ui.ShiftoraTheme
import com.brendigo.shiftora.ui.dateLabel
import com.brendigo.shiftora.ui.monthLabel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/** Guard calendar visual controls and assistive descriptions against UI regressions. */
@RunWith(AndroidJUnit4::class)
class ShiftoraCalendarUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun datesUseCroatianGrammaticalMonthCase() {
        val october = LocalDate.of(2026, 10, 24)
        assertEquals("24. listopada 2026.", october.dateLabel())
        assertEquals("Listopad 2026.", october.monthLabel())
    }

    @Test fun sameDayWorkAndStandbyAreBothRepresentedAndAccessible() {
        val date = YearMonth.now().atDay(15)
        val entries = listOf(
            Shift(date = date, kind = ShiftKind.MORNING),
            Shift(date = date, kind = ShiftKind.STANDBY, startMinute = 840, endMinute = 1200)
        )
        compose.setContent {
            ShiftoraTheme(darkMode = false) {
                CalendarScreen(entries, SalarySettings(), emptyMap(),
                    onAdd = {}, onEdit = {}, onFundSave = { _, _ -> }, onSettings = {})
            }
        }
        compose.onNodeWithContentDescription(
            "${date.dateLabel()}. J: Jutarnja, PR: Pripravnost"
        ).assertExists()
    }

    @Test fun weekNavigationMovesBySevenDaysAndRetainsTheCorrectMonth() {
        compose.setContent {
            ShiftoraTheme(darkMode = false) {
                CalendarScreen(emptyList(), SalarySettings(), emptyMap(),
                    onAdd = {}, onEdit = {}, onFundSave = { _, _ -> }, onSettings = {})
            }
        }
        compose.onNodeWithText("Tjedan").performClick()
        compose.onNodeWithContentDescription("Prethodni tjedan").assertExists()
        repeat(6) {
            compose.onNodeWithContentDescription("Prethodni tjedan").performClick()
        }
        val month = YearMonth.from(LocalDate.now().minusWeeks(6))
        compose.onNodeWithText(month.atDay(1).monthLabel()).assertExists()
    }

    @Test fun selectedDayMenuExposesRealActionsAndLegendRemainsReachable() {
        compose.setContent {
            ShiftoraTheme(darkMode = false) {
                CalendarScreen(emptyList(), SalarySettings(), emptyMap(),
                    onAdd = {}, onEdit = {}, onFundSave = { _, _ -> },
                    onSettings = {})
            }
        }
        compose.onNodeWithContentDescription("Ostale oznake")
            .performScrollTo().performClick()
        compose.onNodeWithContentDescription("Sakrij oznake").assertExists()
        compose.onNodeWithContentDescription("Radnje za odabrani dan")
            .performScrollTo().performClick()
        // One action is in the date menu; the other is the date-card CTA.
        compose.onAllNodesWithText("Dodaj smjenu").assertCountEquals(2)
        compose.onNodeWithText("Povratak na danas").assertExists()
        compose.onNodeWithText("Povratak na danas").performClick()
    }

}
