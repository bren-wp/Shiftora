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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.shiftora.BuildConfig
import com.brendigo.shiftora.data.SalarySettings
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.domain.WorkCalculator
import com.brendigo.shiftora.domain.MonthlyFund
import com.brendigo.shiftora.domain.CurrentShiftIndicator
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
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
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = ZonedDateTime.now()
        }
    }
    val today = now.toLocalDate()
    val activeShift = remember(shifts, now) {
        CurrentShiftIndicator.active(shifts, now.toInstant(), now.zone)
    }
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
    val todayShift = activeShift?.entry ?: shifts.firstOrNull { it.date == today }
    val maxDayMinutes = (dailyTotals.maxOfOrNull { it.paidMinutes } ?: 0L).coerceAtLeast(60L)
    val upcoming = shifts.filter { !it.date.isBefore(today.plusDays(1)) }
        .sortedWith(compareBy<Shift> { it.date }.thenBy { it.startMinute }).take(5)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader(onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                Spacer(Modifier.height(2.dp))
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
                            Modifier.size(55.dp).clip(RoundedCornerShape(17.dp))
                                .background(if (todayShift != null) shiftBg(todayShift.kind) else Palette.BlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (todayShift != null) shiftIcon(todayShift.kind) else Icons.Default.Today,
                                contentDescription = null,
                                tint = if (todayShift != null) shiftColor(todayShift.kind) else Palette.Blue,
                                modifier = Modifier.size(29.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (activeShift != null) "TRENUTNA SMJENA" else "DANAŠNJA SMJENA",
                                    color = Palette.Amber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                if (activeShift != null) {
                                    Text("● U tijeku", color = Color(0xFF087E52),
                                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text(todayShift?.kind?.label ?: "Dodaj smjenu",
                                style = MaterialTheme.typography.titleLarge, maxLines = 1)
                            Text(
                                if (todayShift == null) "Nema unosa za danas"
                                else if (todayShift.kind == ShiftKind.STANDBY)
                                    "Pripravnost ${clock(todayShift.startMinute)} – ${clock(todayShift.endMinute)}"
                                else if (!todayShift.kind.isWork) todayShift.kind.label
                                else "${clock(todayShift.startMinute)} – ${clock(todayShift.endMinute)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp
                            )
                            if (activeShift != null) {
                                Text("Preostalo " + minutesLabel(activeShift.remainingMinutes),
                                    color = Color(0xFF087E52), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        if (todayShift != null && todayShift.kind.isWork) {
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
                        Spacer(Modifier.height(7.dp))
                        Text(minutesLabel(weekTotal.paidMinutes), fontSize = 29.sp, color = Palette.Ink, fontWeight = FontWeight.Bold)
                        Text("Planirano i odrađeno", color = Palette.Secondary, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom) {
                            repeat(7) { day ->
                                val minutes = dailyTotals[day].paidMinutes
                                val fraction = (minutes.toFloat() / maxDayMinutes).coerceIn(0f, 1f)
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.fillMaxWidth().height(38.dp),
                                        contentAlignment = Alignment.BottomCenter) {
                                        Box(Modifier.fillMaxWidth().height(if (minutes > 0L) (36f * fraction).coerceAtLeast(4f).dp else 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (minutes > 0L) Palette.Blue else Color(0xFFCBD7E8)))
                                    }
                                    Text(listOf("P", "U", "S", "Č", "P", "S", "N")[day],
                                        fontSize = 9.sp, color = Palette.Secondary)
                                }
                            }
                        }
                    }
                    SurfaceCard(Modifier.weight(1f).clickable { onTab(3) }, color = Palette.MintLight) {
                        Text("Procjena bruto plaće", color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = Palette.Mint, modifier = Modifier.size(23.dp))
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
    shifts: List<Shift>, salary: SalarySettings, monthlyFunds: Map<String, Int>,
    onAdd: (LocalDate) -> Unit, onEdit: (Shift) -> Unit,
    onFundSave: (YearMonth, Int?) -> Unit, onSettings: () -> Unit
) {
    var shown by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var weekOnly by remember { mutableStateOf(false) }
    var fundEditorOpen by remember { mutableStateOf(false) }
    val fund = remember(shown, shifts, salary, monthlyFunds) {
        MonthlyFund.summary(shown, shifts, salary, monthlyFunds[shown.toString()])
    }
    if (fundEditorOpen) {
        MonthlyFundEditor(shown, monthlyFunds[shown.toString()],
            onSave = { onFundSave(shown, it) },
            onDismiss = { fundEditorOpen = false })
    }
    val first = shown.atDay(1)
    val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val weeks = ((first.dayOfWeek.value - 1 + shown.lengthOfMonth() + 6) / 7)
    val calendarDays = (0 until weeks * 7).map { start.plusDays(it.toLong()) }
    val rows = if (weekOnly) {
        val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
        listOf((0..6).map { monday.plusDays(it.toLong()) })
    } else calendarDays.chunked(7)
    val shiftsByDate = remember(shifts) { shifts.groupBy { it.date } }
    val dayShifts = shiftsByDate[selected].orEmpty()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader(dark = true, onSettings = onSettings)
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 715.dp
        val dayHeight = if (compact) 51.dp else 61.dp
        val spacing = if (compact) 5.dp else 7.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Palette.BlueLight).padding(3.dp)) {
                listOf("Mjesec", "Tjedan").forEachIndexed { index, label ->
                    val isSelected = weekOnly == (index == 1)
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { weekOnly = index == 1 }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center) {
                        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Palette.Blue else Palette.Navy)
                    }
                }
            }
            Spacer(Modifier.height(spacing))
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
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Pon", "Uto", "Sri", "Čet", "Pet", "Sub", "Ned").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
            rows.forEach { dates ->
                Row(Modifier.fillMaxWidth()) {
                    dates.forEach { date ->
                        val chosen = selected == date
                        val entries = shiftsByDate[date].orEmpty()
                        val entry = entries.firstOrNull()
                        val inMonth = YearMonth.from(date) == shown
                        // A filled day tile shows the code clearly: D/N/J/P/GO/BO/SD/PD/PR.
                        // Selection remains a border so it never hides the chosen shift color.
                        val tileColor = if (entry != null) shiftColor(entry.kind)
                            else if (chosen) Palette.BlueLight else Color.Transparent
                        val tileText = if (tileColor.luminance() > .48f) Palette.Navy else Color.White
                        Box(
                            Modifier.weight(1f).padding(2.dp).height(dayHeight)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tileColor)
                                .then(if (chosen) Modifier.border(2.dp, Palette.Navy,
                                    RoundedCornerShape(11.dp)) else Modifier)
                                .clickable {
                                    selected = date
                                    shown = YearMonth.from(date)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(date.dayOfMonth.toString(),
                                    color = if (entry != null) tileText.copy(alpha = .86f)
                                        else if (inMonth) MaterialTheme.colorScheme.onSurface
                                        else Palette.Secondary.copy(alpha = .5f),
                                    fontWeight = FontWeight.Medium, fontSize = 10.sp,
                                    lineHeight = 13.sp)
                                if (entry != null) {
                                    Text(entry.kind.code, color = tileText,
                                        fontSize = if (entry.kind.code.length > 1) 16.sp else 24.sp,
                                        lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1)
                                    if (entries.size > 1) {
                                        Text("+${entries.size - 1}", color = tileText,
                                            fontSize = 9.sp, lineHeight = 10.sp)
                                    }
                                } else {
                                    Spacer(Modifier.height(19.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing))
            Column(Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ShiftKind.calendarOrder.chunked(5).forEach { kinds ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        kinds.forEach { kind ->
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(shiftColor(kind)))
                                Text(kind.code, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing))
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fond sati · ${shown.atDay(1).monthLabel()}",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Fond: ${minutesLabel(fund.fundMinutes.toLong())}" +
                            "   •   Odrađeno: ${minutesLabel(fund.actuallyWorkedMinutes)}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Planirano: ${minutesLabel(fund.plannedMinutes)}" +
                            "   •   GO/BO/PD: ${minutesLabel(fund.leaveCreditMinutes.toLong())}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Preostalo: ${minutesLabel(fund.remainingMinutes)}" +
                            "   •   Uz plan: ${minutesLabel(fund.projectedRemainingMinutes)}",
                            fontSize = 11.sp, color = Palette.Blue, fontWeight = FontWeight.SemiBold)
                        if (fund.overFundMinutes > 0) {
                            Text("Iznad fonda: ${minutesLabel(fund.overFundMinutes)}",
                                color = Palette.Coral, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { fundEditorOpen = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Uredi fond sati")
                    }
                }
            }
            Spacer(Modifier.height(spacing))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(if (compact) 11.dp else 15.dp)
            ) {
                Text(selected.dateLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                if (dayShifts.isEmpty()) {
                    Text("Za ovaj dan nema spremljene smjene.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    dayShifts.forEachIndexed { index, shift ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Box(Modifier.size(if (compact) 44.dp else 52.dp).clip(RoundedCornerShape(14.dp))
                                .background(shiftBg(shift.kind)), contentAlignment = Alignment.Center) {
                                Icon(shiftIcon(shift.kind), contentDescription = null,
                                    tint = shiftColor(shift.kind), modifier = Modifier.size(27.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("${shift.kind.code} · ${shift.kind.label}", style = MaterialTheme.typography.titleMedium)
                                Text(if (shift.kind == ShiftKind.STANDBY)
                                        "${clock(shift.startMinute)} – ${clock(shift.endMinute)} (pripravnost)"
                                    else if (!shift.kind.isWork) shift.kind.label
                                    else "${clock(shift.startMinute)} – ${clock(shift.endMinute)} • ${minutesLabel(WorkCalculator.durationMinutes(shift))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                            }
                            Text(if (!shift.kind.isWork) "Evidencija" else if (shift.actual) "Odrađeno" else "Planirano",
                                color = if (shift.actual) Palette.Mint else Palette.Blue, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                        if (shift.kind.isWork) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailTile("Radno vrijeme", "${clock(shift.startMinute)}–${clock(shift.endMinute)}", Modifier.weight(1f))
                                DetailTile("Pauza", "${shift.breakMinutes} min", Modifier.weight(1f))
                                DetailTile("Bilješka", shift.note.ifBlank { "—" }, Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onEdit(shift) },
                                modifier = Modifier.weight(1f).heightIn(min = 46.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(13.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Uredi", maxLines = 1)
                            }
                            if (index == dayShifts.lastIndex) {
                                BlueAction("Dodaj smjenu", { onAdd(selected) }, Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (dayShifts.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    BlueAction("Dodaj smjenu", { onAdd(selected) }, Modifier.fillMaxWidth())
                }
            }
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
    var weekAnchor by remember {
        mutableStateOf(LocalDate.now().with(
            TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        ))
    }
    val rangeStart = when (period) {
        0 -> weekAnchor
        2 -> LocalDate.of(shown.year, 1, 1)
        else -> shown.atDay(1)
    }
    val rangeEnd = when (period) {
        0 -> rangeStart.plusWeeks(1)
        2 -> rangeStart.plusYears(1)
        else -> rangeStart.plusMonths(1)
    }
    val priorStart = when (period) {
        0 -> rangeStart.minusWeeks(1)
        2 -> rangeStart.minusYears(1)
        else -> rangeStart.minusMonths(1)
    }
    val totals = remember(shifts, settings, rangeStart, rangeEnd) {
        WorkCalculator.calculatePeriod(shifts, settings, rangeStart, rangeEnd)
    }
    val previous = remember(shifts, settings, priorStart, rangeStart) {
        WorkCalculator.calculatePeriod(shifts, settings, priorStart, rangeStart)
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader("Statistika i plaća", "Tvoj rad u brojkama. Veća kontrola.",
            dark = true, showStatsDecor = true, onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                    .background(Palette.BlueLight).padding(3.dp)) {
                    listOf("Tjedan", "Mjesec", "Godina").forEachIndexed { index, title ->
                        val selected = period == index
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                            .background(if (selected) Color(0xFFD0E2FF) else Color.Transparent)
                            .clickable { period = index }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center) {
                            Text(title, color = if (selected) Palette.Blue else Palette.Navy,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (period == 0) weekAnchor = weekAnchor.minusWeeks(1)
                        else shown = shown.minusMonths(if (period == 2) 12 else 1)
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodno razdoblje")
                    }
                    val name = when (period) {
                        0 -> weekAnchor.dateLabel() + " – " + weekAnchor.plusDays(6).dateLabel()
                        2 -> shown.year.toString()
                        else -> shown.atDay(1).monthLabel()
                    }
                    Text(name, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = {
                        if (period == 0) weekAnchor = weekAnchor.plusWeeks(1)
                        else shown = shown.plusMonths(if (period == 2) 12 else 1)
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeće razdoblje")
                    }
                }
            }
            item { ShiftDonutCard(totals) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatIndicatorCard(
                        "Odrađeni sati", minutesLabel(totals.actualMinutes),
                        Icons.Default.Schedule, Palette.BlueLight, Palette.Blue,
                        totals.actualMinutes, previous.actualMinutes, Modifier.weight(1f),
                        "Planirano: " + minutesLabel(totals.plannedMinutes)
                    )
                    StatIndicatorCard(
                        "Prekovremeni", minutesLabel(totals.overtimeMinutes),
                        Icons.Default.AddCircle, Palette.AmberLight, Palette.Amber,
                        totals.overtimeMinutes, previous.overtimeMinutes, Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatIndicatorCard(
                        "Noćni rad", minutesLabel(totals.nightMinutes),
                        Icons.Default.NightsStay, Palette.PurpleLight, Palette.Purple,
                        totals.nightMinutes, previous.nightMinutes, Modifier.weight(1f)
                    )
                    StatIndicatorCard(
                        "Nedjelje / blagdani",
                        minutesLabel(totals.specialMinutes),
                        Icons.Default.Event, Palette.CoralLight, Palette.Coral,
                        totals.specialMinutes, previous.specialMinutes, Modifier.weight(1f),
                        "Ned: " + minutesLabel(totals.sundayMinutes) +
                            " · Blag: " + minutesLabel(totals.holidayMinutes)
                    )
                }
            }
            item { SalaryStatsCard(totals, previous) }
            item { StatsExplanation() }
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
    onBackup: () -> Unit, onRestore: () -> Unit, onEditSalary: () -> Unit,
    onShiftColors: () -> Unit
) {
    var privacyVisible by remember { mutableStateOf(false) }
    if (privacyVisible) {
        AlertDialog(
            onDismissRequest = { privacyVisible = false },
            title = { Text("Privatnost podataka") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text("Shiftora radi lokalno, bez registracije, oglasa, analitike ili praćenja. " +
                        "Ne traži dozvolu za internet, lokaciju ni kontakte.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Smjene i postavke pohranjuju se samo na ovom uređaju. " +
                        "Automatsko sigurnosno kopiranje aplikacije je isključeno. " +
                        "CSV i JSON izvozi stvaraju se samo kada ih sam odabereš, " +
                        "a mjesto spremanja kontroliraš u Androidovom odabiru datoteka.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Za brisanje podataka obriši unose u aplikaciji ili podatke aplikacije " +
                        "u Android postavkama. Ranije izvezene datoteke moraš obrisati zasebno.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text("Javna verzija pravila: github.com/bren-wp/Shiftora/blob/main/PRIVACY.md",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { privacyVisible = false }) { Text("Zatvori") }
            }
        )
    }
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
                ListAction("Boje smjena i izostanaka", "Promijeni boju za D, N, J, P, PR, GO, BO, SD, PD i X",
                    Icons.Default.Palette, onShiftColors)
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
                ListAction("Pravila privatnosti", "Kako čuvamo tvoje podatke",
                    Icons.Default.Shield, { privacyVisible = true })
                HorizontalDivider()
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
