package com.brendigo.shiftora.data

/**
 * UTF-8 CSV for Croatian spreadsheet locale. Use semicolons (Excel regional settings),
 * quote every cell and prevent spreadsheet formula injection in user-controlled notes.
 */
object CsvExporter {
    fun encode(shifts: List<Shift>): String {
        fun clock(minutes: Int) = "%02d:%02d".format(java.util.Locale.ROOT, minutes / 60, minutes % 60)
        fun cell(raw: String): String {
            val leading = raw.trimStart(' ', '\t', '\r', '\n', '\uFEFF')
            val suspicious = leading.firstOrNull() in listOf('=', '+', '-', '@') ||
                raw.firstOrNull() in listOf('\t', '\r', '\n')
            val value = if (suspicious) "'$raw" else raw
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        val header = listOf(
            "Datum", "Oznaka", "Vrsta", "Početak", "Kraj", "Pauza (min)",
            "Neplaćena pauza", "Odrađeno", "Prekovremeno", "Bilješka"
        )
        val records = shifts.map { shift ->
            listOf(
                shift.date.toString(), shift.kind.code, shift.kind.label,
                clock(shift.startMinute), clock(shift.endMinute),
                shift.breakMinutes.toString(), shift.unpaidBreak.toString(),
                shift.actual.toString(), shift.overtime.toString(), shift.note
            )
        }
        return "\uFEFF" + (listOf(header) + records).joinToString("\r\n") { row ->
            row.joinToString(";") { cell(it) }
        } + "\r\n"
    }
}
