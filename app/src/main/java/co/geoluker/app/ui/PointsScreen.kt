package co.geoluker.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.geoluker.app.location.GpsFix
import co.geoluker.app.points.FieldPoint
import co.geoluker.app.points.PointsUiState
import co.geoluker.app.points.navigationToPoint
import kotlinx.coroutines.delay
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
    onClearMessage: () -> Unit,
) {
    var pointToDelete by remember { mutableStateOf<FieldPoint?>(null) }
    var pointToEdit by remember { mutableStateOf<FieldPoint?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(PointSort.RECENT) }
    val visiblePoints = remember(state.points, query, sort, gpsFix) {
        val normalized = query.trim().lowercase(Locale.ROOT)
        val filtered = state.points.filter { point ->
            normalized.isBlank() ||
                point.name.lowercase(Locale.ROOT).contains(normalized) ||
                point.note.lowercase(Locale.ROOT).contains(normalized)
        }
        when (sort) {
            PointSort.RECENT -> filtered.sortedByDescending(FieldPoint::createdAtMillis)
            PointSort.NAME -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
            PointSort.NEARBY -> gpsFix?.let { fix ->
                filtered.sortedBy { point ->
                    navigationToPoint(fix.latitude, fix.longitude, point).distanceMeters
                }
            } ?: filtered.sortedByDescending(FieldPoint::createdAtMillis)
        }
    }

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(2_600)
            onClearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Puntos de campo", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Referencias guardadas en este teléfono",
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            state.points.size.toString(),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = { Text("Buscar punto") },
                    placeholder = { Text("Nombre o nota") },
                    singleLine = true,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PointSort.entries.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option.label) },
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            "Mantén presionado cualquier lugar del mapa para crear un punto.",
                            modifier = Modifier.padding(start = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            if (state.points.isEmpty()) {
                item {
                    EmptyPointsState()
                }
            } else if (visiblePoints.isEmpty()) {
                item {
                    Text(
                        "No hay puntos que coincidan con la búsqueda.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(visiblePoints, key = { it.id }) { point ->
                    PointCard(
                        point = point,
                        gpsFix = gpsFix,
                        onLocate = { onLocatePoint(point) },
                        onEdit = { pointToEdit = point },
                        onDelete = { pointToDelete = point },
                    )
                }
            }
        }

        state.message?.let { message ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 4.dp,
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }

    pointToEdit?.let { point ->
        PointEditDialog(
            point = point,
            onDismiss = { pointToEdit = null },
            onSave = { name, note ->
                onUpdatePoint(point.id, name, note)
                pointToEdit = null
            },
        )
    }

    pointToDelete?.let { point ->
        AlertDialog(
            onDismissRequest = { pointToDelete = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Eliminar ${point.name}") },
            text = { Text("Este punto se eliminará definitivamente del teléfono.") },
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

private enum class PointSort(val label: String) {
    RECENT("Recientes"),
    NEARBY("Cercanos"),
    NAME("Nombre"),
}

@Composable
private fun PointCard(
    point: FieldPoint,
    gpsFix: GpsFix?,
    onLocate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val navigation = remember(gpsFix, point) {
        gpsFix?.let { navigationToPoint(it.latitude, it.longitude, point) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
                    Text(point.name, style = MaterialTheme.typography.titleMedium)
                    navigation?.let {
                        Text(
                            "${formatDistance(it.distanceMeters)} · ${it.cardinal} · ${it.bearingDegrees.toInt()}°",
                            modifier = Modifier.padding(top = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar punto")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eliminar punto")
                }
            }

            if (point.note.isNotBlank()) {
                Text(
                    point.note,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            String.format(Locale.US, "%.6f, %.6f", point.latitude, point.longitude),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            formatPointDate(point.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onLocate) {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Ver mapa")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPointsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 54.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.padding(16.dp).size(30.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Aún no hay puntos",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Crea referencias para accesos, labores, novedades o lugares importantes dentro de Luker Agrícola.",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PointEditDialog(
    point: FieldPoint,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(point.id) { mutableStateOf(point.name) }
    var note by remember(point.id) { mutableStateOf(point.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        title = { Text("Editar punto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, note) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun formatPointDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-CO")).format(Date(value))

private fun formatDistance(meters: Double): String = if (meters >= 1_000.0) {
    String.format(Locale.US, "%.1f km", meters / 1_000.0)
} else {
    String.format(Locale.US, "%.0f m", meters)
}
