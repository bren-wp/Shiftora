package com.brendigo.shiftora

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.ui.CalendarScreen
import com.brendigo.shiftora.ui.ShiftoraTheme
import com.brendigo.shiftora.ui.StatsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phone-width and large-font regression fixtures. These assert meaningful UI
 * composition; visual 1:1 equivalence still requires screenshot comparison.
 */
@RunWith(AndroidJUnit4::class)
class ShiftoraResponsiveUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun calendarComposesAt320dpAnd150PercentFont() {
        compose.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 1.5f)
            ) {
                ShiftoraTheme(darkMode = false) {
                    Box(Modifier.width(320.dp).height(600.dp)) {
                        CalendarScreen(
                            shifts = emptyList(), salary = SalarySettings(),
                            monthlyFunds = emptyMap(), onAdd = {}, onEdit = {},
                            onFundSave = { _, _ -> }, onSettings = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Mjesec").assertExists()
        compose.onNodeWithText("Tjedan").assertExists()
        compose.onNodeWithText("Ostale oznake").assertExists()
    }

    @Test fun statisticsComposesAt320dpAnd150PercentFont() {
        compose.setContent {
            val deviceDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity.density, fontScale = 1.5f)
            ) {
                ShiftoraTheme(darkMode = false) {
                    Box(Modifier.width(320.dp).height(600.dp)) {
                        StatsScreen(emptyList(), SalarySettings(), onSettings = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Statistika i plaća").assertExists()
        compose.onNodeWithText("Mjesec").assertExists()
        compose.onNodeWithText("Redovni sati").assertExists()
    }
}
