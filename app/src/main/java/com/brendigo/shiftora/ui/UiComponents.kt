package com.brendigo.shiftora.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val hr = Locale("hr", "HR")
fun LocalDate.dateLabel(): String = format(DateTimeFormatter.ofPattern("d. LLLL yyyy.", hr))
fun LocalDate.weekdayLabel(): String = format(DateTimeFormatter.ofPattern("EEEE", hr))
fun LocalDate.monthLabel(): String = format(DateTimeFormatter.ofPattern("LLLL yyyy.", hr)).replaceFirstChar { it.uppercase(hr) }
fun minutesLabel(value: Long): String = if (value % 60L == 0L) "${value / 60}h" else "${value / 60}h ${value % 60}min"
fun euros(cents: Long): String = NumberFormat.getNumberInstance(hr).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(cents / 100.0) + " €"
fun clock(minutes: Int): String = "%02d:%02d".format(hr, minutes / 60, minutes % 60)

/** User-defined colors are provided throughout the app by ShiftoraTheme. */
val LocalShiftColors = compositionLocalOf<Map<ShiftKind, Int>> { emptyMap() }

private fun defaultShiftColor(kind: ShiftKind): Color = when (kind) {
    ShiftKind.MORNING -> Palette.Amber
    ShiftKind.AFTERNOON -> Palette.Blue
    ShiftKind.NIGHT -> Palette.Purple
    ShiftKind.CUSTOM -> Palette.Coral
    ShiftKind.OFF -> Palette.Mint
    ShiftKind.DAY -> Color(0xFF1976D2)
    ShiftKind.STANDBY -> Color(0xFF8665CB)
    ShiftKind.ANNUAL -> Color(0xFF059669)
    ShiftKind.SICK -> Color(0xFFAD4BC6)
    ShiftKind.PAID_LEAVE -> Color(0xFF0891B2)
}

@Composable
fun shiftColor(kind: ShiftKind): Color =
    LocalShiftColors.current[kind]?.let(::Color) ?: defaultShiftColor(kind)

@Composable
fun shiftBg(kind: ShiftKind): Color {
    // Light, readable surfaces for labels in the details, full color in month cells.
    val custom = LocalShiftColors.current[kind]
    if (custom != null) return Color(custom).copy(alpha = .15f)
    return when (kind) {
        ShiftKind.MORNING -> Palette.AmberLight
        ShiftKind.AFTERNOON -> Palette.BlueLight
        ShiftKind.NIGHT -> Palette.PurpleLight
        ShiftKind.OFF, ShiftKind.ANNUAL -> Palette.MintLight
        ShiftKind.CUSTOM, ShiftKind.SICK -> Palette.CoralLight
        ShiftKind.DAY -> Palette.BlueLight
        ShiftKind.STANDBY -> Palette.PurpleLight
        ShiftKind.PAID_LEAVE -> Palette.MintLight
    }
}

