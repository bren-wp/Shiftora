package com.brendigo.shiftora

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.YearMonth
import com.brendigo.shiftora.ui.monthLabel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Basic UI smoke test: every navigation destination opens without a crash. */
@RunWith(AndroidJUnit4::class)
class ShiftoraNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun navigationDestinationsAreAccessible() {
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Preskoči").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Preskoči").performClick()
        }
        compose.onNodeWithText("Početna").assertExists()
        // Cold launch and completion of onboarding must land on the calendar,
        // without requiring a bottom-navigation tap.
        compose.onNodeWithText("Mjesec").assertIsSelected()
        compose.onNodeWithText("Tjedan").assertIsNotSelected()
        compose.onNodeWithText("Tjedan").performClick()
        compose.onNodeWithText("Tjedan").assertIsSelected()
        compose.onNodeWithText("Mjesec").assertIsNotSelected()
        compose.onNodeWithText("Mjesec").performClick()
        compose.onNodeWithText("Početna").performClick()
        compose.onNodeWithText("Danas je").assertExists()
        compose.onNodeWithText("Kalendar").performClick()
        compose.onNodeWithText("Mjesec").assertExists()
        compose.onNodeWithContentDescription("Odaberi datum u kalendaru").assertExists()
        val nextMonth = YearMonth.now().plusMonths(1).atDay(1).monthLabel()
        compose.onNodeWithContentDescription("Sljedeći mjesec").performClick()
        compose.onNodeWithText(nextMonth).assertExists()
        // Navigating through tabs must never reset the selected future month.
        compose.onNodeWithText("Statistika").performClick()
        compose.onNodeWithText("Kalendar").performClick()
        compose.onNodeWithText(nextMonth).assertExists()
        // Secondary statuses are collapsible to preserve space for the day card.
        compose.onNodeWithContentDescription("Ostale oznake").performScrollTo().performClick()
        compose.onNodeWithText("GO").assertExists()
        compose.onNodeWithText("BO").assertExists()
        compose.onNodeWithContentDescription("Uredi fond sati").performScrollTo().performClick()
        compose.onNodeWithText("Zadani fond").assertExists()
        compose.onNodeWithText("Zadani fond").performClick()
        compose.onNodeWithText("Statistika").performClick()
        compose.onNodeWithText("Statistika i plaća").assertExists()
        compose.onNodeWithContentDescription("Odaberi razdoblje statistike").assertExists()
        compose.onNodeWithText("Redovni sati").assertExists()
        compose.onNodeWithText("Odrađeni sati").assertExists()
        compose.onNodeWithText("Mjesec").assertIsSelected()
        compose.onNodeWithText("Godina").performClick()
        compose.onNodeWithText("Godina").assertIsSelected()
        compose.onNodeWithText("Tjedan").performClick()
        compose.onNodeWithText("Tjedan").assertIsSelected()
        compose.onNodeWithText("Mjesec").performClick()
        compose.onNodeWithText("Mjesec").assertIsSelected()
        compose.onNodeWithText("Plaća").performClick()
        compose.onNodeWithText("Pregled radnih sati i dodataka.").assertExists()
        compose.onNodeWithContentDescription("Odaberi mjesec obračuna").assertExists()
        compose.onNodeWithText("Više").performClick()
        compose.onNodeWithText("Alati").assertExists()
        compose.onNodeWithText("Boje smjena i izostanaka").performScrollTo().performClick()
        compose.onNodeWithText("Boje i oznake smjena").assertExists()
        compose.onNodeWithText("Gotovo").performClick()
        compose.onNodeWithText("Arhiva rasporeda").performScrollTo().performClick()
        compose.onNodeWithText("Zatvori").assertExists()
        compose.onNodeWithText("Zatvori").performClick()
    }
}
