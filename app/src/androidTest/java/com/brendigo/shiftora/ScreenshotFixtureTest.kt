package com.brendigo.shiftora

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftStore
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/**
 * This fixture can only populate an emulator when capture-emulator.sh explicitly
 * invokes it. Regular instrumentation runs skip it; production contains no seed code.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotFixtureTest {
    @Test fun seedScreenshotMonth() {
        assumeTrue(
            "Screenshot data is opt-in and must never affect real installations",
            InstrumentationRegistry.getArguments()
                .getString("shiftoraScreenshotFixture") == "true"
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val month = YearMonth.now()
        ShiftStore(context).use { store ->
            val pattern = listOf(
                ShiftKind.MORNING,
                ShiftKind.AFTERNOON,
                ShiftKind.NIGHT,
                ShiftKind.OFF
            )
            // Full-month examples let screenshot QA inspect filled calendar
            // cells, populated statistics and upcoming shift rows.
            store.addRotation(month.atDay(1), pattern, month.lengthOfMonth())
        }
    }
}
