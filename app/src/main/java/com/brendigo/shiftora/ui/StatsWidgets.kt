package com.brendigo.shiftora.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.shiftora.data.WorkTotals
import com.brendigo.shiftora.data.SalarySettings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** A zero previous value is 'not comparable', rather than a fictional +100% growth. */
fun percentageChange(now: Long, previous: Long): Double? = when {
    previous == 0L && now == 0L -> 0.0
    previous == 0L -> null
    else -> (now.toDouble() - previous.toDouble()) * 100.0 / previous.toDouble()
}

@Composable
private fun TrendIndicator(now: Long, before: Long, modifier: Modifier = Modifier) {
    val change = percentageChange(now, before)
    val accent = when {
        change == null -> Palette.Secondary
        change > 0.01 -> Color(0xFF079B60)
        change < -0.01 -> Palette.Coral
        else -> Palette.Secondary
    }
    val icon = when {
        change == null || abs(change) < .01 -> Icons.Default.TrendingFlat
        change > 0 -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Text(if (change == null) "Nema usporedbe"
            else (if (change > 0.01) "+" else "") +
                String.format(Locale("hr", "HR"), "%.0f%%", change),
            color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1)
    }
}

@Composable
fun StatIndicatorCard(
    title: String,
    value: String,
    icon: ImageVector,
    surface: Color,
    tint: Color,
    currentMinutes: Long,
    previousMinutes: Long,
    modifier: Modifier = Modifier,
    extra: String? = null
) {
    // Match the reference's compact two-column KPI cards. Allow
    // two-line titles without forcing the second row below the fold.
    Column(modifier.clip(RoundedCornerShape(21.dp)).background(surface)
        .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = tint,
                modifier = Modifier.size(25.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Palette.Ink,
                    fontSize = 12.sp, lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(value, color = Palette.Ink, fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, lineHeight = 26.sp)
            }
        }
        TrendIndicator(currentMinutes, previousMinutes)
        if (extra != null) {
            Text(extra, color = Palette.Secondary, fontSize = 10.sp,
                lineHeight = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ShiftDonutCard(totals: WorkTotals) {
    val total = totals.paidMinutes
    val sections = listOf(
        Triple("Redovni sati", totals.chartRegularMinutes, Palette.Blue),
        Triple("Prekovremeni", totals.chartOvertimeMinutes, Palette.Amber),
        Triple("Noćni rad", totals.chartNightMinutes, Palette.Purple),
        Triple("Nedjelje / blagdani", totals.chartSpecialMinutes, Palette.Coral)
    )
    val chart: @Composable () -> Unit = {
        Box(Modifier.size(129.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(9.dp)) {
                val stroke = Stroke(width = 19.dp.toPx(), cap = StrokeCap.Butt)
                if (total <= 0L) {
                    drawArc(Palette.BlueLight, -90f, 360f, useCenter = false, style = stroke)
                } else {
                    var offset = -90f
                    sections.forEach { (_, minutes, color) ->
                        if (minutes > 0L) {
                            val sweep = (minutes.toFloat() / total.toFloat()) * 360f
                            drawArc(color, offset, sweep, useCenter = false, style = stroke)
                            offset += sweep
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(minutesLabel(total), color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Ukupno", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp)
            }
        }
    }
    val legend: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (name, amount, color) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                    Text(name, color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(minutesLabel(amount), color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(if (total == 0L) "0%" else
                        ((amount * 100.0 / total).roundToInt()).toString() + "%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
        .background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Preserve the reference's donut + legend layout on normal phones.
            // Stack only when text would otherwise collapse on narrow screens.
            if (maxWidth < 310.dp || LocalDensity.current.fontScale > 1.3f) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chart()
                    legend(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    chart()
                    legend(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SalaryStatsCard(totals: WorkTotals, previous: WorkTotals) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
            .background(Palette.MintLight).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.BarChart, contentDescription = null,
                tint = Color(0xFF008F7F), modifier = Modifier.size(30.dp))
            Text("Procijenjena plaća", color = Palette.Navy,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.Info,
                contentDescription = "Iznos je informativna bruto procjena, bez neto izračuna",
                tint = Color(0xFF087C74), modifier = Modifier.size(19.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 288.dp || LocalDensity.current.fontScale > 1.3f
            val amount: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(euros(totals.grossCents), color = Palette.Navy,
                        fontSize = if (stacked) 32.sp else 24.sp,
                        lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TrendIndicator(totals.grossCents, previous.grossCents)
                }
            }
            val breakdown: @Composable (Modifier) -> Unit = { modifier ->
                Column(modifier.clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = .70f))
                    .padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Bruto (procjena)", color = Palette.Secondary, fontSize = 11.sp)
                    Text(euros(totals.grossCents), color = Palette.Navy,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HorizontalDivider(color = Palette.Mint.copy(alpha = .22f))
                    Text("Neto nije izračunat", color = Palette.Secondary,
                        fontSize = 10.sp, lineHeight = 13.sp)
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    amount(Modifier.fillMaxWidth())
                    breakdown(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    amount(Modifier.weight(1.2f))
                    breakdown(Modifier.weight(.9f))
                }
            }
        }
        Text("Osnovica " + euros(totals.baseCents) +
            "  ·  Dodaci " + euros(totals.premiumCents),
            color = Color(0xFF346C68), fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
fun StatsExplanation(totals: WorkTotals, settings: SalarySettings) {
    var expanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(14.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }.padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.AmberLight),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Palette.Amber)
            }
            Spacer(Modifier.width(8.dp))
            Text("Kako se računa procijenjena plaća?", fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), fontSize = 14.sp)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Sažmi" else "Proširi")
        }
        if (expanded) {
            Spacer(Modifier.height(9.dp))
            Text("Procjena temelji se na spremljenim smjenama, ugovorenoj bruto satnici i postocima iz postavki.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val items = listOf(
                Triple("Redovni sati", minutesLabel(totals.chartRegularMinutes), Palette.BlueLight),
                Triple("Prekovremeni", minutesLabel(totals.overtimeMinutes) + " · " +
                    settings.overtimePercent + "%", Palette.AmberLight),
                Triple("Noćni rad", minutesLabel(totals.nightMinutes) + " · " +
                    settings.nightPercent + "%", Palette.PurpleLight),
                Triple("Nedjelje / blagdani", minutesLabel(totals.specialMinutes),
                    Palette.CoralLight)
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 295.dp &&
                    LocalDensity.current.fontScale <= 1.3f) 4 else 2
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items.chunked(columns).forEach { line ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            line.forEach { (label, value, color) ->
                                Column(Modifier.weight(1f)
                                    .clip(RoundedCornerShape(13.dp)).background(color)
                                    .padding(horizontal = 5.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(value, fontWeight = FontWeight.Bold,
                                        fontSize = if (columns == 4) 12.sp else 14.sp,
                                        color = Palette.Navy, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(label, fontSize = 10.sp, color = Palette.Secondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2, lineHeight = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            Text("Kategorije na grafikonu ne preklapaju se, ali dodaci mogu. Iznos nije službeni obračun plaće.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
