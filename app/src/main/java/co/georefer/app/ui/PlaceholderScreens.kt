package co.georefer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.georefer.app.location.GpsFix
import co.georefer.app.points.FieldPoint
import co.georefer.app.points.PointFileFormat
import co.georefer.app.points.PointsUiState
import co.georefer.app.points.navigationToPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PointsScreen(
    state: PointsUiState,
    gpsFix: GpsFix?,
    onDeletePoint: (Long) -> Unit,
    onUpdatePoint: (Long, String, String) -> Unit,
    onLocatePoint: (FieldPoint) -> Unit,
    onExport: (PointFileFormat) -> Unit,
    onImport: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var pointToDelete by remember { mutableStateOf<FieldPoint?>(null) }
    var pointToEdit by remember { mutableStateOf<FieldPoint?>(null) }
    var showExportFormats by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Puntos guardados", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${state.points.size} guardados solamente en este teléfono",
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showExportFormats = true },
                    enabled = state.points.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(7.dp))
                    Text("Exportar")
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Spacer(Modifier.size(7.dp))
                    Text("Importar")
                }
            }
            state.message?.let { message ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    onClick = onClearMessage,
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (state.points.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Aún no hay puntos",
                        modifier = Modifier.padding(top = 14.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "Mantén presionado un lugar del mapa o importa un archivo compatible.",
                        modifier = Modifier.padding(top = 5.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        items(state.points, key = { it.id }) { point ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(point.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", point.latitude, point.longitude),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            gpsFix?.let { fix ->
                                val navigation = navigationToPoint(fix.latitude, fix.longitude, point)
                                Text(
                                    "${formatDistance(navigation.distanceMeters)} · ${navigation.cardinal} · ${navigation.bearingDegrees.toInt()}°",
                                    modifier = Modifier.padding(top = 3.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = { pointToEdit = point }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar punto")
                        }
                        IconButton(onClick = { pointToDelete = point }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar punto")
                        }
                    }
                    if (point.note.isNotBlank()) {
                        Text(
                            point.note,
                            modifier = Modifier.padding(top = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatPointDate(point.createdAtMillis),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        TextButton(onClick = { onLocatePoint(point) }) {
                            Icon(
                                Icons.Outlined.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.size(5.dp))
                            Text("Ver en mapa")
                        }
                    }
                }
            }
        }
    }

    if (showExportFormats) {
        AlertDialog(
            onDismissRequest = { showExportFormats = false },
            title = { Text("Formato de exportación") },
            text = {
                Column {
                    PointFileFormat.entries.forEach { format ->
                        TextButton(
                            onClick = {
                                showExportFormats = false
                                onExport(format)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(format.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormats = false }) { Text("Cancelar") }
            },
        )
    }

    pointToEdit?.let { point ->
        var name by remember(point.id) { mutableStateOf(point.name) }
        var note by remember(point.id) { mutableStateOf(point.note) }
        AlertDialog(
            onDismissRequest = { pointToEdit = null },
            title = { Text("Editar punto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePoint(point.id, name, note)
                        pointToEdit = null
                    },
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { pointToEdit = null }) { Text("Cancelar") }
            },
        )
    }

    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            title = { Text("Eliminar ${point.name}") },
            text = { Text("El punto se borrará solamente de este teléfono.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePoint(point.id)
                        pointToDelete = null
                    },
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pointToDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

private fun formatPointDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-CO")).format(Date(value))

private fun formatDistance(meters: Double): String = if (meters >= 1_000.0) {
    String.format(Locale.US, "%.1f km", meters / 1_000.0)
} else {
    String.format(Locale.US, "%.0f m", meters)
}

@Composable
fun SettingsScreen(
    northUpLocked: Boolean,
    onNorthUpLockedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Preferencias de la aplicación",
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "MAPA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Explore,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Norte arriba", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mantener la orientación fija del mapa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = northUpLocked,
                onCheckedChange = onNorthUpLockedChange,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Place,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Ubicación precisa", style = MaterialTheme.typography.titleMedium)
                Text(
                    "La app utiliza el GPS interno del teléfono",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
