package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import java.time.LocalDate

private fun parseClock(value: String): Int? {
    val split = value.trim().split(":")
    if (split.size != 2) return null
    val hour = split[0].toIntOrNull() ?: return null
    val minute = split[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

@Composable
fun ShiftEditorDialog(
    entry: Shift,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var kind by remember(entry) { mutableStateOf(entry.kind) }
    var date by remember(entry) { mutableStateOf(entry.date.toString()) }
    var start by remember(entry) { mutableStateOf(clock(entry.startMinute)) }
    var end by remember(entry) { mutableStateOf(clock(entry.endMinute)) }
    var breakMin by remember(entry) { mutableStateOf(entry.breakMinutes.toString()) }
    var breakAfter by remember(entry) { mutableStateOf(entry.breakAfterMinutes.toString()) }
    var unpaid by remember(entry) { mutableStateOf(entry.unpaidBreak) }
    var actual by remember(entry) { mutableStateOf(entry.actual) }
    var overtime by remember(entry) { mutableStateOf(entry.overtime) }
    var note by remember(entry) { mutableStateOf(entry.note) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = runCatching {
        val parsedDate = LocalDate.parse(date)
        val startMinutes = parseClock(start) ?: kotlin.error("Početak mora biti HH:mm.")
        val endMinutes = parseClock(end) ?: kotlin.error("Završetak mora biti HH:mm.")
        val pause = breakMin.toIntOrNull() ?: kotlin.error("Pauza mora biti broj minuta.")
        val after = breakAfter.toIntOrNull() ?: kotlin.error("Početak pauze mora biti broj minuta.")
        if (pause !in 0..240 || after !in 0..1440) kotlin.error("Provjeri trajanje pauze.")
        entry.copy(date = parsedDate, kind = kind,
            startMinute = startMinutes, endMinute = endMinutes,
            breakMinutes = pause, breakAfterMinutes = after,
            unpaidBreak = unpaid, actual = actual, overtime = overtime, note = note.take(500))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(25.dp),
        title = { Text(if (entry.id == 0L) "Dodaj smjenu" else "Uredi smjenu") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 510.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vrsta smjene", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ShiftKind.entries) { option ->
                        FilterChip(selected = kind == option, onClick = {
                            kind = option
                            start = clock(option.start)
                            end = clock(option.end)
                        }, label = { Text(option.label) },
                            leadingIcon = { Icon(shiftIcon(option), contentDescription = null,
                                modifier = Modifier.size(17.dp)) })
                    }
                }
                Text("Datum", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val selected = LocalDate.parse(date)
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                            selected.year, selected.monthValue - 1, selected.dayOfMonth
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(LocalDate.parse(date).dateLabel())
                }
                if (kind != ShiftKind.OFF) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Početak", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val minute = parseClock(start) ?: 0
                                    TimePickerDialog(context,
                                        { _, h, m -> start = clock(h * 60 + m) },
                                        minute / 60, minute % 60, true).show()
                                }
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text(start)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Završetak", style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val minute = parseClock(end) ?: 0
                                    TimePickerDialog(context,
                                        { _, h, m -> end = clock(h * 60 + m) },
                                        minute / 60, minute % 60, true).show()
                                }
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text(end)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(value = breakMin, onValueChange = { breakMin = it },
                            label = { Text("Pauza (min)") }, singleLine = true,
                            modifier = Modifier.weight(1f))
                        OutlinedTextField(value = breakAfter, onValueChange = { breakAfter = it },
                            label = { Text("Pauza nakon (min)") }, singleLine = true,
                            modifier = Modifier.weight(1f))
                    }
                    OptionSwitch("Pauza nije plaćena", unpaid) { unpaid = it }
                    OptionSwitch("Smjena je odrađena", actual) { actual = it }
                    OptionSwitch("Označi kao prekovremeni rad", overtime) { overtime = it }
                }
                OutlinedTextField(value = note, onValueChange = { note = it.take(500) },
                    label = { Text("Bilješka") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 3)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                if (entry.id != 0L) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Obriši ovu smjenu", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                valid.onSuccess {
                    onSave(it)
                    onDismiss()
                }.onFailure { error = "Provjeri datum, vrijeme i pauzu." }
            }, enabled = valid.isSuccess) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Obrisati smjenu?") },
            text = { Text("Ova se radnja ne može poništiti.") },
            confirmButton = {
                TextButton(onClick = { onDelete(entry.id); confirmDelete = false; onDismiss() }) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Odustani") } }
        )
    }
}

