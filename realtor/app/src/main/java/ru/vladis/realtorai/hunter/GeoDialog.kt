package ru.vladis.realtorai.hunter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.ui.Field
import ru.vladis.realtorai.ui.Muted

@Composable
fun GeoDialog(query: GeoQuery, onDismiss: () -> Unit, onSave: (GeoQuery) -> Unit) {
    var areas by remember { mutableStateOf(query.locations.joinToString("; ")) }
    var stations by remember { mutableStateOf(query.stations.joinToString("; ")) }
    var address by remember { mutableStateOf(query.address) }
    var center by remember { mutableStateOf(query.center?.let { "${it.lat}; ${it.lon}" }.orEmpty()) }
    var radius by remember { mutableStateOf(query.radiusKm?.toString().orEmpty()) }
    var mkad by remember { mutableStateOf(query.mkadKm?.toString().orEmpty()) }
    val parts = center.split(';').map { it.trim().replace(',', '.').toDoubleOrNull() }
    val point = GeoScope.point(parts.getOrNull(0), parts.getOrNull(1))
    val distance = radius.replace(',', '.').toDoubleOrNull()
    val ringDistance = mkad.replace(',', '.').toDoubleOrNull()
    val valid = (radius.isBlank() || point != null && distance != null && distance in 0.1..250.0) &&
        (mkad.isBlank() || ringDistance != null && ringDistance in 0.1..250.0)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("География поиска") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("По умолчанию Москва, Новая Москва и Московская область.", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (location in listOf("Москва", "Новая Москва", "МО")) {
                        val selected = location in GeoPreferences.split(areas)
                        FilterChip(selected, {
                            val current = GeoPreferences.split(areas)
                            areas = (if (selected) current - location else current + location).joinToString("; ")
                        }, label = { Text(location) })
                    }
                }
                Field(areas, { areas = it.take(600) }, "Районы и города через ;")
                Field(stations, { stations = it.take(600) }, "Метро, МЦК, МЦД через ;")
                Field(address, { address = it.take(160) }, "Часть адреса")
                Field(center, { center = it.take(60) }, "Центр: широта; долгота")
                Field(radius, { radius = it.take(8) }, "Радиус в км")
                Field(mkad, { mkad = it.take(8) }, "До МКАД, по прямой, км")
                Text("Радиус и МКАД применяются к объектам с координатами. Станции берутся из фида. Границы и МКАД © OpenStreetMap.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = {
            Button({ onSave(GeoQuery(GeoPreferences.split(areas), GeoPreferences.split(stations), address.trim(), point, distance, ringDistance)) }, enabled = valid) {
                Text("Применить")
            }
        }, dismissButton = { TextButton({ onSave(GeoPreferences.default) }) { Text("Сбросить") } })
}
