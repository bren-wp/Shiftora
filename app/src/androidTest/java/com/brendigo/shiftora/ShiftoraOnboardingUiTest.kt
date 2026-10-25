package com.brendigo.shiftora

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brendigo.shiftora.ui.OnboardingScreen
import com.brendigo.shiftora.ui.ShiftoraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All reference onboarding pages must remain reachable without external assets. */
@RunWith(AndroidJUnit4::class)
class ShiftoraOnboardingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomeAndThreeTutorialPagesNavigateAndComplete() {
        val completed = mutableStateOf(false)
        val visited = mutableListOf<Int>()
        compose.setContent {
            ShiftoraTheme(darkMode = false) {
                OnboardingScreen(onComplete = { completed.value = true },
                    onStepChange = { visited.add(it) })
            }
        }
        compose.onNodeWithText("Dobro došao u Shiftoru!").assertExists()
        compose.onNodeWithText("Započni").performClick()
        compose.onNodeWithText("Planiraj smjene").assertExists()
        compose.onNodeWithText("Dalje").performClick()
        compose.onNodeWithText("Prati svoje sate").assertExists()
        compose.onNodeWithText("Dalje").performClick()
        compose.onNodeWithText("Procijeni plaću").assertExists()
        compose.onNodeWithText("Započni").performClick()
        compose.runOnIdle {
            assertTrue(completed.value)
            assertEquals(listOf(0, 1, 2, 3), visited.distinct())
        }
    }
}
