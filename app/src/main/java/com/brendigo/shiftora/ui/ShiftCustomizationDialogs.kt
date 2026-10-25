package com.brendigo.shiftora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.domain.MonthlyFund
import java.time.YearMonth

private val swatches = listOf(
    0xFF2563EB.toInt(), 0xFF0EA5E9.toInt(), 0xFF0891B2.toInt(),
    0xFF10B981.toInt(), 0xFF65A30D.toInt(), 0xFFF59E0B.toInt(),
    0xFFEA580C.toInt(), 0xFFEF4444.toInt(), 0xFFDB2777.toInt(),
    0xFF9333EA.toInt(), 0xFF6D43E9.toInt(), 0xFF64748B.toInt(),
    0xFF334155.toInt(), 0xFF0F766E.toInt(), 0xFF854D0E.toInt()
)

/** Allows changing every shift code color without modifying historical shifts. */
@Composable
fun ShiftColorSettingsDialog(
    colors: Map<ShiftKind, Int>,
    onChange: (ShiftKind, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(ShiftKind.DAY) }
    var hex by remember(selected) { mutableStateOf("") }
    val parsed = remember(hex) {
        val raw = hex.trim().removePrefix("#")
        if (raw.matches(Regex("[a-fA-F0-9]{6}"))) raw.toLongOrNull(16)
            ?.let { (it or 0xFF000000L).toInt() }
        else null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boje i oznake smjena") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Odaberi oznaku. Boja se odmah primjenjuje na cijeli kalendar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                ShiftKind.calendarOrder.chunked(2).forEach { kinds ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        kinds.forEach { kind ->
                            val active = selected == kind
                            val color = colors[kind]?.let(::Color) ?: shiftColor(kind)
                            Row(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant)
                                    .heightIn(min = 56.dp)
                                    .semantics {
                                        contentDescription = "${kind.label}, oznaka ${kind.code}"
                                        this.selected = active
                                    }
                                    .clickable { selected = kind }
                                    .padding(horizontal = 9.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(19.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(7.dp))
                                Text(kind.code, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(5.dp))
                                Text(kind.label, fontSize = 12.sp, maxLines = 2,
                                    lineHeight = 14.sp)
                            }
                        }
                        if (kinds.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text("Nova boja za ${selected.code} — ${selected.label}",
                    fontWeight = FontWeight.SemiBold)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val swatchColumns = if (maxWidth < 270.dp ||
                        LocalDensity.current.fontScale > 1.3f) 4 else 5
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        swatches.chunked(swatchColumns).forEach { group ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                group.forEach { value ->
                            Box(Modifier.size(48.dp).clip(CircleShape)
                                .background(Color(value))
                                .border(3.dp,
                                    if (colors[selected] == value) MaterialTheme.colorScheme.onSurface
                                    else Color.Transparent, CircleShape)
                                .semantics {
                                    contentDescription = "Boja " +
                                        String.format("#%06X", value and 0xFFFFFF)
                                }
                                .clickable { onChange(selected, value) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colors[selected] == value) {
                                    Icon(Icons.Default.Check, contentDescription = null,
                                        tint = if (Color(value).luminance() > .3f) Color.Black
                                            else Color.White,
                                        modifier = Modifier.size(22.dp))
                                }
                            }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it.take(7) },
                    label = { Text("Vlastita HEX boja (#RRGGBB)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    supportingText = {
                        Text("Možeš unijeti bilo koju šesteroznamenkastu RGB boju.")
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onChange(selected, null); hex = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Vrati zadanu") }
                    Button(
                        enabled = parsed != null,
                        onClick = { parsed?.let { onChange(selected, it) } },
                        modifier = Modifier.weight(1f)
                    ) { Text("Primijeni HEX") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Gotovo") }
        }
    )
}

@Composable
fun MonthlyFundEditor(
    month: YearMonth,
    overrideMinutes: Int?,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val standard = MonthlyFund.standardMinutes(month)
    var hoursText by remember(month, overrideMinutes) {
        mutableStateOf(((overrideMinutes ?: standard) / 60).toString())
    }
    var minutesText by remember(month, overrideMinutes) {
        mutableStateOf(((overrideMinutes ?: standard) % 60).toString())
    }
    val parsedFund = MonthlyFund.parseFund(hoursText, minutesText)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fond sati — ${month.atDay(1).monthLabel()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("Standardni fond: ${standard / 60} h. Računa se 8 sati po radnom danu, " +
                    "bez vikenda i hrvatskih blagdana. Fond po ugovoru može biti drukčiji.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Sati") },
                        suffix = { Text("h") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = parsedFund == null,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        suffix = { Text("min") },
                        singleLine = true,
                        isError = parsedFund == null,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (parsedFund == null) {
                    Text("Unesi ispravan fond (najviše 744 h, minute od 0 do 59).",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("Godišnji, bolovanje i plaćeni dopust evidentiraju se odvojeno " +
                    "od odrađenih sati. Ovaj fond nije službeni obračun plaće.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = {
                parsedFund?.let { onSave(it); onDismiss() }
            }, enabled = parsedFund != null) { Text("Spremi") }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null); onDismiss() }) {
                Text("Zadani fond")
            }
        }
    )
}
