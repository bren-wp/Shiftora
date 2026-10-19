package com.brendigo.shiftora

import android.content.Context
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

/**
 * A dedicated database per test prevents archived fixtures from polluting user data
 * or changing the outcome of later emulator runs.
 */
@RunWith(AndroidJUnit4::class)
class ShiftStoreDataSafetyInstrumentedTest {
    private fun isolatedStore(test: (ShiftStore) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "shiftora_safety_${System.nanoTime()}.db"
        try {
            ShiftStore(context, name).use(test)
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun importingAnArchivedDateDoesNotSilentlyRestoreIt() = isolatedStore { store ->
        val archivedDay = LocalDate.of(2163, 1, 11)
        val newDay = LocalDate.of(2163, 1, 13)
        val originalId = store.save(Shift(date = archivedDay, kind = ShiftKind.DAY,
            note = "Korisnik je arhivirao ovu smjenu"))
        store.delete(originalId)
        val archiveId = store.archived().single().archiveId

        val summary = store.mergeBackup(listOf(
            Shift(date = archivedDay, kind = ShiftKind.MORNING),
            Shift(date = newDay, kind = ShiftKind.NIGHT)
        ))

        assertEquals(1, summary.added)
        assertEquals(1, summary.skipped)
        assertFalse(store.all().any { it.date == archivedDay })
        assertTrue(store.all().any { it.date == newDay })
        assertEquals(archiveId, store.archived().single().archiveId)
        assertEquals("Korisnik je arhivirao ovu smjenu", store.archived().single().entry.note)

        // The user may still explicitly restore their original archived entry.
        store.restoreArchived(archiveId)
        assertTrue(store.all().any { it.date == archivedDay && it.kind == ShiftKind.DAY })
    }

    @Test fun indexedImportStillRejectsPreviousNightOverlap() = isolatedStore { store ->
        val start = LocalDate.of(2164, 5, 4)
        val id = store.save(Shift(date = start, kind = ShiftKind.CUSTOM,
            startMinute = 23 * 60, endMinute = 7 * 60, note = "Noćni rad"))
        val result = store.mergeBackup(listOf(
            Shift(date = start.plusDays(1), kind = ShiftKind.MORNING)
        ))
        assertEquals(0, result.added)
        assertEquals(1, result.skipped)
        assertEquals(id, store.all().single().id)
    }

    @Test fun repeatedImportIsIdempotentWithoutRemovingOriginalShifts() = isolatedStore { store ->
        val historical = Shift(date = LocalDate.of(2158, 2, 3),
            kind = ShiftKind.MORNING, note = "Stara smjena")
        val future = Shift(date = LocalDate.of(2168, 2, 3),
            kind = ShiftKind.NIGHT, note = "Buduća smjena")
        val id = store.save(historical)
        val first = store.mergeBackup(listOf(historical, future))
        val second = store.mergeBackup(listOf(historical, future))
        assertEquals(1, first.added)
        assertEquals(1, first.skipped)
        assertEquals(0, second.added)
        assertEquals(2, second.skipped)
        assertEquals(id, store.all().first { it.date == historical.date }.id)
        assertEquals(2, store.all().size)
    }

    @Test fun upgradingLegacyV1DatabaseRetainsRowsAndCreatesArchive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "shiftora_v1_migration_${System.nanoTime()}.db"
        val day = LocalDate.of(2159, 6, 22)
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL("""CREATE TABLE shifts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL, kind TEXT NOT NULL,
                    start_minute INTEGER NOT NULL, end_minute INTEGER NOT NULL,
                    break_minutes INTEGER NOT NULL, break_after INTEGER NOT NULL,
                    unpaid_break INTEGER NOT NULL, actual INTEGER NOT NULL,
                    overtime INTEGER NOT NULL, note TEXT NOT NULL
                )""".trimIndent())
                db.execSQL(
                    """INSERT INTO shifts (date, kind, start_minute, end_minute,
                        break_minutes, break_after, unpaid_break, actual, overtime, note)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf(day.toString(), "NIGHT", 1320, 360, 0, 240, 0, 1, 0, "Migrirana smjena")
                )
                db.version = 1
            }
            ShiftStore(context, name).use { store ->
                assertEquals(2, store.readableDatabase.version)
                val shift = store.all().single()
                assertEquals(day, shift.date)
                assertEquals("Migrirana smjena", shift.note)
                store.delete(shift.id)
                assertTrue(store.all().isEmpty())
                assertEquals("Migrirana smjena", store.archived().single().entry.note)
            }
            ShiftStore(context, name).use { store ->
                assertEquals("Migrirana smjena", store.archived().single().entry.note)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
