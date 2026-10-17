package com.brendigo.shiftora

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brendigo.shiftora.data.Preferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesInstrumentedTest {
    @Test fun corruptedSavedSalaryFieldsDoNotCrashAppStartup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val raw = context.getSharedPreferences("shiftora_settings", 0)
        val keys = listOf("hourly", "night", "sunday", "holiday", "overtime")
        val originals = keys.associateWith { raw.getString(it, null) }
        val oldFrom = if (raw.contains("night_start_hour")) raw.getInt("night_start_hour", 22) else null
        val oldUntil = if (raw.contains("night_end_hour")) raw.getInt("night_end_hour", 6) else null
        try {
            raw.edit()
                .putString("hourly", "Infinity")
                .putString("night", "NaN")
                .putString("sunday", "-50")
                .putString("holiday", "1000000")
                .putString("overtime", "bad-data")
                .putInt("night_start_hour", 23)
                .putInt("night_end_hour", 23)
                .commit()
            val settings = Preferences(context).salarySettings()
            assertEquals(8.0, settings.hourlyGross, 0.001)
            assertEquals(30.0, settings.nightPercent, 0.001)
            assertEquals(50.0, settings.sundayPercent, 0.001)
            assertEquals(50.0, settings.holidayPercent, 0.001)
            assertEquals(50.0, settings.overtimePercent, 0.001)
            assertEquals(22, settings.nightStartHour)
            assertEquals(6, settings.nightEndHour)
        } finally {
            val edit = raw.edit()
            originals.forEach { (key, value) ->
                if (value == null) edit.remove(key) else edit.putString(key, value)
            }
            if (oldFrom == null) edit.remove("night_start_hour") else edit.putInt("night_start_hour", oldFrom)
            if (oldUntil == null) edit.remove("night_end_hour") else edit.putInt("night_end_hour", oldUntil)
            edit.commit()
        }
    }
}