fun shiftIcon(kind: ShiftKind): ImageVector = when (kind) {
    ShiftKind.MORNING -> Icons.Default.WbSunny
    ShiftKind.AFTERNOON -> Icons.Default.WbTwilight
    ShiftKind.NIGHT -> Icons.Default.NightsStay
    ShiftKind.OFF -> Icons.Default.BeachAccess
    ShiftKind.CUSTOM -> Icons.Default.Schedule
    ShiftKind.DAY -> Icons.Default.WbSunny
    ShiftKind.STANDBY -> Icons.Default.NotificationsActive
    ShiftKind.ANNUAL -> Icons.Default.BeachAccess
    ShiftKind.SICK -> Icons.Default.Healing
    ShiftKind.PAID_LEAVE -> Icons.Default.EventAvailable
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, darkSurface: Boolean = false) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(13.dp))
            .background(if (darkSurface) Palette.DeepNavy else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_shiftora_foreground),
            contentDescription = null,
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
    showStatsDecor: Boolean = false,
    onSettings: () -> Unit = {}
) {
    val background = if (dark) Brush.linearGradient(listOf(Palette.Navy, Palette.DeepNavy))
    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    val fg = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    Column(
        Modifier.fillMaxWidth().background(background)
            .padding(start = 17.dp, end = 17.dp, top = 13.dp,
                bottom = if (title == null) 12.dp else if (dark) 25.dp else 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BrandMark(Modifier.size(45.dp), darkSurface = dark)
            Spacer(Modifier.width(8.dp))
            Text("Shiftora", color = fg, fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp, letterSpacing = (-0.65).sp)
            Spacer(Modifier.weight(1f))
            if (showSettings) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Postavke", tint = fg)
                }
            }
        }
        if (title != null) {
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 30.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.ExtraBold, color = fg)
                    if (subtitle != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(subtitle, fontSize = 15.sp,
                            color = if (dark) Color(0xFFB8CEE9) else Palette.Secondary)
                    }
                }
                if (showStatsDecor) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null,
                            tint = Color(0xFF32D5BD), modifier = Modifier.size(27.dp))
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(13, 20, 27, 35).forEach { h ->
                                Box(Modifier.width(10.dp).height(h.dp)
                                    .background(Palette.Blue, RoundedCornerShape(
                                        topStart = 3.dp, topEnd = 3.dp)))
                            }
                        }
                    }
                }
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
            .shadow(5.dp, RoundedCornerShape(21.dp), ambientColor = Palette.Navy.copy(alpha = .09f))
            .background(color, RoundedCornerShape(21.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun SectionTitle(title: String, trailing: String? = null, onTrailing: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (trailing != null) TextButton(onClick = onTrailing) {
            Text(trailing, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
fun ShiftBadge(kind: ShiftKind, modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(13.dp))
            .background(shiftBg(kind)).padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(shiftIcon(kind), contentDescription = null, tint = shiftColor(kind), modifier = Modifier.size(19.dp))
        Text(if (compact) kind.code else kind.label, color = shiftColor(kind),
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ShiftRow(shift: Shift, onClick: () -> Unit, showDate: Boolean = true) {
    SurfaceCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 335.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (narrow) 5.dp else 8.dp)) {
            if (showDate) {
                Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(shift.date.format(DateTimeFormatter.ofPattern("EEE", hr)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(shift.date.dayOfMonth.toString(), fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Text(shift.date.format(DateTimeFormatter.ofPattern("LLL", hr)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            ShiftBadge(shift.kind, compact = narrow)
            Column(Modifier.weight(1f)) {
                Text(
                    if (shift.kind.hasTime) "${clock(shift.startMinute)} – ${clock(shift.endMinute)}"
                    else shift.kind.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                val detail = when {
                    shift.kind.isWork && shift.actual -> "Odrađeno · " +
                        minutesLabel(com.brendigo.shiftora.domain.WorkCalculator.durationMinutes(shift))
                    shift.kind.isWork -> "Planirano · " +
                        minutesLabel(com.brendigo.shiftora.domain.WorkCalculator.durationMinutes(shift))
                    shift.kind == ShiftKind.STANDBY -> "Pripravnost"
                    else -> shift.kind.label
                }
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
        }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector, bg: Color, accent: Color, modifier: Modifier = Modifier, sub: String? = null) {
    Column(modifier = modifier.clip(RoundedCornerShape(19.dp)).background(bg).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(label, color = Palette.Ink, fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
        }
        Spacer(Modifier.height(3.dp))
        Text(value, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        if (sub != null) Text(sub, color = Palette.Secondary, fontSize = 10.sp, lineHeight = 12.sp)
    }
}

@Composable
fun BlueAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.Default.Add) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.Blue),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, maxLines = 2, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, lineHeight = 18.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
fun LabelValue(label: String, value: String, tint: Color? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
            lineHeight = 19.sp)
        Text(value, color = tint ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
