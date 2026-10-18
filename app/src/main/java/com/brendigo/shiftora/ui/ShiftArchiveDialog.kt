package com.brendigo.shiftora.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brendigo.shiftora.data.ArchivedShift

/** Archived shifts remain on the device until explicitly restored. No purge action. */
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
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Arhiviranje ne briše podatke. Odabrane smjene možeš vratiti u kalendar. " +
                        "Ako na datumu postoji sukobljena smjena, izvorni arhivirani unos ostaje sačuvan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entries.isEmpty()) {
                    Text("Nema arhiviranih smjena.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Sačuvano: ${entries.size} unosa", fontWeight = FontWeight.SemiBold)
                    entries.forEach { archived ->
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${archived.entry.date.dateLabel()} · ${archived.entry.kind.code}",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    archived.entry.kind.label +
                                        if (archived.entry.kind.hasTime)
                                            " · ${clock(archived.entry.startMinute)}–${clock(archived.entry.endMinute)}"
                                        else "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (archived.entry.note.isNotBlank()) {
                                    Text(archived.entry.note, maxLines = 2,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(onClick = { onRestore(archived.archiveId) }) {
                                Icon(Icons.Default.Restore, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Vrati")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } }
    )
}
