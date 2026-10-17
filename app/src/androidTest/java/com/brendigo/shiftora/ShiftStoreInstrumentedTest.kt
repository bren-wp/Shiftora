package com.brendigo.shiftora

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ShiftStoreInstrumentedTest {
    @Test fun createEditDeleteAndPreventOverlapPersistCorrectly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val date = LocalDate.of(2097, 1, 2)
        ShiftStore(context).use { store ->
            store.all().filter { it.date == date }.forEach { store.delete(it.id) }
            var id = 0L
            try {
                id = store.save(Shift(date = date, kind = ShiftKind.MORNING))
                assertTrue(store.all().any { it.id == id && it.kind == ShiftKind.MORNING })
                store.save(store.all().first { it.id == id }.copy(note = "Provjera uređivanja"))
                assertEquals("Provjera uređivanja", store.all().first { it.id == id }.note)
                try {
                    store.save(Shift(date = date, kind = ShiftKind.CUSTOM,
                        startMinute = 700, endMinute = 900))
                    throw AssertionError("Overlapping shift must not be persisted")
                } catch (_: IllegalArgumentException) {
                    assertEquals(1, store.all().count { it.date == date })
                }
            } finally {
                if (id != 0L) store.delete(id)
            }
            assertFalse(store.all().any { it.date == date })
        }
    }

    @Test fun repeatedRotationNeverOverwritesExistingManualShift() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val date = LocalDate.of(2097, 2, 3)
        ShiftStore(context).use { store ->
            store.all().filter { it.date >= date && it.date < date.plusDays(3) }
                .forEach { store.delete(it.id) }
            try {
                store.save(Shift(date = date, kind = ShiftKind.NIGHT, note = "Ručno"))
                val inserted = store.addRotation(date,
                    listOf(ShiftKind.MORNING, ShiftKind.OFF), 3)
                assertEquals(2, inserted)
                assertEquals("Ručno", store.all().single { it.date == date }.note)
            } finally {
                store.all().filter { it.date >= date && it.date < date.plusDays(3) }
                    .forEach { store.delete(it.id) }
            }
        }
    }
}
