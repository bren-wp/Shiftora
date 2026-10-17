package com.brendigo.shiftora.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Explicit user-controlled portable backup. No cloud transfer or implicit restore. */
data class ShiftBackup(val shifts: List<Shift>, val salary: SalarySettings)

object BackupCodec {
    private const val FORMAT = "shiftora-backup"
    private const val VERSION = 1
    const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_SHIFTS = 20_000

    fun encode(shifts: List<Shift>, salary: SalarySettings): String {
        require(shifts.size <= MAX_SHIFTS) { "Previše smjena za sigurnosnu kopiju." }
        val settings = JSONObject()
            .put("hourlyGross", salary.hourlyGross)
            .put("nightPercent", salary.nightPercent)
            .put("sundayPercent", salary.sundayPercent)
            .put("holidayPercent", salary.holidayPercent)
            .put("overtimePercent", salary.overtimePercent)
            .put("nightStartHour", salary.nightStartHour)
            .put("nightEndHour", salary.nightEndHour)
        val data = JSONArray()
        shifts.forEach { shift ->
            data.put(JSONObject()
                .put("date", shift.date.toString())
                .put("kind", shift.kind.name)
                .put("startMinute", shift.startMinute)
                .put("endMinute", shift.endMinute)
                .put("breakMinutes", shift.breakMinutes)
                .put("breakAfterMinutes", shift.breakAfterMinutes)
                .put("unpaidBreak", shift.unpaidBreak)
                .put("actual", shift.actual)
                .put("overtime", shift.overtime)
                .put("note", shift.note))
        }
        val result = JSONObject().put("format", FORMAT).put("version", VERSION)
            .put("salary", settings).put("shifts", data).toString(2)
        require(result.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Previše podataka za jednu kopiju (najviše 4 MB)."
        }
        return result
    }

    fun decode(text: String): ShiftBackup {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "Datoteka je prevelika. Ograničenje je 4 MB."
        }
        val root = JSONObject(text)
        require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
            "Ovo nije podržana Shiftora sigurnosna kopija."
        }
        val settings = root.getJSONObject("salary")
        fun percentage(name: String): Double = settings.getDouble(name).also {
            require(it.isFinite() && it in 0.0..300.0) { "Neispravna vrijednost: $name" }
        }
        val hourly = settings.getDouble("hourlyGross")
        require(hourly.isFinite() && hourly in 0.0..1_000_000.0) {
            "Neispravna bruto satnica."
        }
        val salary = SalarySettings(
            hourlyGross = hourly,
            nightPercent = percentage("nightPercent"),
            sundayPercent = percentage("sundayPercent"),
            holidayPercent = percentage("holidayPercent"),
            overtimePercent = percentage("overtimePercent"),
            nightStartHour = settings.getInt("nightStartHour"),
            nightEndHour = settings.getInt("nightEndHour")
        )
        val entries = root.getJSONArray("shifts")
        require(entries.length() <= MAX_SHIFTS) { "Sigurnosna kopija sadrži previše smjena." }
        val shifts = ArrayList<Shift>(entries.length())
        repeat(entries.length()) { index ->
            val item = entries.getJSONObject(index)
            val day = LocalDate.parse(item.getString("date"))
            require(day.year in 1900..2200) { "Neispravan datum u sigurnosnoj kopiji." }
            val note = item.getString("note")
            require(note.length <= 500) { "Bilješka je predugačka." }
            val breakAfter = item.getInt("breakAfterMinutes")
            require(breakAfter in 0..1440) { "Neispravan početak pauze." }
            shifts += Shift(
                date = day,
                kind = ShiftKind.valueOf(item.getString("kind")),
                startMinute = item.getInt("startMinute"),
                endMinute = item.getInt("endMinute"),
                breakMinutes = item.getInt("breakMinutes"),
                breakAfterMinutes = breakAfter,
                unpaidBreak = item.getBoolean("unpaidBreak"),
                actual = item.getBoolean("actual"),
                overtime = item.getBoolean("overtime"),
                note = note
            )
        }
        return ShiftBackup(shifts, salary)
    }
}
