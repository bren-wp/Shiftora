package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.domain.ShiftScheduleValidator
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
    existing: List<Shift>,
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
        val result = entry.copy(date = parsedDate, kind = kind,
            startMinute = startMinutes, endMinute = endMinutes,
            breakMinutes = pause, breakAfterMinutes = after,
            unpaidBreak = kind.isWork && unpaid,
            actual = kind.isWork && actual,
            overtime = kind.isWork && overtime, note = note.take(500))
        ShiftScheduleValidator.validateInsertion(existing, result)
        result
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(25.dp),
        title = { Text(if (entry.id == 0L) "Dodaj smjenu" else "Uredi smjenu") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = adaptiveDialogBodyHeight(510.dp)).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Vrsta smjene", fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ShiftKind.calendarOrder) { option ->
                        FilterChip(selected = kind == option, onClick = {
                            kind = option
                            start = clock(option.start)
                            end = clock(option.end)
                        }, label = { Text("${option.code} · ${option.label}") },
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
                if (kind.hasTime) {
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
                    if (kind.isWork) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(value = breakMin, onValueChange = { breakMin = it.filter(Char::isDigit).take(3) },
                            label = { Text("Pauza (min)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                        OutlinedTextField(value = breakAfter, onValueChange = { breakAfter = it.filter(Char::isDigit).take(4) },
                            label = { Text("Pauza nakon (min)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                    }
                    OptionSwitch("Pauza nije plaćena", unpaid) { unpaid = it }
                    OptionSwitch("Smjena je odrađena", actual) { actual = it }
                    OptionSwitch("Označi kao prekovremeni rad", overtime) { overtime = it }
                    } else {
                        Text("Pripravnost se evidentira s vremenom, ali se ne pribraja " +
                            "odrađenim satima ni procijenjenoj plaći.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it.take(500) },
                    label = { Text("Bilješka") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 3)
                val validationError = valid.exceptionOrNull()?.message
                if (validationError != null) {
                    Text(validationError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                if (entry.id != 0L) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Premjesti u arhivu", color = MaterialTheme.colorScheme.error)
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
            title = { Text("Arhivirati smjenu?") },
            text = { Text("Smjena će nestati iz aktivnog kalendara, ali ostaje sigurno spremljena u lokalnoj arhivi. Možeš je vratiti u Više → Arhiva rasporeda. Ne briše se trajno.") },
            confirmButton = {
                TextButton(onClick = { onDelete(entry.id); confirmDelete = false; onDismiss() }) {
                    Text("Arhiviraj", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Odustani") } }
        )
    }
}

@Composable
private fun OptionSwitch(text: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
        .toggleable(value = value, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = value, onCheckedChange = null)
    }
}

@Composable
fun RotationDialog(onDismiss: () -> Unit, onGenerate: (LocalDate, List<ShiftKind>, Int) -> Unit) {
    val context = LocalContext.current
    val presets = listOf(
        "D–N–SD–SD (turnus)" to listOf(ShiftKind.DAY, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF),
        "J–P–N–SD" to listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF),
        "2+2+2+2" to listOf(ShiftKind.MORNING, ShiftKind.MORNING,
            ShiftKind.AFTERNOON, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF),
        "5 radnih + 2 slobodna" to listOf(ShiftKind.MORNING, ShiftKind.MORNING,
            ShiftKind.MORNING, ShiftKind.MORNING, ShiftKind.MORNING, ShiftKind.OFF, ShiftKind.OFF),
        "Noćna + 2 slobodna" to listOf(ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF)
    )
    var selection by remember { mutableIntStateOf(0) }
    val customPattern = remember {
        mutableStateListOf(ShiftKind.DAY, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF)
    }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var days by remember { mutableStateOf("90") }
    val parsedStart = runCatching { LocalDate.parse(start) }.getOrNull()
    val parsedDays = days.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Automatski raspored") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = adaptiveDialogBodyHeight(480.dp)).verticalScroll(rememberScrollState()),
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
                Row(Modifier.fillMaxWidth().clickable { selection = presets.size },
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selection == presets.size,
                        onClick = { selection = presets.size })
                    Text("Moj prilagođeni ciklus")
                }
                if (selection == presets.size) {
                    Text("Dodirni vrstu smjene za promjenu. Redoslijed se ponavlja od početka.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(customPattern.size) { index ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        val kind = customPattern[index]
                                        customPattern[index] =
                                            ShiftKind.calendarOrder[(ShiftKind.calendarOrder.indexOf(kind) + 1) % ShiftKind.calendarOrder.size]
                                    },
                                    label = { Text("${index + 1}. ${customPattern[index].label}") },
                                    leadingIcon = {
                                        Icon(shiftIcon(customPattern[index]), contentDescription = null,
                                            modifier = Modifier.size(15.dp))
                                    }
                                )
                                if (customPattern.size > 1) {
                                    IconButton(onClick = { customPattern.removeAt(index) },
                                        modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Ukloni ${index + 1}. dan",
                                            modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { customPattern.add(ShiftKind.MORNING) },
                        enabled = customPattern.size < 28,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Dodaj dan u ciklus (${customPattern.size}/28)")
                    }
                }
                OutlinedTextField(start, onValueChange = { start = it.take(10) },
                    label = { Text("Od datuma (GGGG-MM-DD)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val selected = parsedStart ?: LocalDate.now()
                            DatePickerDialog(context, { _, y, m, d ->
                                start = LocalDate.of(y, m + 1, d).toString()
                            }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Odaberi početni datum")
                        }
                    })
                OutlinedTextField(days, onValueChange = { days = it.filter(Char::isDigit).take(3) },
                    label = { Text("Broj dana (1–366)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        onGenerate(parsedStart, if (selection == presets.size) customPattern.toList() else presets[selection].second, parsedDays)
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
    var nightFrom by remember(current) { mutableStateOf(current.nightStartHour.toString()) }
    var nightUntil by remember(current) { mutableStateOf(current.nightEndHour.toString()) }
    fun number(text: String) = text.replace(',', '.').toDoubleOrNull()
    val parsed = runCatching {
        SalarySettings(
            hourlyGross = number(hourly) ?: kotlin.error("Satnica"),
            nightPercent = number(night) ?: kotlin.error("Noćni"),
            sundayPercent = number(sunday) ?: kotlin.error("Nedjelje"),
            holidayPercent = number(holiday) ?: kotlin.error("Blagdani"),
            overtimePercent = number(overtime) ?: kotlin.error("Prekovremeni"),
            nightStartHour = nightFrom.toIntOrNull() ?: kotlin.error("Početak noći"),
            nightEndHour = nightUntil.toIntOrNull() ?: kotlin.error("Kraj noći")
        )
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Postavke plaće") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = adaptiveDialogBodyHeight(450.dp)).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unesi svoju ugovorenu bruto satnicu i postotke dodataka. Početne vrijednosti samo su primjeri.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(hourly, onValueChange = { hourly = it },
                    label = { Text("Bruto satnica (€/h)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(night, onValueChange = { night = it },
                    label = { Text("Dodatak za noćni rad (%)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(sunday, onValueChange = { sunday = it },
                    label = { Text("Dodatak za nedjelje (%)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(holiday, onValueChange = { holiday = it },
                    label = { Text("Dodatak za blagdane (%)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(overtime, onValueChange = { overtime = it },
                    label = { Text("Dodatak za prekovremeni rad (%)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text("Razdoblje noćnog rada (24-satni format)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(nightFrom, onValueChange = { nightFrom = it },
                        label = { Text("Od sata") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(nightUntil, onValueChange = { nightUntil = it },
                        label = { Text("Do sata") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f))
                }
                Text("Primjer: od 22 do 6. Satnica i dodaci nisu službeni obračun.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (parsed == null) {
                    Text("Provjeri iznose (0–300 %), bruto satnicu i vrijeme noćnog rada. " +
                        "Početak i završetak noćnog razdoblja moraju biti različiti.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
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
fun OnboardingScreen(onComplete: () -> Unit, onStepChange: (Int) -> Unit = {}) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(step) { onStepChange(step) }
    if (step == 0) {
        ShiftoraWelcomeScreen(onStart = { step = 1 }, onSkip = onComplete)
        return
    }
    val page = step - 1
    val compact = LocalConfiguration.current.screenHeightDp < 680
    val titles = listOf("Planiraj smjene", "Prati svoje sate", "Procijeni plaću")
    val descriptions = listOf(
        "Lako dodaj, uredi i pregledaj svoje smjene uz pregledan kalendar.",
        "Evidentiraj odrađene sate, noćni rad i prekovremene sate na jednom mjestu.",
        "Procijeni bruto zaradu prema svojoj satnici i postavkama dodataka."
    )
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = if (compact) 8.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(Modifier.size(42.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text("Shiftora", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Moje smjene. Moji sati.", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onComplete) { Text("Preskoči") }
        }
        Spacer(Modifier.height(if (compact) 15.dp else 40.dp))
        Box(Modifier.fillMaxWidth().height(if (compact) 215.dp else 290.dp),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(if (compact) 195.dp else 255.dp)
                .background(Palette.BlueLight, RoundedCornerShape(60.dp)))
            Box(Modifier.fillMaxWidth(.9f)
                .height(if (compact) 189.dp else 238.dp)
                .background(Color.White, RoundedCornerShape(27.dp))
                .padding(15.dp)) {
                when (page) {
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null,
                                tint = Palette.Blue, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Moj kalendar", fontWeight = FontWeight.Bold,
                                color = Palette.Navy, fontSize = 16.sp)
                        }
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned").forEach {
                                Text(it, Modifier.weight(1f), color = Palette.Secondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 9.sp)
                            }
                        }
                        listOf(0, 1, 2).forEach { week ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (0..6).forEach { day ->
                                    val kind = listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON,
                                        ShiftKind.NIGHT, ShiftKind.OFF)[(day + week) % 4]
                                    Column(Modifier.weight(1f)
                                        .height(if (compact) 37.dp else 46.dp)
                                        .background(shiftBg(kind), RoundedCornerShape(9.dp)),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text((8 + week * 7 + day).toString(),
                                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                            color = Palette.Navy)
                                        Icon(shiftIcon(kind), contentDescription = null,
                                            tint = shiftColor(kind),
                                            modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null,
                                tint = Palette.Blue)
                            Spacer(Modifier.width(9.dp))
                            Text("Tvoje radne smjene", fontWeight = FontWeight.Bold,
                                color = Palette.Navy)
                        }
                        listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT).forEach { kind ->
                            Row(Modifier.fillMaxWidth()
                                .background(shiftBg(kind), RoundedCornerShape(13.dp))
                                .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(shiftIcon(kind), contentDescription = null,
                                    tint = shiftColor(kind), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(kind.label + " smjena", fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold, color = Palette.Navy)
                                    Text(clock(kind.start) + " – " + clock(kind.end),
                                        fontSize = 11.sp, color = Palette.Secondary)
                                }
                                Text("8h", fontWeight = FontWeight.Bold,
                                    color = Palette.Navy)
                            }
                        }
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null,
                                tint = Palette.Mint)
                            Spacer(Modifier.width(8.dp))
                            Text("Procjena bruto plaće", fontWeight = FontWeight.Bold,
                                color = Palette.Navy, fontSize = 15.sp)
                        }
                        Text("Na temelju tvojih smjena", fontSize = 12.sp,
                            color = Palette.Secondary)
                        Row(Modifier.fillMaxWidth().height(110.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Bottom) {
                            listOf(32, 50, 66, 83, 105).forEachIndexed { i, height ->
                                Box(Modifier.weight(1f).height(height.dp)
                                    .background(if (i < 2) Palette.Blue else Palette.Mint,
                                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)))
                            }
                        }
                        Text("Satnica + dodatak za smjene", color = Palette.Navy,
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(if (compact) 16.dp else 28.dp))
        Text(titles[page], color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(descriptions[page],
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(if (compact) 20.dp else 36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                Box(Modifier.size(if (page == i) 18.dp else 8.dp, 8.dp)
                    .background(if (page == i) Palette.Blue else Color(0xFFCBD5E1),
                        RoundedCornerShape(8.dp)))
            }
        }
        Spacer(Modifier.height(20.dp))
        BlueAction(if (step == 3) "Započni" else "Dalje",
            { if (step == 3) onComplete() else step++ },
            Modifier.fillMaxWidth(), Icons.Default.ArrowForward)
        Spacer(Modifier.height(12.dp))
        Text("Bez prijave i interneta · tvoji podaci ostaju na uređaju",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

/** Dark welcome page is rendered in native Compose, not a static marketing screenshot. */
@Composable
private fun ShiftoraWelcomeScreen(onStart: () -> Unit, onSkip: () -> Unit) {
    val compact = LocalConfiguration.current.screenHeightDp < 680
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(
                Palette.DeepNavy, Palette.Navy, Color(0xFF06294C), Palette.DeepNavy
            )))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = if (compact) 12.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Preskoči", color = Color(0xFFC9DAF5))
            }
        }
        Spacer(Modifier.height(if (compact) 36.dp else 76.dp))
        Text("Tvoj rad. Tvoja pravila.",
            color = Color(0xFFB8D2F4), fontWeight = FontWeight.Medium, fontSize = 18.sp)
        Spacer(Modifier.height(if (compact) 22.dp else 44.dp))
        BrandMark(Modifier.size(if (compact) 128.dp else 168.dp), darkSurface = true)
        Spacer(Modifier.height(12.dp))
        Text("Shiftora", color = Color.White, fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) 45.sp else 52.sp, lineHeight = 58.sp)
        Text("Moje smjene. Moji sati.", color = Color(0xFFD8E6F7), fontSize = 16.sp)
        Spacer(Modifier.height(if (compact) 45.dp else 82.dp))
        Text("Dobro došao u Shiftoru!", color = Color.White,
            fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Planiraj smjene, prati radne sate i procijeni bruto plaću. " +
            "Sve na jednom mjestu, jednostavno i bez registracije.",
            color = Color(0xFFD0DCF0), fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        BlueAction("Započni", onStart, Modifier.fillMaxWidth(), Icons.Default.ArrowForward)
        Spacer(Modifier.height(12.dp))
        Text("Radi bez interneta · tvoji podaci ostaju na uređaju",
            color = Color(0xFF9BB4D8), fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
