package com.brendigo.shiftora.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brendigo.shiftora.data.ArchivedShift

/** Lazy archive keeps multi-year histories smooth, without permanently deleting entries. */
@Composable
fun ShiftArchiveDialog(
    entries: List<ArchivedShift>,
    onRestore: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arhiva rasporeda") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    Text(
                        "Arhivirane smjene ostaju spremljene na uređaju i mogu se vratiti. " +
                            "Ako na datumu postoji sukobljena smjena, izvorni zapis ostaje sačuvan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Text(
                        if (entries.isEmpty()) "Nema arhiviranih smjena."
                        else "Sačuvano: ${entries.size} unosa",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(entries, key = { it.archiveId }) { archived ->
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = shiftBg(archived.entry.kind),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(shiftIcon(archived.entry.kind), contentDescription = null,
                                    tint = shiftColor(archived.entry.kind),
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${archived.entry.date.dateLabel()} · ${archived.entry.kind.code}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                archived.entry.kind.label +
                                    if (archived.entry.kind.hasTime)
                                        " · ${clock(archived.entry.startMinute)}–${clock(archived.entry.endMinute)}"
                                    else "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (archived.entry.note.isNotBlank()) {
                                Text(archived.entry.note, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(
                            onClick = { onRestore(archived.archiveId) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Vrati")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}
