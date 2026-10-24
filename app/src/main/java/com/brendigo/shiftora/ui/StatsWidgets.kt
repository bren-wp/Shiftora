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
    Column(modifier.clip(RoundedCornerShape(19.dp)).background(surface)
        .padding(horizontal = 11.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(title, color = Palette.Ink, fontSize = 12.sp, lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2,
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(3.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 190.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(value, color = Palette.Ink, fontSize = 21.sp,
                        lineHeight = 25.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TrendIndicator(currentMinutes, previousMinutes)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(value, color = Palette.Ink,
                        fontSize = if (value.length > 10) 17.sp else 22.sp,
                        lineHeight = 25.sp, maxLines = 1, fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TrendIndicator(currentMinutes, previousMinutes)
                }
            }
        }
        if (extra != null) {
            Text(extra, color = Palette.Secondary, fontSize = 10.sp, lineHeight = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface).padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(9.dp)) {
                    val width = 21.dp.toPx()
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
                    Text(minutesLabel(total), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("Ukupno", color = Palette.Secondary, fontSize = 12.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sections.forEach { (name, amount, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
                        Text(name, color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(minutesLabel(amount), color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
        .background(Palette.MintLight).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF087C74),
                modifier = Modifier.size(19.dp))
            Text("Procijenjena bruto plaća", color = Palette.Navy, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(euros(totals.grossCents), color = Palette.Navy, fontSize = 32.sp,
            fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        TrendIndicator(totals.grossCents, previous.grossCents)
        Text("Osnovica " + euros(totals.baseCents) + "  ·  Dodaci " + euros(totals.premiumCents),
            color = Color(0xFF346C68), fontSize = 12.sp)
        Text("Informativni bruto iznos, nije neto plaća.",
            color = Color(0xFF346C68), fontSize = 11.sp)
    }
}

@Composable
fun StatsExplanation() {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surface)
        .clickable { expanded = !expanded }.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Palette.Amber)
            Spacer(Modifier.width(8.dp))
            Text("Kako se računaju sati i dodaci?", fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f), fontSize = 13.sp)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Sažmi" else "Proširi")
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Kružni graf svakom plaćenom satu dodjeljuje jednu kategoriju: " +
                    "prekovremeni → nedjelja/blagdan → noćni → redovni. " +
                    "Zbroj prikazanih kategorija zato je jednak ukupnim satima. " +
                    "Kartice dodataka prikazuju stvarne minute koje se mogu preklapati: " +
                    "primjerice, rad nedjeljom može biti i noćni. " +
                    "Promjena postotka računa se prema istom prethodnom razdoblju; " +
                    "kad prethodne vrijednosti nema, ne prikazuje se izmišljeni rast.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
