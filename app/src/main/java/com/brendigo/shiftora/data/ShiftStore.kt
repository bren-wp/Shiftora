package com.brendigo.shiftora.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.Instant
import com.brendigo.shiftora.domain.ShiftScheduleValidator

data class ArchivedShift(val archiveId: Long, val archivedAt: String, val entry: Shift)

/** Local-only storage: no internet permission, accounts or analytics. */
class ShiftStore(context: Context, databaseName: String = "shiftora.db") :
    SQLiteOpenHelper(context, databaseName, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE shifts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                kind TEXT NOT NULL,
                start_minute INTEGER NOT NULL,
                end_minute INTEGER NOT NULL,
                break_minutes INTEGER NOT NULL,
                break_after INTEGER NOT NULL,
                unpaid_break INTEGER NOT NULL,
                actual INTEGER NOT NULL,
                overtime INTEGER NOT NULL,
                note TEXT NOT NULL
            )""".trimIndent()
        )
        db.execSQL("CREATE INDEX idx_shifts_date ON shifts(date)")
        createArchive(db)
    }

    private fun createArchive(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS shift_archive (
            archive_id INTEGER PRIMARY KEY AUTOINCREMENT,
            original_id INTEGER NOT NULL,
            archived_at TEXT NOT NULL,
            date TEXT NOT NULL,
            kind TEXT NOT NULL,
            start_minute INTEGER NOT NULL,
            end_minute INTEGER NOT NULL,
            break_minutes INTEGER NOT NULL,
            break_after INTEGER NOT NULL,
            unpaid_break INTEGER NOT NULL,
            actual INTEGER NOT NULL,
            overtime INTEGER NOT NULL,
            note TEXT NOT NULL
        )""".trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_shift_archive_date ON shift_archive(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createArchive(db)
    }

    @Synchronized fun all(): List<Shift> {
        val shifts = mutableListOf<Shift>()
        readableDatabase.rawQuery("SELECT * FROM shifts ORDER BY date, start_minute, id", null).use { c ->
            while (c.moveToNext()) {
                shifts += Shift(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    date = LocalDate.parse(c.getString(c.getColumnIndexOrThrow("date"))),
                    kind = ShiftKind.valueOf(c.getString(c.getColumnIndexOrThrow("kind"))),
                    startMinute = c.getInt(c.getColumnIndexOrThrow("start_minute")),
                    endMinute = c.getInt(c.getColumnIndexOrThrow("end_minute")),
                    breakMinutes = c.getInt(c.getColumnIndexOrThrow("break_minutes")),
                    breakAfterMinutes = c.getInt(c.getColumnIndexOrThrow("break_after")),
                    unpaidBreak = c.getInt(c.getColumnIndexOrThrow("unpaid_break")) != 0,
                    actual = c.getInt(c.getColumnIndexOrThrow("actual")) != 0,
                    overtime = c.getInt(c.getColumnIndexOrThrow("overtime")) != 0,
                    note = c.getString(c.getColumnIndexOrThrow("note"))
                )
            }
        }
        return shifts
    }

    /** Records are kept here when the user explicitly removes a calendar entry. */
    @Synchronized fun archived(): List<ArchivedShift> {
        val items = mutableListOf<ArchivedShift>()
        readableDatabase.rawQuery("SELECT * FROM shift_archive ORDER BY archive_id DESC", null).use { c ->
            while (c.moveToNext()) {
                val entry = Shift(
                    id = c.getLong(c.getColumnIndexOrThrow("original_id")),
                    date = LocalDate.parse(c.getString(c.getColumnIndexOrThrow("date"))),
                    kind = ShiftKind.valueOf(c.getString(c.getColumnIndexOrThrow("kind"))),
                    startMinute = c.getInt(c.getColumnIndexOrThrow("start_minute")),
                    endMinute = c.getInt(c.getColumnIndexOrThrow("end_minute")),
                    breakMinutes = c.getInt(c.getColumnIndexOrThrow("break_minutes")),
                    breakAfterMinutes = c.getInt(c.getColumnIndexOrThrow("break_after")),
                    unpaidBreak = c.getInt(c.getColumnIndexOrThrow("unpaid_break")) != 0,
                    actual = c.getInt(c.getColumnIndexOrThrow("actual")) != 0,
                    overtime = c.getInt(c.getColumnIndexOrThrow("overtime")) != 0,
                    note = c.getString(c.getColumnIndexOrThrow("note"))
                )
                items += ArchivedShift(
                    c.getLong(c.getColumnIndexOrThrow("archive_id")),
                    c.getString(c.getColumnIndexOrThrow("archived_at")), entry
                )
            }
        }
        return items
    }

    private fun values(shift: Shift) = ContentValues().apply {
        put("date", shift.date.toString())
        put("kind", shift.kind.name)
        put("start_minute", shift.startMinute)
        put("end_minute", shift.endMinute)
        put("break_minutes", shift.breakMinutes)
        put("break_after", shift.breakAfterMinutes)
        put("unpaid_break", if (shift.unpaidBreak) 1 else 0)
        put("actual", if (shift.actual) 1 else 0)
        put("overtime", if (shift.overtime) 1 else 0)
        put("note", shift.note)
    }

    @Synchronized fun save(shift: Shift): Long {
        ShiftScheduleValidator.validateInsertion(all(), shift)
        val db = writableDatabase
        if (shift.id == 0L) return db.insertOrThrow("shifts", null, values(shift))
        val count = db.update("shifts", values(shift), "id=?", arrayOf(shift.id.toString()))
        require(count == 1) { "Shift not found: ${shift.id}" }
        return shift.id
    }

    /**
     * No irreversible deletion from the calendar. Move the full record to the
     * on-device archive atomically so it can be restored at any future date.
     */
    @Synchronized fun delete(id: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("""INSERT INTO shift_archive (
                    original_id, archived_at, date, kind, start_minute, end_minute,
                    break_minutes, break_after, unpaid_break, actual, overtime, note
                ) SELECT id, ?, date, kind, start_minute, end_minute, break_minutes,
                    break_after, unpaid_break, actual, overtime, note
                FROM shifts WHERE id = ?""",
                arrayOf(Instant.now().toString(), id))
            require(db.delete("shifts", "id=?", arrayOf(id.toString())) == 1) {
                "Smjena nije pronađena. Nema promjena."
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Archived entry stays untouched if restoration would create a conflict. */
    @Synchronized fun restoreArchived(archiveId: Long): Long {
        val entry = archived().firstOrNull { it.archiveId == archiveId }
            ?: error("Arhivirana smjena nije pronađena.")
        val candidate = entry.entry.copy(id = 0L)
        ShiftScheduleValidator.validateInsertion(all(), candidate)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val restored = db.insertOrThrow("shifts", null, values(candidate))
            require(db.delete("shift_archive", "archive_id=?", arrayOf(archiveId.toString())) == 1) {
                "Arhivirani unos nije pronađen."
            }
            db.setTransactionSuccessful()
            restored
        } finally {
            db.endTransaction()
        }
    }

    data class MergeResult(val added: Int, val skipped: Int, val archivedAdded: Int)

    /**
     * Non-destructive import. Every existing past/future shift keeps its original
     * row and ID. Conflicting or identical incoming entries are skipped.
     */
    @Synchronized fun mergeBackup(
        incoming: List<Shift>, incomingArchived: List<Shift> = emptyList()
    ): MergeResult {
        require(incoming.size + incomingArchived.size <= 20_000) {
            "Sigurnosna kopija sadrži previše smjena."
        }
        ShiftScheduleValidator.validateAll(incoming)
        incomingArchived.forEach { ShiftScheduleValidator.validateBreak(it) }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val saved = all()
            val preservedArchive = archived().map { it.entry.copy(id = 0L) }.toMutableSet()
            // An archived date reflects an explicit removal; a bulk import must not
            // silently recreate its calendar entry. Manual creation/restoration is still allowed.
            val archivedDates = preservedArchive.mapTo(mutableSetOf()) { it.date }
            val existingEntries = saved.mapTo(mutableSetOf()) { it.copy(id = 0L) }
            val entriesByDate = saved.groupBy { it.date }
                .mapValuesTo(mutableMapOf()) { (_, items) -> items.toMutableList() }
            var activeCount = saved.size
            var added = 0
            var skipped = 0
            var archivedAdded = 0
            incoming.forEach { raw ->
                val shift = raw.copy(id = 0L)
                // A shift can only span its own date and the next one. Checking
                // neighbouring start dates preserves overnight overlap validation
                // without repeatedly scanning every historical shift.
                val nearby = listOf(shift.date.minusDays(1), shift.date, shift.date.plusDays(1))
                    .flatMap { entriesByDate[it].orEmpty() }
                val valid = shift !in existingEntries &&
                    shift.date !in archivedDates &&
                    runCatching {
                        ShiftScheduleValidator.validateInsertion(nearby, shift)
                    }.isSuccess
                if (!valid || activeCount >= 20_000) {
                    skipped++
                } else {
                    val id = db.insertOrThrow("shifts", null, values(shift))
                    entriesByDate.getOrPut(shift.date) { mutableListOf() }.add(shift.copy(id = id))
                    existingEntries += shift
                    activeCount++
                    added++
                }
            }
            incomingArchived.forEach { raw ->
                val shift = raw.copy(id = 0L)
                if (shift !in preservedArchive) {
                    val archiveValues = ContentValues(values(shift)).apply {
                        put("original_id", raw.id)
                        put("archived_at", Instant.now().toString())
                    }
                    db.insertOrThrow("shift_archive", null, archiveValues)
                    preservedArchive += shift
                    archivedAdded++
                }
            }
            db.setTransactionSuccessful()
            MergeResult(added, skipped, archivedAdded)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Add an entire rotation atomically, but preserve days where the user already entered data.
     * No silent overwrite of manual work is allowed.
     */
    @Synchronized fun addRotation(start: LocalDate, pattern: List<ShiftKind>, days: Int): Int {
        require(pattern.isNotEmpty() && days in 1..366)
        val db = writableDatabase
        var inserted = 0
        db.beginTransaction()
        try {
            val saved = all().toMutableList()
            val existing = mutableSetOf<String>()
            db.rawQuery("SELECT date FROM shifts UNION SELECT date FROM shift_archive", null).use { c ->
                while (c.moveToNext()) existing += c.getString(0)
            }
            repeat(days) { offset ->
                val date = start.plusDays(offset.toLong())
                if (date.toString() !in existing) {
                    val kind = pattern[offset % pattern.size]
                    val candidate = Shift(date = date, kind = kind)
                    if (saved.none { ShiftScheduleValidator.overlaps(it, candidate) }) {
                        db.insertOrThrow("shifts", null, values(candidate))
                        saved += candidate
                        existing += date.toString()
                        inserted++
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return inserted
    }
}
