package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.brendigo.shiftora.BuildConfig
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.domain.WorkCalculator
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HomeScreen(
    shifts: List<Shift>,
    salary: SalarySettings,
    onAdd: (LocalDate) -> Unit,
    onEdit: (Shift) -> Unit,
    onTrack: (LocalDate) -> Unit,
    onTab: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekTotal = remember(shifts, salary, weekStart) {
        WorkCalculator.calculatePeriod(shifts, salary, weekStart, weekStart.plusDays(7))
    }
    val monthTotal = remember(shifts, salary, today) {
        val month = YearMonth.from(today)
        WorkCalculator.calculatePeriod(shifts, salary, month.atDay(1), month.plusMonths(1).atDay(1))
    }
    val dailyTotals = remember(shifts, salary, weekStart) {
        (0..6).map { day ->
            val start = weekStart.plusDays(day.toLong())
            WorkCalculator.calculatePeriod(shifts, salary, start, start.plusDays(1))
        }
    }
    val todayShift = shifts.firstOrNull { it.date == today }
    val upcoming = shifts.filter { !it.date.isBefore(today.plusDays(1)) }
        .sortedWith(compareBy<Shift> { it.date }.thenBy { it.startMinute }).take(5)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader(onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(3.dp))
                Text("Danas je", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                Text(today.dateLabel(), style = MaterialTheme.typography.headlineLarge)
                Text(today.weekdayLabel().replaceFirstChar { it.uppercase(Locale("hr", "HR")) },
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth().clickable {
                    if (todayShift == null) onAdd(today) else onEdit(todayShift)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(66.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (todayShift != null) shiftBg(todayShift.kind) else Palette.BlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (todayShift != null) shiftIcon(todayShift.kind) else Icons.Default.Today,
                                contentDescription = null,
                                tint = if (todayShift != null) shiftColor(todayShift.kind) else Palette.Blue,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("DANAŠNJA SMJENA", color = Palette.Amber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(todayShift?.kind?.label ?: "Dodaj smjenu", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (todayShift == null) "Nema unosa za danas"
                                else if (todayShift.kind == ShiftKind.OFF) "Slobodan dan"
                                else "${clock(todayShift.startMinute)} – ${clock(todayShift.endMinute)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                            )
                        }
                        if (todayShift != null && todayShift.kind != ShiftKind.OFF) {
                            Text(minutesLabel(WorkCalculator.durationMinutes(todayShift)),
                                fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Palette.Secondary)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SurfaceCard(Modifier.weight(1f).clickable { onTab(2) }, color = Palette.BlueLight) {
                        Text("Ovaj tjedan", color = Palette.Ink, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Text(minutesLabel(weekTotal.paidMinutes), fontSize = 29.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Text("Planirano i odrađeno", color = Palette.Secondary, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                            repeat(7) { day ->
                                val t = dailyTotals[day]
                                val ratio = (t.paidMinutes / 600f).coerceIn(.08f, 1f)
                                Box(Modifier.weight(1f).height((38f * ratio).dp)
                                    .clip(RoundedCornerShape(4.dp)).background(if (t.paidMinutes > 0) Palette.Blue else Color(0xFFBED1EF)))
                            }
                        }
                    }
                    SurfaceCard(Modifier.weight(1f).clickable { onTab(3) }, color = Palette.MintLight) {
                        Text("Procjena bruto plaće", color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(13.dp))
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Palette.Mint, modifier = Modifier.size(27.dp))
                        Text(euros(monthTotal.grossCents), fontWeight = FontWeight.Bold, color = Palette.Ink,
                            fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(YearMonth.from(today).atDay(1).monthLabel(), color = Palette.Secondary, fontSize = 11.sp)
                    }
                }
            }
            item {
                SectionTitle("Brze akcije")
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 340.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BlueAction("Dodaj smjenu", { onAdd(today) }, Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onTrack(today) }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Evidentiraj sate", maxLines = 2)
                                }
                                OutlinedButton(onClick = { onTab(3) }, modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Izračunaj plaću", maxLines = 2)
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            BlueAction("Dodaj smjenu", { onAdd(today) }, Modifier.weight(1f))
                            OutlinedButton(onClick = { onTrack(today) }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 7.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Evidentiraj sate", fontSize = 12.sp, maxLines = 2)
                            }
                            OutlinedButton(onClick = { onTab(3) }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 7.dp)) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Izračunaj plaću", fontSize = 12.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Nadolazeće smjene", "Prikaži sve") { onTab(1) } }
            if (upcoming.isEmpty()) {
                item {
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Text("Još nema budućih smjena.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        BlueAction("Isplaniraj smjene", { onTab(1) })
                    }
                }
            } else items(upcoming, key = { it.id }) { shift ->
                ShiftRow(shift, onClick = { onEdit(shift) })
            }
        }
    }
}

@Composable
fun CalendarScreen(
    shifts: List<Shift>, onAdd: (LocalDate) -> Unit,
    onEdit: (Shift) -> Unit, onRotation: () -> Unit,
    onSettings: () -> Unit
) {
    var shown by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var weekOnly by remember { mutableStateOf(false) }
    val first = shown.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val weeks = ((first.dayOfWeek.value - 1 + shown.lengthOfMonth() + 6) / 7)
    val calendarDays = (0 until weeks * 7).map { start.plusDays(it.toLong()) }
    val rows = if (weekOnly) {
        val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
        listOf((0..6).map { monday.plusDays(it.toLong()) })
    } else calendarDays.chunked(7)
    val dayShifts = shifts.filter { it.date == selected }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader(dark = true, onSettings = onSettings)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Palette.BlueLight).padding(3.dp)) {
                listOf("Mjesec", "Tjedan").forEachIndexed { index, label ->
                    val isSelected = weekOnly == (index == 1)
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { weekOnly = index == 1 }.padding(11.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Palette.Blue else Palette.Navy)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    shown = shown.minusMonths(1)
                    selected = shown.atDay(1)
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodni mjesec") }
                Text(shown.atDay(1).monthLabel(), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = {
                    shown = shown.plusMonths(1)
                    selected = shown.atDay(1)
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeći mjesec") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            rows.forEach { dates ->
                Row(Modifier.fillMaxWidth()) {
                    dates.forEach { date ->
                        val chosen = selected == date
                        val entry = shifts.firstOrNull { it.date == date }
                        val inMonth = YearMonth.from(date) == shown
                        val bg = if (chosen) Palette.Blue
                        else if (entry != null) shiftBg(entry.kind)
                        else Color.Transparent
                        Column(
                            Modifier.weight(1f).padding(2.dp).height(43.dp)
                                .clip(RoundedCornerShape(13.dp)).background(bg)
                                .clickable {
                                    selected = date
                                    shown = YearMonth.from(date)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(date.dayOfMonth.toString(),
                                color = if (chosen) Color.White else if (inMonth) MaterialTheme.colorScheme.onSurface else Palette.Secondary.copy(alpha = .45f),
                                fontWeight = if (chosen || date == LocalDate.now()) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp)
                            Spacer(Modifier.height(3.dp))
                            if (entry != null) {
                                Box(Modifier.size(7.dp).clip(CircleShape)
                                    .background(if (chosen) Color.White else shiftColor(entry.kind)))
                            } else Spacer(Modifier.height(7.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val kinds = listOf(ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF)
                if (maxWidth < 350.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        kinds.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth()) {
                                pair.forEach { kind ->
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(9.dp).clip(CircleShape).background(shiftColor(kind)))
                                        Text(kind.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        kinds.forEach { kind ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(shiftColor(kind)))
                                Text(kind.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(selected.dateLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                if (dayShifts.isEmpty()) {
                    Text("Za ovaj dan nema spremljene smjene.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    dayShifts.forEach { shift ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                                .background(shiftBg(shift.kind)), contentAlignment = Alignment.Center) {
                                Icon(shiftIcon(shift.kind), contentDescription = null,
                                    tint = shiftColor(shift.kind), modifier = Modifier.size(31.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("${shift.kind.label} smjena", style = MaterialTheme.typography.titleMedium)
                                Text(if (shift.kind == ShiftKind.OFF) "Slobodan dan"
                                    else "${clock(shift.startMinute)} – ${clock(shift.endMinute)} • ${minutesLabel(WorkCalculator.durationMinutes(shift))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Text(if (shift.actual) "Odrađeno" else "Planirano", color = Palette.Blue, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (shift.kind != ShiftKind.OFF) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailTile("Radno vrijeme", "${clock(shift.startMinute)}–${clock(shift.endMinute)}", Modifier.weight(1f))
                                DetailTile("Pauza", "${shift.breakMinutes} min", Modifier.weight(1f))
                                DetailTile("Bilješka", shift.note.ifBlank { "—" }, Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { onEdit(shift) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Uredi smjenu")
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                BlueAction("Dodaj smjenu", { onAdd(selected) }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRotation, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoMode, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Automatski ponovi raspored")
            }
        }
    }
}

@Composable
private fun DetailTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        RoundedCornerShape(12.dp)).padding(9.dp)) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatsScreen(shifts: List<Shift>, settings: SalarySettings, onSettings: () -> Unit) {
    var shown by remember { mutableStateOf(YearMonth.now()) }
    var period by remember { mutableIntStateOf(1) }
    var weekAnchor by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))) }
    val totals = remember(shifts, settings, period, shown, weekAnchor) {
        when (period) {
            0 -> WorkCalculator.calculatePeriod(shifts, settings, weekAnchor, weekAnchor.plusDays(7))
            2 -> WorkCalculator.calculatePeriod(shifts, settings, LocalDate.of(shown.year, 1, 1), LocalDate.of(shown.year + 1, 1, 1))
            else -> WorkCalculator.calculatePeriod(shifts, settings, shown.atDay(1), shown.plusMonths(1).atDay(1))
        }
    }
    val byType = listOf(
        Triple("Jutarnja", totals.morningMinutes, Palette.Amber),
        Triple("Popodnevna", totals.afternoonMinutes, Palette.Blue),
        Triple("Noćna", totals.nightShiftMinutes, Palette.Purple),
        Triple("Ostale", (totals.paidMinutes - totals.morningMinutes - totals.afternoonMinutes - totals.nightShiftMinutes).coerceAtLeast(0), Palette.Coral)
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader("Statistika i plaća", "Tvoj rad u brojkama. Veća kontrola.", dark = true, onSettings = onSettings)
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.BlueLight)) {
                    listOf("Tjedan", "Mjesec", "Godina").forEachIndexed { index, name ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                            .background(if (period == index) Color(0xFFD0E2FF) else Color.Transparent)
                            .clickable { period = index }.padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(name, color = Palette.Navy, fontWeight = if (period == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (period == 0) weekAnchor = weekAnchor.minusWeeks(1) else shown = shown.minusMonths(if (period == 2) 12 else 1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodno razdoblje")
                    }
                    Text(if (period == 2) shown.year.toString()
                        else if (period == 0) "${weekAnchor.dateLabel()} – ${weekAnchor.plusDays(6).dateLabel()}"
                        else shown.atDay(1).monthLabel(),
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (period == 0) weekAnchor = weekAnchor.plusWeeks(1) else shown = shown.plusMonths(if (period == 2) 12 else 1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeće razdoblje")
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(126.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize()) {
                                val arcWidth = 22.dp.toPx()
                                val all = totals.paidMinutes.toFloat()
                                if (all <= 0f) drawArc(Palette.BlueLight, 0f, 360f,
                                    useCenter = false, style = Stroke(arcWidth))
                                else {
                                    var start = -90f
                                    byType.forEach { (_, n, color) ->
                                        if (n > 0) {
                                            val sweep = n / all * 360f
                                            drawArc(color, start, sweep,
                                                useCenter = false, style = Stroke(arcWidth, cap = StrokeCap.Butt))
                                            start += sweep
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(minutesLabel(totals.paidMinutes), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                Text("Ukupno", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            byType.forEach { (name, minutes, color) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                                    Spacer(Modifier.width(6.dp))
                                    Text(name, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                    Text(minutesLabel(minutes), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Odrađeni sati", minutesLabel(totals.actualMinutes), Icons.Default.Schedule,
                        Palette.BlueLight, Palette.Blue, Modifier.weight(1f))
                    MetricCard("Prekovremeni", minutesLabel(totals.overtimeMinutes), Icons.Default.AddCircle,
                        Palette.AmberLight, Palette.Amber, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Noćni rad", minutesLabel(totals.nightMinutes), Icons.Default.NightsStay,
                        Palette.PurpleLight, Palette.Purple, Modifier.weight(1f))
                    MetricCard("Nedjelje / blagdani", minutesLabel(totals.sundayMinutes) + " / " + minutesLabel(totals.holidayMinutes),
                        Icons.Default.Event, Palette.CoralLight, Palette.Coral, Modifier.weight(1f),
                        "Kategorije se mogu preklapati")
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth(), color = Palette.MintLight) {
                    Text("Procijenjena bruto plaća", color = Palette.Navy, fontWeight = FontWeight.SemiBold)
                    Text(euros(totals.grossCents), color = Palette.Navy, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                    Text("Osnovica ${euros(totals.baseCents)} + dodaci ${euros(totals.premiumCents)}",
                        color = Palette.Secondary, fontSize = 12.sp)
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Palette.Amber)
                        Spacer(Modifier.width(9.dp))
                        Text("Kako se računaju sati?", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Graf prikazuje vrste smjena. Noćni rad, nedjelje, blagdani i prekovremeni sati dodatne su kategorije koje se mogu preklapati. Ne pribrajaju se ponovno ukupnim radnim satima.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SalaryScreen(
    shifts: List<Shift>, settings: SalarySettings,
    onEditSalary: () -> Unit, onSettings: () -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    val totals = remember(shifts, settings, month) {
        WorkCalculator.calculatePeriod(shifts, settings, month.atDay(1), month.plusMonths(1).atDay(1))
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader("Plaća", "Pregled radnih sati i dodataka.", dark = true, onSettings = onSettings)
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ month = month.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodni mjesec")
                    }
                    Text(month.atDay(1).monthLabel(), Modifier.weight(1f),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton({ month = month.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeći mjesec")
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth(), color = Palette.MintLight) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null,
                        tint = Palette.Mint, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(9.dp))
                    Text("Procijenjena bruto plaća", fontWeight = FontWeight.SemiBold, color = Palette.Navy)
                    Text(euros(totals.grossCents), style = MaterialTheme.typography.headlineLarge, color = Palette.Navy)
                    Spacer(Modifier.height(4.dp))
                    Text("Prema ${minutesLabel(totals.paidMinutes)} unesenog rada i postavkama dodataka.",
                        fontSize = 12.sp, color = Palette.Secondary)
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Obračun")
                    LabelValue("Osnovna satnica (bruto)", "${settings.hourlyGross} €/h")
                    Spacer(Modifier.height(10.dp))
                    LabelValue("Osnovica za rad", euros(totals.baseCents))
                    Spacer(Modifier.height(10.dp))
                    LabelValue("Svi dodaci", euros(totals.premiumCents), Palette.Mint)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    LabelValue("Ukupno bruto (procjena)", euros(totals.grossCents))
                    Spacer(Modifier.height(12.dp))
                    BlueAction("Uredi obračunske postavke", onEditSalary, Modifier.fillMaxWidth(), Icons.Default.Tune)
                }
            }
            item {
                SectionTitle("Radni sati i dodaci")
                SurfaceCard(Modifier.fillMaxWidth()) {
                    LabelValue("Ukupno plaćenih sati", minutesLabel(totals.paidMinutes))
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Odrađeno", minutesLabel(totals.actualMinutes))
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Planirano", minutesLabel(totals.plannedMinutes))
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    LabelValue("Prekovremeni (${settings.overtimePercent}%)", minutesLabel(totals.overtimeMinutes))
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Noćni (${settings.nightPercent}%)", minutesLabel(totals.nightMinutes))
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Nedjelje (${settings.sundayPercent}%)", minutesLabel(totals.sundayMinutes))
                    Spacer(Modifier.height(8.dp))
                    LabelValue("Blagdani (${settings.holidayPercent}%)", minutesLabel(totals.holidayMinutes))
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text("Važna napomena", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Iznos je informativna procjena bruto zarade, a ne službeni obračun plaće. Stvarni dodaci, preklapanja i porezni tretman ovise o ugovoru o radu, kolektivnom ugovoru i važećim propisima. Neto iznos namjerno nije izmišljen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MoreScreen(
    darkMode: Boolean, onDark: (Boolean) -> Unit,
    onRotation: () -> Unit, onCsv: () -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit, onEditSalary: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader("Više", "Postavke i tvoji podaci.", dark = true, showSettings = false)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                SectionTitle("Alati")
                ListAction("Automatski raspored", "Ponovi rotaciju smjena", Icons.Default.AutoMode, onRotation)
                HorizontalDivider()
                ListAction("Postavke plaće", "Satnica i dodaci", Icons.Default.Calculate, onEditSalary)
                HorizontalDivider()
                ListAction("Izvoz CSV", "Izvezi smjene i evidenciju sati", Icons.Default.FileDownload, onCsv)
                HorizontalDivider()
                ListAction("Sigurnosna kopija", "Spremi sve smjene i satnicu u JSON", Icons.Default.SaveAlt, onBackup)
                HorizontalDivider()
                ListAction("Vrati podatke", "Provjeri kopiju prije zamjene postojećih podataka", Icons.Default.Restore, onRestore)
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                SectionTitle("Izgled")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = Palette.Blue)
                    Spacer(Modifier.width(12.dp))
                    Text("Tamni način rada", Modifier.weight(1f))
                    Switch(checked = darkMode, onCheckedChange = onDark)
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text("Shiftora • Brendigo", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Verzija ${BuildConfig.VERSION_NAME} • Tvoji se podaci pohranjuju lokalno na uređaju. Aplikacija ne zahtijeva prijavu niti internetsku vezu.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ListAction(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Palette.Blue)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}
