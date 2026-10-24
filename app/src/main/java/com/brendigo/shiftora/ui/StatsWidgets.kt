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
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    Column(modifier.clip(RoundedCornerShape(21.dp)).background(surface)
        .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = tint,
                modifier = Modifier.size(29.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Palette.Ink,
                    fontSize = 13.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(value, color = Palette.Ink, fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, lineHeight = 28.sp)
            }
        }
        TrendIndicator(currentMinutes, previousMinutes)
        if (extra != null) {
            Text(extra, color = Palette.Secondary, fontSize = 10.sp,
                lineHeight = 13.sp, maxLines = 2,
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
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surface).padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(129.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(9.dp)) {
                    val width = 19.dp.toPx()
                    if (total <= 0L) {
                        drawArc(Palette.BlueLight, -90f, 360f,
                            useCenter = false, style = Stroke(width, cap = StrokeCap.Butt))
                    } else {
                        var offset = -90f
                        sections.forEach { (_, minutes, color) ->
                            if (minutes > 0L) {
                                val arc = minutes.toFloat() / total.toFloat() * 360f
                                drawArc(color, offset, arc, useCenter = false,
                                    style = Stroke(width, cap = StrokeCap.Butt))
                                offset += arc
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(minutesLabel(total), color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Ukupno", color = Palette.Secondary, fontSize = 12.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sections.forEach { (name, amount, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Text(name, color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp, maxLines = 1,
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
    }
}

@Composable
fun SalaryStatsCard(totals: WorkTotals, previous: WorkTotals) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
        .background(Palette.MintLight).padding(17.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.TrendingFlat, contentDescription = null,
                tint = Color(0xFF008F7F), modifier = Modifier.size(27.dp))
            Text("Procijenjena bruto plaća", color = Palette.Navy,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.Info, contentDescription =
                "Procjena bruto iznosa, ne neto plaća",
                tint = Color(0xFF087C74), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(9.dp))
        Text(euros(totals.grossCents), color = Palette.Navy, fontSize = 34.sp,
            lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(7.dp))
        TrendIndicator(totals.grossCents, previous.grossCents)
        Spacer(Modifier.height(4.dp))
        Text("Osnovica " + euros(totals.baseCents) +
            "   •   Dodaci " + euros(totals.premiumCents),
            color = Color(0xFF346C68), fontSize = 12.sp)
        Text("Informativni bruto iznos · neto se ne izračunava.",
            color = Color(0xFF346C68), fontSize = 11.sp)
    }
}

@Composable
fun StatsExplanation(totals: WorkTotals, settings: SalarySettings) {
    var expanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(21.dp))
        .background(MaterialTheme.colorScheme.surface)
        .clickable { expanded = !expanded }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.AmberLight),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Palette.Amber)
            }
            Spacer(Modifier.width(8.dp))
            Text("Kako se računaju sati i dodaci?", fontWeight = FontWeight.SemiBold,
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
            items.chunked(2).forEach { line ->
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    line.forEach { (label, value, color) ->
                        Column(Modifier.weight(1f)
                            .clip(RoundedCornerShape(13.dp)).background(color)
                            .padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = Palette.Navy, maxLines = 1)
                            Text(label, fontSize = 10.sp, color = Palette.Secondary,
                                maxLines = 2, lineHeight = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
            Text("Kategorije na grafikonu ne preklapaju se, ali dodaci mogu. Iznos nije službeni obračun plaće.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
