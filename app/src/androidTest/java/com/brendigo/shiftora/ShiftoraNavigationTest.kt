package com.brendigo.shiftora

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftStore
import java.time.YearMonth
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
