package com.brendigo.shiftora

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftStore
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
        compose.onNodeWithText("Kalendar").performClick()
        compose.onNodeWithText("Mjesec").assertExists()
        val nextMonth = YearMonth.now().plusMonths(1).atDay(1).monthLabel()
        compose.onNodeWithContentDescription("Sljedeći mjesec").performClick()
        compose.onNodeWithText(nextMonth).assertExists()
        // Navigating through tabs must never reset the selected future month.
        compose.onNodeWithText("Statistika").performClick()
        compose.onNodeWithText("Kalendar").performClick()
        compose.onNodeWithText(nextMonth).assertExists()
        compose.onNodeWithText("GO").assertExists()
        compose.onNodeWithText("BO").assertExists()
        compose.onNodeWithContentDescription("Uredi fond sati").performScrollTo().performClick()
        compose.onNodeWithText("Zadani fond").assertExists()
        compose.onNodeWithText("Zadani fond").performClick()
        compose.onNodeWithText("Statistika").performClick()
        compose.onNodeWithText("Statistika i plaća").assertExists()
        compose.onNodeWithText("Redovni sati").assertExists()
        compose.onNodeWithText("Odrađeni sati").assertExists()
        compose.onNodeWithText("Godina").performClick()
        compose.onNodeWithText("Tjedan").performClick()
        compose.onNodeWithText("Mjesec").performClick()
        compose.onNodeWithText("Plaća").performClick()
        compose.onNodeWithText("Pregled radnih sati i dodataka.").assertExists()
        compose.onNodeWithText("Više").performClick()
        compose.onNodeWithText("Alati").assertExists()
        compose.onNodeWithText("Boje smjena i izostanaka").performClick()
        compose.onNodeWithText("Boje i oznake smjena").assertExists()
        compose.onNodeWithText("Gotovo").performClick()
        compose.onNodeWithText("Arhiva rasporeda").performScrollTo().performClick()
        compose.onNodeWithText("Zatvori").assertExists()
        compose.onNodeWithText("Zatvori").performClick()
        // Emulator-only sample data used by screenshot QA. Never included in production.
        val store = ShiftStore(compose.activity.applicationContext)
        try {
            val thisMonth = YearMonth.now()
            store.addRotation(
                thisMonth.atDay(1),
                listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF),
                thisMonth.lengthOfMonth()
            )
        } finally {
            store.close()
        }
    }
}
