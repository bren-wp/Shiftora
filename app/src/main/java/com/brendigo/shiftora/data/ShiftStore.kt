package com.brendigo.shiftora.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import com.brendigo.shiftora.domain.ShiftScheduleValidator

/** Local-only storage: no internet permission, accounts or analytics. */
class ShiftStore(context: Context) : SQLiteOpenHelper(context, "shiftora.db", null, 1) {
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit, non-destructive migrations when schema changes.
        require(oldVersion == newVersion) { "Missing migration from $oldVersion to $newVersion" }
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

    @Synchronized fun delete(id: Long) {
        writableDatabase.delete("shifts", "id=?", arrayOf(id.toString()))
    }

    /**
     * Transactional replacement. The original schedule remains intact when a row is invalid
     * or the database encounters an error before the transaction is committed.
     */
    @Synchronized fun replaceAll(shifts: List<Shift>) {
        require(shifts.size <= 20_000)
        ShiftScheduleValidator.validateAll(shifts)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("shifts", null, null)
            shifts.forEach { shift ->
                db.insertOrThrow("shifts", null, values(shift))
            }
            db.setTransactionSuccessful()
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
            db.rawQuery("SELECT DISTINCT date FROM shifts", null).use { c ->
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
