package com.brendigo.shiftora.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ShiftBackupTest {
    @Test fun roundTripPreservesShiftAndSalarySettings() {
        val shift = Shift(
            date = LocalDate.of(2026, 10, 31),
            kind = ShiftKind.NIGHT, actual = true, overtime = true,
            unpaidBreak = true, breakMinutes = 30, breakAfterMinutes = 180,
            note = "Vikend, noćna smjena — posebne napomene / 漢字"
        )
        val salary = SalarySettings(
            hourlyGross = 12.75, nightPercent = 40.0,
            sundayPercent = 50.0, holidayPercent = 60.0,
            overtimePercent = 70.0, nightStartHour = 21, nightEndHour = 5
        )
        val result = BackupCodec.decode(BackupCodec.encode(listOf(shift), salary))
        assertEquals(shift, result.shifts.single())
        assertEquals(salary, result.salary)
        assertEquals(0L, result.shifts.single().id)
    }

    @Test fun supportsEmptyBackup() {
        assertTrue(BackupCodec.decode(BackupCodec.encode(emptyList(), SalarySettings())).shifts.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongAppFormat() {
        BackupCodec.decode("""{"format":"other","version":1,"salary":{},"shifts":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedSchema() {
        BackupCodec.decode("""{"format":"shiftora-backup","version":4,"salary":{},"shifts":[]}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHugeInputBeforeParsing() {
        BackupCodec.decode(" ".repeat(BackupCodec.MAX_BYTES + 1))
    }

    @Test fun rejectsMalformedShiftWithoutOverwritingData() {
        val original = BackupCodec.encode(
            listOf(Shift(date = LocalDate.of(2026, 10, 1), kind = ShiftKind.MORNING)),
            SalarySettings()
        )
        try {
            BackupCodec.decode(original.replace("\"breakMinutes\": 0", "\"breakMinutes\": 900"))
            fail("Invalid break duration must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.isNotBlank() == true)
        }
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlappingEntriesBeforeRestoreConfirmation() {
        val day = LocalDate.of(2026, 10, 14)
        val first = Shift(date = day, kind = ShiftKind.MORNING)
        val overlapping = Shift(date = day, kind = ShiftKind.CUSTOM,
            startMinute = 700, endMinute = 950)
        val unsafeBackup = BackupCodec.encode(listOf(first, overlapping), SalarySettings())
        BackupCodec.decode(unsafeBackup)
    }

    @Test fun roundTripPreservesColorsAndMonthlyFunds() {
        val shifts = listOf(Shift(date = LocalDate.of(2026, 10, 19), kind = ShiftKind.ANNUAL))
        val colors = mapOf(ShiftKind.DAY to 0xFF123456.toInt(),
            ShiftKind.PAID_LEAVE to 0xFF77AA33.toInt())
        val funds = mapOf("2026-10" to 9600, "2026-11" to 8400)
        val decoded = BackupCodec.decode(BackupCodec.encode(shifts, SalarySettings(), colors, funds))
        assertEquals(3, decoded.version)
        assertEquals(colors, decoded.colors)
        assertEquals(funds, decoded.monthlyFunds)
        assertEquals(ShiftKind.ANNUAL, decoded.shifts.single().kind)
    }

    @Test fun oldVersionOneBackupStillDecodes() {
        val newBackup = BackupCodec.encode(emptyList(), SalarySettings())
        val oldBackup = newBackup.replace("\"version\": 3", "\"version\": 1")
        val decoded = BackupCodec.decode(oldBackup)
        assertEquals(1, decoded.version)
        assertTrue(decoded.colors.isEmpty())
    }


    @Test fun archivingIsIncludedInPortableBackupV3() {
        val history = listOf(Shift(date = LocalDate.of(2021, 2, 14),
            kind = ShiftKind.NIGHT, note = "Arhivirana prošla smjena"))
        val active = listOf(Shift(date = LocalDate.of(2030, 8, 20),
            kind = ShiftKind.DAY, note = "Buduća smjena"))
        val restored = BackupCodec.decode(
            BackupCodec.encode(active, SalarySettings(), archivedShifts = history)
        )
        assertEquals(3, restored.version)
        assertEquals(active, restored.shifts)
        assertEquals(history, restored.archivedShifts)
    }

    @Test fun olderV2BackupDoesNotRequireArchiveField() {
        val current = BackupCodec.encode(
            listOf(Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING)),
            SalarySettings(), monthlyFunds = mapOf("2026-10" to 9600)
        )
        val legacy = current.replace("\"version\": 3", "\"version\": 2")
        val obj = org.json.JSONObject(legacy).apply { remove("archived") }
        val restored = BackupCodec.decode(obj.toString())
        assertEquals(2, restored.version)
        assertEquals(1, restored.shifts.size)
        assertEquals(9600, restored.monthlyFunds["2026-10"])
        assertTrue(restored.archivedShifts.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidArchivedShiftWithoutAffectingActive() {
        val valid = BackupCodec.encode(
            listOf(Shift(date = LocalDate.of(2026, 10, 14), kind = ShiftKind.MORNING)),
            SalarySettings(),
            archivedShifts = listOf(Shift(date = LocalDate.of(2026, 10, 15), kind = ShiftKind.NIGHT))
        )
        val obj = org.json.JSONObject(valid)
        obj.getJSONArray("archived").getJSONObject(0).put("breakMinutes", 999)
        BackupCodec.decode(obj.toString())
    }

}
