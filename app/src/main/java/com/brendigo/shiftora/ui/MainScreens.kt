package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
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
    val actualToday = now.toLocalDate()
    var homeDateText by rememberSaveable { mutableStateOf(actualToday.toString()) }
    val today = LocalDate.parse(homeDateText)
    val context = LocalContext.current
    val activeShift = remember(shifts, now, today) {
        if (today == actualToday) CurrentShiftIndicator.active(shifts, now.toInstant(), now.zone)
        else null
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
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(if (today == actualToday) "Danas je" else "Odabrani datum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                        Text(today.dateLabel(), fontSize = 28.sp, lineHeight = 33.sp,
                            fontWeight = FontWeight.ExtraBold, maxLines = 2)
                        Text(today.weekdayLabel().replaceFirstChar {
                            it.uppercase(Locale("hr", "HR"))
                        }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 17.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, year, month, day ->
                                homeDateText = LocalDate.of(year, month + 1, day).toString()
                            }, today.year, today.monthValue - 1, today.dayOfMonth).show()
                        },
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (today == actualToday) "Danas" else "Datum",
                            maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    }
                }
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
                                Text(if (activeShift != null) "TRENUTNA SMJENA"
                                    else if (today == actualToday) "DANAŠNJA SMJENA"
                                    else "ODABRANA SMJENA",
                                    color = Palette.Amber, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                if (activeShift != null) {
                                    Text("● Još traje",
                                        modifier = Modifier.clip(RoundedCornerShape(30.dp))
                                            .background(Palette.MintLight)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color(0xFF087E52),
                                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text(todayShift?.kind?.label ?: "Dodaj smjenu",
                                style = MaterialTheme.typography.titleLarge, maxLines = 1)
                            Text(
                                if (todayShift == null)
                                    if (today == actualToday) "Nema unosa za danas"
                                    else "Nema unosa za odabrani datum"
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
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeWeeklyOverview(weekTotal.paidMinutes, dailyTotals, maxDayMinutes,
                                Modifier.fillMaxWidth()) { onTab(2) }
                            HomeSalaryOverview(monthTotal.grossCents, YearMonth.from(today),
                                Modifier.fillMaxWidth()) { onTab(3) }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HomeWeeklyOverview(weekTotal.paidMinutes, dailyTotals, maxDayMinutes,
                                Modifier.weight(1f)) { onTab(2) }
                            HomeSalaryOverview(monthTotal.grossCents, YearMonth.from(today),
                                Modifier.weight(1f)) { onTab(3) }
                        }
                    }
                }
            }
            item {
                SectionTitle("Brze akcije")
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth < 355.dp || LocalDensity.current.fontScale > 1.25f) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeQuickAction("Dodaj smjenu", Icons.Default.Add, Palette.Blue,
                                Color.White, Modifier.fillMaxWidth()) { onAdd(today) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HomeQuickAction("Evidentiraj sate", Icons.Default.Schedule,
                                    Palette.BlueLight, Palette.Blue,
                                    Modifier.weight(1f)) { onTrack(today) }
                                HomeQuickAction("Izračunaj plaću", Icons.Default.Calculate,
                                    Palette.MintLight, Color(0xFF087D64),
                                    Modifier.weight(1f)) { onTab(3) }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeQuickAction("Dodaj smjenu", Icons.Default.Add,
                                Palette.Blue, Color.White,
                                Modifier.weight(1f)) { onAdd(today) }
                            HomeQuickAction("Evidentiraj sate", Icons.Default.Schedule,
                                Palette.BlueLight, Palette.Blue,
                                Modifier.weight(1f)) { onTrack(today) }
                            HomeQuickAction("Izračunaj plaću", Icons.Default.Calculate,
                                Palette.MintLight, Color(0xFF087D64),
                                Modifier.weight(1f)) { onTab(3) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    shifts: List<Shift>, salary: SalarySettings, monthlyFunds: Map<String, Int>,
    onAdd: (LocalDate) -> Unit, onEdit: (Shift) -> Unit,
    onFundSave: (YearMonth, Int?) -> Unit, onSettings: () -> Unit
) {
    var shownText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val shown = YearMonth.parse(shownText)
    val selected = LocalDate.parse(selectedText)
    val context = LocalContext.current
    var weekOnly by rememberSaveable { mutableStateOf(false) }
    var showAllLegend by rememberSaveable { mutableStateOf(false) }
    var fundEditorOpen by remember { mutableStateOf(false) }
    val detailRequester = remember { BringIntoViewRequester() }
    var revealDetail by remember { mutableStateOf(false) }
    LaunchedEffect(selectedText, revealDetail) {
        if (revealDetail) {
            detailRequester.bringIntoView()
            revealDetail = false
        }
    }
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
        BrandedHeader(dark = true, showNotifications = true, onSettings = onSettings)
        BoxWithConstraints(Modifier.fillMaxSize().clip(
            RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp)
        ).background(MaterialTheme.colorScheme.surface)) {
        val compact = maxHeight < 760.dp
        // Fit the selected-day card on a standard-height phone while preserving
        // a usable ~48dp day target and the full 7-column month grid.
        val dayHeight = if (compact) 48.dp else 52.dp
        val spacing = if (compact) 5.dp else 8.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 19.dp)) {
            SegmentedPeriodTabs(
                labels = listOf("Mjesec", "Tjedan"),
                selectedIndex = if (weekOnly) 1 else 0,
                onSelected = { weekOnly = it == 1 }
            )
            Spacer(Modifier.height(spacing))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val target = if (weekOnly) selected.minusWeeks(1) else shown.minusMonths(1).atDay(1)
                    selectedText = target.toString()
                    shownText = YearMonth.from(target).toString()
                }) { Icon(Icons.Default.ChevronLeft, contentDescription =
                    if (weekOnly) "Prethodni tjedan" else "Prethodni mjesec") }
                TextButton(
                    onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            val chosen = LocalDate.of(year, month + 1, day)
                            shownText = YearMonth.from(chosen).toString()
                            selectedText = chosen.toString()
                            revealDetail = true
                        }, selected.year, selected.monthValue - 1,
                            selected.dayOfMonth).show()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(shown.atDay(1).monthLabel(), color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Default.ArrowDropDown,
                        contentDescription = "Odaberi datum u kalendaru",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    val target = if (weekOnly) selected.plusWeeks(1) else shown.plusMonths(1).atDay(1)
                    selectedText = target.toString()
                    shownText = YearMonth.from(target).toString()
                }) { Icon(Icons.Default.ChevronRight, contentDescription =
                    if (weekOnly) "Sljedeći tjedan" else "Sljedeći mjesec") }
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
                        val today = date == LocalDate.now()
                        val tileColor = if (chosen) Palette.Blue
                            else if (entry != null) shiftBg(entry.kind)
                            else Color.Transparent
                        val tileText = if (chosen) Color.White
                            else if (entry != null) Palette.Navy
                            else MaterialTheme.colorScheme.onSurface
                        val details = if (entries.isEmpty()) "Nema smjene"
                            else entries.joinToString(", ") { "${it.kind.code}: ${it.kind.label}" }
                        Box(
                            Modifier.weight(1f).padding(1.dp).height(dayHeight)
                                .clip(RoundedCornerShape(11.dp))
                                .background(tileColor)
                                .then(if (today && !chosen)
                                    Modifier.border(1.5.dp, Palette.Blue, RoundedCornerShape(11.dp))
                                    else Modifier)
                                .semantics {
                                    contentDescription = "${date.dateLabel()}. $details"
                                    this.selected = chosen
                                }
                                .clickable {
                                    selectedText = date.toString()
                                    shownText = YearMonth.from(date).toString()
                                    // Show the action card without forcing users to hunt below
                                    // a five/six-row month grid after selecting a day.
                                    revealDetail = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(date.dayOfMonth.toString(), color =
                                    if (chosen) Color.White
                                    else if (entry != null) Palette.Navy
                                    else if (inMonth) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .48f),
                                    fontWeight = if (chosen || today) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp, lineHeight = 17.sp)
                                if (entries.isNotEmpty()) {
                                    if (entries.size == 1 && entries.first().kind in listOf(
                                            ShiftKind.MORNING, ShiftKind.AFTERNOON,
                                            ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.DAY
                                        )
                                    ) {
                                        val kind = entries.first().kind
                                        if (!chosen && (kind == ShiftKind.MORNING ||
                                                kind == ShiftKind.AFTERNOON)) {
                                            // Reference month grid: compact colored dots for
                                            // morning/afternoon, icons for night/free days.
                                            Box(Modifier.size(8.dp).clip(CircleShape)
                                                .background(shiftColor(kind)))
                                        } else {
                                            Icon(shiftIcon(kind),
                                                contentDescription = null,
                                                tint = if (chosen) Color.White else shiftColor(kind),
                                                modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Text(entries.first().kind.code,
                                            fontSize = 10.sp, lineHeight = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chosen) Color.White
                                                else shiftColor(entries.first().kind))
                                    }
                                    if (entries.size > 1) {
                                        Text("+${entries.size - 1}", fontSize = 9.sp,
                                            lineHeight = 10.sp, color = tileText)
                                    }
                                } else if (inMonth) {
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val mainKinds = listOf(
                    ShiftKind.MORNING to "Jutarnja",
                    ShiftKind.AFTERNOON to "Popodnevna",
                    ShiftKind.NIGHT to "Noćna",
                    ShiftKind.OFF to "Slobodno"
                )
                val columns = if (maxWidth < 355.dp ||
                    LocalDensity.current.fontScale > 1.25f) 2 else 4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mainKinds.chunked(columns).forEach { group ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            group.forEach { (kind, name) ->
                                Row(Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(10.dp).clip(CircleShape)
                                        .background(shiftColor(kind)))
                                    Text(name, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
            // The supplied calendar reference shows the four main colors on one
            // line. Keep every special status discoverable without crowding the
            // selected-day detail below the month grid.
             if (showAllLegend) {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ShiftKind.calendarOrder.chunked(5).forEach { kinds ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            kinds.forEach { kind ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(shiftColor(kind)))
                                    Text(kind.code, fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(spacing))
            }
            Column(
                Modifier.fillMaxWidth()
                    .bringIntoViewRequester(detailRequester)
                    .shadow(4.dp, RoundedCornerShape(21.dp),
                        ambientColor = Palette.Navy.copy(alpha = .08f))
                    .clip(RoundedCornerShape(21.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f),
                        RoundedCornerShape(21.dp))
                    .padding(if (compact) 11.dp else 15.dp)
            ) {
                Row(Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.dateLabel(), Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp, maxLines = 2)
                    TextButton(
                        onClick = { showAllLegend = !showAllLegend },
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp)
                    ) {
                        Text(if (showAllLegend) "Sakrij oznake" else "Ostale oznake",
                            fontSize = 11.sp)
                        Icon(if (showAllLegend) Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,
                            contentDescription = null, modifier = Modifier.size(17.dp))
                    }
                }
                Spacer(Modifier.height(3.dp))
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
                                Text(if (shift.kind.isWork) "${shift.kind.label} smjena" else shift.kind.label,
                                    style = MaterialTheme.typography.titleMedium)
                                Text(if (shift.kind == ShiftKind.STANDBY)
                                        "${clock(shift.startMinute)} – ${clock(shift.endMinute)} (pripravnost)"
                                    else if (!shift.kind.isWork) shift.kind.label
                                    else "${clock(shift.startMinute)} – ${clock(shift.endMinute)} • ${minutesLabel(WorkCalculator.durationMinutes(shift))}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                            }
                            Text(if (!shift.kind.isWork) "Evidencija"
                                else if (shift.actual) "Odrađeno" else "Planirano",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (shift.actual) Palette.MintLight
                                        else Palette.AmberLight)
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                                color = if (shift.actual) Color(0xFF087E52)
                                    else Color(0xFFB96D00),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(7.dp))
                        if (shift.kind.isWork) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                if (maxWidth < 340.dp ||
                                    LocalDensity.current.fontScale > 1.3f) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DetailTile("Radno vrijeme",
                                                "${clock(shift.startMinute)}–${clock(shift.endMinute)}",
                                                Modifier.weight(1f), Icons.Default.Schedule)
                                            DetailTile("Pauza", "${shift.breakMinutes} min",
                                                Modifier.weight(1f), Icons.Default.Coffee)
                                        }
                                        DetailTile("Bilješka", shift.note.ifBlank { "—" },
                                            Modifier.fillMaxWidth(), Icons.Default.Description)
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DetailTile("Radno vrijeme",
                                            "${clock(shift.startMinute)}–${clock(shift.endMinute)}",
                                            Modifier.weight(1f), Icons.Default.Schedule)
                                        DetailTile("Pauza", "${shift.breakMinutes} min",
                                            Modifier.weight(1f), Icons.Default.Coffee)
                                        DetailTile("Bilješka", shift.note.ifBlank { "—" },
                                            Modifier.weight(1f), Icons.Default.Description)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onEdit(shift) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
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
        }
        }
    }
}

@Composable
private fun DetailTile(
    label: String, value: String, modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(modifier.heightIn(min = 75.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
            RoundedCornerShape(12.dp)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Text(label, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(5.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, lineHeight = 14.sp, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatsScreen(shifts: List<Shift>, settings: SalarySettings, onSettings: () -> Unit) {
    val context = LocalContext.current
    var shownText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val shown = YearMonth.parse(shownText)
    var period by rememberSaveable { mutableIntStateOf(1) }
    var weekAnchorText by rememberSaveable {
        mutableStateOf(LocalDate.now().with(
            TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        ).toString())
    }
    val weekAnchor = LocalDate.parse(weekAnchorText)
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
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SegmentedPeriodTabs(
                    labels = listOf("Tjedan", "Mjesec", "Godina"),
                    selectedIndex = period,
                    onSelected = { period = it }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (period == 0) weekAnchorText = weekAnchor.minusWeeks(1).toString()
                        else shownText = shown.minusMonths(if (period == 2) 12 else 1).toString()
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodno razdoblje")
                    }
                    val name = when (period) {
                        0 -> weekAnchor.dateLabel() + " – " + weekAnchor.plusDays(6).dateLabel()
                        2 -> shown.year.toString()
                        else -> shown.atDay(1).monthLabel()
                    }
                    TextButton(onClick = {
                        val initial = if (period == 0) weekAnchor else shown.atDay(1)
                        DatePickerDialog(context, { _, year, month, day ->
                            val date = LocalDate.of(year, month + 1, day)
                            shownText = YearMonth.from(date).toString()
                            weekAnchorText = date.with(
                                TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
                            ).toString()
                        }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(name, color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown,
                            contentDescription = "Odaberi razdoblje statistike",
                            modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = {
                        if (period == 0) weekAnchorText = weekAnchor.plusWeeks(1).toString()
                        else shownText = shown.plusMonths(if (period == 2) 12 else 1).toString()
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeće razdoblje")
                    }
                }
            }
            item { ShiftDonutCard(totals) }
            item {
                ResponsiveStatPair(
                    first = { m ->
                        StatIndicatorCard(
                            "Odrađeni sati", minutesLabel(totals.actualMinutes),
                            Icons.Default.Schedule, Palette.BlueLight, Palette.Blue,
                            totals.actualMinutes, previous.actualMinutes, m,
                            "Planirano: " + minutesLabel(totals.plannedMinutes)
                        )
                    },
                    second = { m ->
                        StatIndicatorCard(
                            "Prekovremeni", minutesLabel(totals.overtimeMinutes),
                            Icons.Default.AddCircle, Palette.AmberLight, Palette.Amber,
                            totals.overtimeMinutes, previous.overtimeMinutes, m
                        )
                    }
                )
            }
            item {
                ResponsiveStatPair(
                    first = { m ->
                        StatIndicatorCard(
                            "Noćni rad", minutesLabel(totals.nightMinutes),
                            Icons.Default.NightsStay, Palette.PurpleLight, Palette.Purple,
                            totals.nightMinutes, previous.nightMinutes, m
                        )
                    },
                    second = { m ->
                        StatIndicatorCard(
                            "Nedjelje / blagdani", minutesLabel(totals.specialMinutes),
                            Icons.Default.Event, Palette.CoralLight, Palette.Coral,
                            totals.specialMinutes, previous.specialMinutes, m,
                            "Ned: " + minutesLabel(totals.sundayMinutes) +
                                " · Blag: " + minutesLabel(totals.holidayMinutes)
                        )
                    }
                )
            }
            item { SalaryStatsCard(totals, previous) }
            item { StatsExplanation(totals, settings) }
        }
    }
}

/** Keep the reference's two-column KPI grid until each card would be unreadable. */
@Composable
private fun ResponsiveStatPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 330.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SalaryScreen(
    shifts: List<Shift>, settings: SalarySettings,
    onEditSalary: () -> Unit, onSettings: () -> Unit
) {
    val context = LocalContext.current
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val totals = remember(shifts, settings, month) {
        WorkCalculator.calculatePeriod(shifts, settings, month.atDay(1), month.plusMonths(1).atDay(1))
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandedHeader("Plaća", "Pregled radnih sati i dodataka.", dark = true, onSettings = onSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ monthText = month.minusMonths(1).toString() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prethodni mjesec")
                    }
                    TextButton(onClick = {
                        DatePickerDialog(context, { _, year, selectedMonth, _ ->
                            monthText = YearMonth.of(year, selectedMonth + 1).toString()
                        }, month.year, month.monthValue - 1, 1).show()
                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)) {
                        Text(month.atDay(1).monthLabel(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown,
                            contentDescription = "Odaberi mjesec obračuna",
                            modifier = Modifier.size(19.dp))
                    }
                    IconButton({ monthText = month.plusMonths(1).toString() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sljedeći mjesec")
                    }
                }
            }
            item {
                SurfaceCard(Modifier.fillMaxWidth(), color = Palette.MintLight) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(54.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color.White.copy(alpha = .55f)),
                            contentAlignment = Alignment.Center) {
                            Icon(ShiftoraCoinsIcon, contentDescription = null,
                                tint = Color(0xFF008F75), modifier = Modifier.size(31.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Procijenjena bruto plaća",
                                fontWeight = FontWeight.SemiBold, color = Palette.Navy,
                                fontSize = 15.sp)
                            Text("Za ${month.atDay(1).monthLabel()}",
                                color = Palette.Secondary, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Info,
                            contentDescription = "Informativna bruto procjena",
                            tint = Color(0xFF008F75), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(euros(totals.grossCents),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold, color = Palette.Navy,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement =
                        Arrangement.spacedBy(9.dp)) {
                        Column(Modifier.weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = .6f))
                            .padding(10.dp)) {
                            Text("Osnovica", color = Palette.Secondary, fontSize = 11.sp)
                            Text(euros(totals.baseCents), color = Palette.Navy,
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        }
                        Column(Modifier.weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = .6f))
                            .padding(10.dp)) {
                            Text("Dodaci", color = Palette.Secondary, fontSize = 11.sp)
                            Text(euros(totals.premiumCents), color = Color(0xFF087D64),
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("Temelji se na ${minutesLabel(totals.paidMinutes)} rada i postavkama dodataka. Neto se ne izračunava.",
                        fontSize = 11.sp, color = Palette.Secondary, lineHeight = 16.sp)
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
    savedShifts: Int, archivedShifts: Int,
    onRotation: () -> Unit, onCsv: () -> Unit,
    onBackup: () -> Unit, onRestore: () -> Unit, onEditSalary: () -> Unit,
    onShiftColors: () -> Unit,
    onArchive: () -> Unit
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
                    Text("Ručno arhivirani unosi ostaju u lokalnoj arhivi i mogu se vratiti. " +
                        "Uvoz ne briše postojeći raspored. Brisanje podataka aplikacije ili " +
                        "deinstalacija mogu ukloniti lokalnu bazu, zato prije promjene uređaja " +
                        "izvezi JSON kopiju na sigurno mjesto.",
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
        Column(Modifier.fillMaxSize().clip(RoundedCornerShape(
            topStart = 27.dp, topEnd = 27.dp
        )).background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
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
                ListAction("Sigurnosna kopija", "Spremi aktivne i arhivirane smjene u JSON", Icons.Default.SaveAlt, onBackup)
                HorizontalDivider()
                ListAction("Arhiva rasporeda", "Pregledaj i vrati ručno arhivirane smjene", Icons.Default.Inventory2, onArchive)
                HorizontalDivider()
                ListAction("Uvezi raspored", "Spoji JSON kopiju bez brisanja postojećih unosa", Icons.Default.Restore, onRestore)
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Palette.Mint)
                    Column(Modifier.weight(1f)) {
                        Text("Zaštita rasporeda", fontWeight = FontWeight.Bold)
                        Text("U kalendaru: $savedShifts · U arhivi: $archivedShifts",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Pregled prošlih i budućih smjena ne briše podatke. " +
                    "Uvoz samo dopunjuje raspored, a ručno arhivirani unosi mogu se vratiti. " +
                    "Za promjenu uređaja ili ponovnu instalaciju izvezi JSON sigurnosnu kopiju.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Izvezi sigurnosnu kopiju")
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                SectionTitle("Izgled")
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .toggleable(value = darkMode, role = Role.Switch, onValueChange = onDark),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Tamni način rada", Modifier.weight(1f))
                    Switch(checked = darkMode, onCheckedChange = null)
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
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}


@Composable
private fun HomeWeeklyOverview(
    paidMinutes: Long,
    dailyTotals: List<com.brendigo.shiftora.data.WorkTotals>,
    maxDayMinutes: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SurfaceCard(modifier.clickable(onClick = onClick), color = Palette.BlueLight) {
        Text("Ovaj tjedan", color = Palette.Ink, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(7.dp))
        Text(minutesLabel(paidMinutes), fontSize = 26.sp, color = Palette.Ink,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Planirano i odrađeno", color = Palette.Secondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom) {
            repeat(7) { day ->
                val minutes = dailyTotals[day].paidMinutes
                val fraction = (minutes.toFloat() / maxDayMinutes).coerceIn(0f, 1f)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(38.dp),
                        contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth()
                            .height(if (minutes > 0L) (36f * fraction).coerceAtLeast(4f).dp else 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (minutes > 0L) Palette.Blue else Color(0xFFCBD7E8)))
                    }
                    Text(listOf("P", "U", "S", "Č", "P", "S", "N")[day],
                        fontSize = 10.sp, color = Palette.Secondary)
                }
            }
        }
    }
}

@Composable
private fun HomeSalaryOverview(
    grossCents: Long,
    month: YearMonth,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SurfaceCard(modifier.clickable(onClick = onClick), color = Palette.MintLight) {
        Text("Procjena bruto plaće", color = Palette.Ink,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Icon(Icons.Default.BarChart, contentDescription = null,
            tint = Palette.Mint, modifier = Modifier.size(24.dp))
        Text(euros(grossCents), fontWeight = FontWeight.Bold, color = Palette.Ink,
            fontSize = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(month.atDay(1).monthLabel(), color = Palette.Secondary, fontSize = 12.sp)
    }
}

@Composable
private fun HomeQuickAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(modifier.heightIn(min = 65.dp).clip(RoundedCornerShape(18.dp))
        .background(background).clickable(onClick = onClick)
        .padding(horizontal = 9.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = foreground,
            modifier = Modifier.size(25.dp))
        Text(text, color = foreground, fontWeight = FontWeight.Bold,
            fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
    }
}