@Composable
private fun OptionSwitch(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun RotationDialog(onDismiss: () -> Unit, onGenerate: (LocalDate, List<ShiftKind>, Int) -> Unit) {
    val presets = listOf(
        "J–P–N–S" to listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF),
        "2+2+2+2" to listOf(ShiftKind.MORNING, ShiftKind.MORNING,
            ShiftKind.AFTERNOON, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF),
        "5 radnih + 2 slobodna" to listOf(ShiftKind.MORNING, ShiftKind.MORNING,
            ShiftKind.MORNING, ShiftKind.MORNING, ShiftKind.MORNING, ShiftKind.OFF, ShiftKind.OFF),
        "Noćna + 2 slobodna" to listOf(ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF)
    )
    var selection by remember { mutableIntStateOf(0) }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var days by remember { mutableStateOf("90") }
    val parsedStart = runCatching { LocalDate.parse(start) }.getOrNull()
    val parsedDays = days.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Automatski raspored") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Odaberi ciklus koji se ponavlja. Postojeći dani neće biti prebrisani.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                presets.forEachIndexed { index, pair ->
                    Row(Modifier.fillMaxWidth().clickable { selection = index },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selection == index, onClick = { selection = index })
                        Text(pair.first)
                    }
                }
                OutlinedTextField(start, onValueChange = { start = it },
                    label = { Text("Od datuma (GGGG-MM-DD)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(days, onValueChange = { days = it },
                    label = { Text("Broj dana (1–366)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Text("Ciklus se računa od prvog dana. Slobodni dani također se prikazuju u kalendaru.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedStart != null && parsedDays != null) {
                        onGenerate(parsedStart, presets[selection].second, parsedDays)
                        onDismiss()
                    }
                }, enabled = parsedStart != null && parsedDays != null && parsedDays in 1..366
            ) { Text("Generiraj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
fun SalarySettingsDialog(current: SalarySettings, onDismiss: () -> Unit, onSave: (SalarySettings) -> Unit) {
    var hourly by remember(current) { mutableStateOf(current.hourlyGross.toString()) }
    var night by remember(current) { mutableStateOf(current.nightPercent.toString()) }
    var sunday by remember(current) { mutableStateOf(current.sundayPercent.toString()) }
    var holiday by remember(current) { mutableStateOf(current.holidayPercent.toString()) }
    var overtime by remember(current) { mutableStateOf(current.overtimePercent.toString()) }
    fun number(text: String) = text.replace(',', '.').toDoubleOrNull()
    val parsed = runCatching {
        SalarySettings(
            hourlyGross = number(hourly) ?: kotlin.error("Satnica"),
            nightPercent = number(night) ?: kotlin.error("Noćni"),
            sundayPercent = number(sunday) ?: kotlin.error("Nedjelje"),
            holidayPercent = number(holiday) ?: kotlin.error("Blagdani"),
            overtimePercent = number(overtime) ?: kotlin.error("Prekovremeni")
        )
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Postavke plaće") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unesi svoju ugovorenu bruto satnicu i postotke dodataka. Početne vrijednosti samo su primjeri.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(hourly, onValueChange = { hourly = it },
                    label = { Text("Bruto satnica (€/h)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(night, onValueChange = { night = it },
                    label = { Text("Dodatak za noćni rad (%)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sunday, onValueChange = { sunday = it },
                    label = { Text("Dodatak za nedjelje (%)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(holiday, onValueChange = { holiday = it },
                    label = { Text("Dodatak za blagdane (%)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(overtime, onValueChange = { overtime = it },
                    label = { Text("Dodatak za prekovremeni rad (%)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (parsed != null) { onSave(parsed); onDismiss() } },
                enabled = parsed != null) { Text("Spremi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } }
    )
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val titles = listOf("Planiraj smjene", "Prati svoje sate", "Procijeni plaću")
    val descriptions = listOf(
        "Lako dodaj, uređuj i pregledaj svoj raspored u kalendaru.",
        "Razlikuj planirano od odrađenog, računaj noćne sate i prekovremeni rad.",
        "Procijeni bruto plaću prema satnici i dodacima koje sam odrediš."
    )
    val icons = listOf(Icons.Default.CalendarMonth, Icons.Default.Schedule, Icons.Default.BarChart)
    Column(
        Modifier.fillMaxSize().background(Palette.DeepNavy)
            .statusBarsPadding().navigationBarsPadding().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onComplete) { Text("Preskoči", color = Color.White) }
        }
        Spacer(Modifier.weight(.6f))
        BrandMark(Modifier.size(112.dp), darkSurface = true)
        Spacer(Modifier.height(17.dp))
        Text("Shiftora", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        Text("Moje smjene. Moji sati.", color = Color(0xFFB8CEE9))
        Spacer(Modifier.weight(.65f))
        Box(Modifier.size(130.dp).background(Palette.Blue.copy(alpha = .14f),
            RoundedCornerShape(36.dp)), contentAlignment = Alignment.Center) {
            Icon(icons[step], contentDescription = null, tint = Color(0xFF5EEAD4),
                modifier = Modifier.size(74.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(titles[step], color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(descriptions[step], textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color(0xFFB8CEE9), modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(.9f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(if (step == i) 18.dp else 7.dp, 7.dp)
                    .background(if (step == i) Palette.Blue else Color(0xFF60718C),
                        RoundedCornerShape(6.dp)))
            }
        }
        Spacer(Modifier.height(25.dp))
        BlueAction(if (step == 2) "Započni" else "Dalje",
            { if (step == 2) onComplete() else step++ }, Modifier.fillMaxWidth(),
            Icons.Default.ArrowForward)
        Spacer(Modifier.height(15.dp))
        Text("Bez prijave i bez interneta", color = Color(0xFFB8CEE9),
            style = MaterialTheme.typography.bodySmall)
    }
}
