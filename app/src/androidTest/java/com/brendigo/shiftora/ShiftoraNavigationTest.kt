package com.brendigo.shiftora

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        compose.onNodeWithText("Plaća", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Pregled radnih sati i dodataka.").assertExists()
        compose.onNodeWithText("Više").performClick()
        compose.onNodeWithText("Alati").assertExists()
    }
}
