package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brendigo.shiftora.R
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftKind
import java.text.NumberFormat
import java.util.Currency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hr = Locale("hr", "HR")
fun LocalDate.dateLabel(): String = format(DateTimeFormatter.ofPattern("d. LLLL yyyy.", hr))
fun LocalDate.weekdayLabel(): String = format(DateTimeFormatter.ofPattern("EEEE", hr))
fun LocalDate.monthLabel(): String = format(DateTimeFormatter.ofPattern("LLLL yyyy.", hr)).replaceFirstChar { it.uppercase(hr) }
fun minutesLabel(value: Long): String = if (value % 60L == 0L) "${value / 60}h" else "${value / 60}h ${value % 60}min"
fun euros(cents: Long): String = NumberFormat.getCurrencyInstance(hr).apply {
    currency = Currency.getInstance("EUR")
}.format(cents / 100.0)
fun clock(minutes: Int): String = "%02d:%02d".format(hr, minutes / 60, minutes % 60)

fun shiftColor(kind: ShiftKind): Color = when (kind) {
    ShiftKind.MORNING -> Palette.Amber
    ShiftKind.AFTERNOON -> Palette.Blue
    ShiftKind.NIGHT -> Palette.Purple
    ShiftKind.OFF -> Palette.Mint
    ShiftKind.CUSTOM -> Palette.Coral
}
fun shiftBg(kind: ShiftKind): Color = when (kind) {
    ShiftKind.MORNING -> Palette.AmberLight
    ShiftKind.AFTERNOON -> Palette.BlueLight
    ShiftKind.NIGHT -> Palette.PurpleLight
    ShiftKind.OFF -> Palette.MintLight
    ShiftKind.CUSTOM -> Palette.CoralLight
}
fun shiftIcon(kind: ShiftKind): ImageVector = when (kind) {
    ShiftKind.MORNING -> Icons.Default.WbSunny
    ShiftKind.AFTERNOON -> Icons.Default.WbTwilight
    ShiftKind.NIGHT -> Icons.Default.NightsStay
    ShiftKind.OFF -> Icons.Default.BeachAccess
    ShiftKind.CUSTOM -> Icons.Default.Schedule
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, darkSurface: Boolean = false) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(13.dp))
            .background(if (darkSurface) Palette.DeepNavy else Palette.Navy),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_shiftora_foreground),
            contentDescription = "Shiftora",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BrandedHeader(
    title: String? = null,
    subtitle: String? = null,
    dark: Boolean = false,
    showSettings: Boolean = true,
    onSettings: () -> Unit = {}
) {
    val background = if (dark) Brush.linearGradient(listOf(Palette.Navy, Palette.DeepNavy))
    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    Column(
        Modifier.fillMaxWidth().background(background)
            .padding(start = 20.dp, end = 16.dp, top = 15.dp, bottom = if (title == null) 16.dp else 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BrandMark(Modifier.size(43.dp), darkSurface = true)
            Spacer(Modifier.width(10.dp))
            Text("Shiftora", color = fg, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Spacer(Modifier.weight(1f))
            if (showSettings) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Postavke", tint = fg)
                }
            }
        }
        if (title != null) {
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = fg)
            if (subtitle != null) {
                Spacer(Modifier.height(5.dp))
                Text(subtitle, fontSize = 15.sp, color = if (dark) Color(0xFFB8CEE9) else Palette.Secondary)
            }
        }
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(22.dp), ambientColor = Palette.Navy.copy(alpha = .05f))
            .background(color, RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionTitle(title: String, trailing: String? = null, onTrailing: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) TextButton(onClick = onTrailing) { Text(trailing, color = Palette.Blue) }
    }
}

@Composable
fun ShiftBadge(kind: ShiftKind, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(13.dp))
            .background(shiftBg(kind)).padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(shiftIcon(kind), contentDescription = null, tint = shiftColor(kind), modifier = Modifier.size(19.dp))
        Text(kind.label, color = shiftColor(kind), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun ShiftRow(shift: Shift, onClick: () -> Unit, showDate: Boolean = true) {
    SurfaceCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showDate) {
                Column(Modifier.width(47.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(shift.date.format(DateTimeFormatter.ofPattern("EEE", hr)), color = Palette.Secondary, fontSize = 12.sp)
                    Text(shift.date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(shift.date.format(DateTimeFormatter.ofPattern("LLL", hr)), color = Palette.Secondary, fontSize = 11.sp)
                }
            }
            ShiftBadge(shift.kind)
            Column(Modifier.weight(1f)) {
                if (shift.kind != ShiftKind.OFF) {
                    Text("${clock(shift.startMinute)} – ${clock(shift.endMinute)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (shift.actual) Text("Odrađeno", color = Palette.Mint, fontSize = 11.sp)
                } else Text("Slobodan dan", fontSize = 12.sp, color = Palette.Secondary)
            }
            if (shift.kind != ShiftKind.OFF) Text(minutesLabel(com.brendigo.shiftora.domain.WorkCalculator.durationMinutes(shift)),
                fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Palette.Secondary)
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector, bg: Color, accent: Color, modifier: Modifier = Modifier, sub: String? = null) {
    SurfaceCard(modifier = modifier, color = bg) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, color = Palette.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(value, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        if (sub != null) Text(sub, color = Palette.Secondary, fontSize = 11.sp)
    }
}

@Composable
fun BlueAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.Add) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.Blue),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(7.dp))
        Text(text, maxLines = 2)
    }
}

@Composable
fun LabelValue(label: String, value: String, tint: Color? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, color = tint ?: MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
