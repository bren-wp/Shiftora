package com.brendigo.shiftora.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {
    @Test fun localeFriendlyExportUsesSemicolonsAndUtf8Bom() {
        val shift = Shift(date = LocalDate.of(2026, 10, 17),
            kind = ShiftKind.NIGHT, note = "Rad; noć")
        val csv = CsvExporter.encode(listOf(shift))
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"Rad; noć\""))
        assertTrue(csv.contains("\"22:00\";\"06:00\""))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test fun escapedQuotesAndNewlinesDoNotCorruptRows() {
        val shift = Shift(date = LocalDate.of(2026, 10, 17),
            kind = ShiftKind.MORNING, note = "Raspored \"A\"\nNova smjena")
        val csv = CsvExporter.encode(listOf(shift))
        assertTrue(csv.contains("\"Raspored \"\"A\"\"\nNova smjena\""))
    }

    @Test fun formulasAreNeutralizedEvenAfterWhitespace() {
        val notes = listOf("=HYPERLINK(1)", "  +1+2", "\t@SUM(1,2)", "-42", "Redovno")
        val csv = CsvExporter.encode(notes.mapIndexed { idx, note ->
            Shift(date = LocalDate.of(2026, 10, 17).plusDays(idx.toLong()),
                kind = ShiftKind.CUSTOM, note = note)
        })
        assertTrue(csv.contains("\"'=HYPERLINK(1)\""))
        assertTrue(csv.contains("\"'  +1+2\""))
        assertTrue(csv.contains("\"'\t@SUM(1,2)\""))
        assertTrue(csv.contains("\"'-42\""))
        assertTrue(csv.contains("\"Redovno\""))
    }
}
