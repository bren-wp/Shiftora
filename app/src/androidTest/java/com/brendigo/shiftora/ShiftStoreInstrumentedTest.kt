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
    @Test fun standbyAndWorkPersistTogetherWithoutInflatingWorkedHours() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val day = LocalDate.of(2097, 3, 5)
        ShiftStore(context).use { store ->
            store.all().filter { it.date == day }.forEach { store.delete(it.id) }
            try {
                store.save(Shift(date = day, kind = ShiftKind.MORNING, actual = true))
                store.save(Shift(date = day, kind = ShiftKind.STANDBY,
                    startMinute = 840, endMinute = 1200))
                val entries = store.all().filter { it.date == day }
                assertEquals(2, entries.size)
                assertTrue(entries.any { it.kind == ShiftKind.STANDBY })
                val summary = com.brendigo.shiftora.domain.WorkCalculator.calculate(
                    entries, com.brendigo.shiftora.data.SalarySettings()
                )
                assertEquals(480L, summary.paidMinutes)
                assertEquals(480L, summary.actualMinutes)
                try {
                    store.save(Shift(date = day, kind = ShiftKind.ANNUAL))
                    throw AssertionError("Annual leave must not coexist with work or standby")
                } catch (_: IllegalArgumentException) {
                    assertEquals(2, store.all().count { it.date == day })
                }
            } finally {
                store.all().filter { it.date == day }.forEach { store.delete(it.id) }
            }
        }
    }

    @Test fun importNeverDeletesOldOrFutureSchedulesEvenWithConflicts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ShiftStore(context).use { store ->
            val oldDay = LocalDate.of(2096, 1, 6)
            val futureDay = LocalDate.of(2096, 12, 29)
            val importedDay = LocalDate.of(2096, 6, 10)
            listOf(oldDay, futureDay, importedDay).forEach { date ->
                store.all().filter { it.date == date }.forEach { store.delete(it.id) }
            }
            try {
                val historic = Shift(date = oldDay, kind = ShiftKind.DAY,
                    note = "Povijesni raspored ostaje zauvijek")
                val future = Shift(date = futureDay, kind = ShiftKind.NIGHT,
                    note = "Budući raspored mora ostati")
                val idOld = store.save(historic)
                val idFuture = store.save(future)
                val merge = store.mergeBackup(listOf(
                    Shift(date = oldDay, kind = ShiftKind.OFF),
                    Shift(date = importedDay, kind = ShiftKind.MORNING)
                ))
                assertEquals(1, merge.added)
                assertEquals(1, merge.skipped)
                val stored = store.all()
                assertTrue(stored.any { it.id == idOld && it.note == historic.note })
                assertTrue(stored.any { it.id == idFuture && it.note == future.note })
                assertTrue(stored.any { it.date == importedDay })
            } finally {
                store.all().filter {
                    it.date.year == 2096 && it.date in LocalDate.of(2096, 1, 1)..LocalDate.of(2096, 12, 31)
                }.forEach { store.delete(it.id) }
            }
        }
    }

    @Test fun archivedShiftSurvivesDatabaseReopeningAndCanBeRestored() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val day = LocalDate.of(2095, 10, 11)
        var archiveId = 0L
        ShiftStore(context).use { store ->
            store.all().filter { it.date == day }.forEach { store.delete(it.id) }
            val id = store.save(Shift(date = day, kind = ShiftKind.DAY, note = "Za oporavak"))
            store.delete(id)
            assertFalse(store.all().any { it.id == id })
            archiveId = store.archived().first {
                it.entry.date == day && it.entry.note == "Za oporavak"
            }.archiveId
            val skippedRotation = store.addRotation(day, listOf(ShiftKind.MORNING), 1)
            assertEquals(0, skippedRotation)
        }
        ShiftStore(context).use { reopened ->
            assertTrue(reopened.archived().any { it.archiveId == archiveId })
            val id = reopened.restoreArchived(archiveId)
            assertTrue(reopened.all().any { it.id == id && it.date == day })
            assertFalse(reopened.archived().any { it.archiveId == archiveId })
            reopened.delete(id)
        }
    }



    @Test fun backupImportMergesArchiveWithoutOverwritingExistingFutureWork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val future = LocalDate.of(2094, 12, 8)
        val historicalArchive = Shift(date = LocalDate.of(2093, 2, 11),
            kind = ShiftKind.NIGHT, note = "Arhiva iz stare kopije")
        ShiftStore(context).use { store ->
            store.all().filter { it.date == future }.forEach { store.delete(it.id) }
            try {
                val id = store.save(Shift(date = future, kind = ShiftKind.DAY,
                    note = "Ovo ostaje u kalendaru"))
                val first = store.mergeBackup(
                    listOf(Shift(date = future, kind = ShiftKind.OFF)),
                    listOf(historicalArchive)
                )
                assertEquals(0, first.added)
                assertEquals(1, first.skipped)
                assertEquals(1, first.archivedAdded)
                assertTrue(store.all().any { it.id == id && it.note == "Ovo ostaje u kalendaru" })
                val second = store.mergeBackup(emptyList(), listOf(historicalArchive))
                assertEquals(0, second.archivedAdded)
                assertTrue(store.archived().any { it.entry.note == historicalArchive.note })
            } finally {
                store.all().filter { it.date == future }.forEach { store.delete(it.id) }
            }
        }
    }

}
