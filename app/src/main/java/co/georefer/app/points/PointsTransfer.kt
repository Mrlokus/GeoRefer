package co.georefer.app.points

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.xml.sax.InputSource

object PointsTransfer {
    fun export(points: List<FieldPoint>, format: PointFileFormat): String = when (format) {
        PointFileFormat.GEOJSON -> exportGeoJson(points)
        PointFileFormat.KML -> exportKml(points)
        PointFileFormat.GPX -> exportGpx(points)
        PointFileFormat.CSV -> exportCsv(points)
    }

    fun import(content: String, fileName: String): List<ImportedPoint> = when {
        fileName.endsWith(".geojson", true) || fileName.endsWith(".json", true) ||
            content.trimStart().startsWith("{") -> importGeoJson(content)
        fileName.endsWith(".kml", true) || content.contains("<kml", true) -> importKml(content)
        fileName.endsWith(".gpx", true) || content.contains("<gpx", true) -> importGpx(content)
        fileName.endsWith(".csv", true) -> importCsv(content)
        else -> error("Formato no reconocido. Usa GeoJSON, KML, GPX o CSV")
    }

    private fun exportGeoJson(points: List<FieldPoint>): String {
        val features = JSONArray()
        points.forEach { point ->
            features.put(
                JSONObject()
                    .put("type", "Feature")
                    .put(
                        "properties",
                        JSONObject().put("name", point.name).put("note", point.note),
                    )
                    .put(
                        "geometry",
                        JSONObject()
                            .put("type", "Point")
                            .put("coordinates", JSONArray().put(point.longitude).put(point.latitude)),
                    ),
            )
        }
        return JSONObject().put("type", "FeatureCollection").put("features", features).toString(2)
    }

    private fun exportKml(points: List<FieldPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>\n")
        points.forEach { point ->
            append("  <Placemark><name>${xml(point.name)}</name>")
            if (point.note.isNotBlank()) append("<description>${xml(point.note)}</description>")
            append("<Point><coordinates>${point.longitude},${point.latitude},0</coordinates></Point></Placemark>\n")
        }
        append("</Document></kml>\n")
    }

    private fun exportGpx(points: List<FieldPoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Georefer\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        points.forEach { point ->
            append("  <wpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">")
            append("<name>${xml(point.name)}</name>")
            if (point.note.isNotBlank()) append("<desc>${xml(point.note)}</desc>")
            append("</wpt>\n")
        }
        append("</gpx>\n")
    }

    private fun exportCsv(points: List<FieldPoint>): String = buildString {
        append("name,note,latitude,longitude\n")
        points.forEach { point ->
            append(csv(point.name)).append(',')
            append(csv(point.note)).append(',')
            append(point.latitude).append(',').append(point.longitude).append('\n')
        }
    }

    private fun importGeoJson(content: String): List<ImportedPoint> {
        val root = JSONObject(content)
        val features = root.optJSONArray("features") ?: return emptyList()
        return buildList {
            for (index in 0 until features.length()) {
                val feature = features.optJSONObject(index) ?: continue
                val geometry = feature.optJSONObject("geometry") ?: continue
                if (!geometry.optString("type").equals("Point", true)) continue
                val coordinates = geometry.optJSONArray("coordinates") ?: continue
                if (coordinates.length() < 2) continue
                val properties = feature.optJSONObject("properties") ?: JSONObject()
                add(
                    ImportedPoint(
                        name = properties.optString("name", "Punto importado"),
                        note = properties.optString("note", properties.optString("description", "")),
                        latitude = coordinates.optDouble(1),
                        longitude = coordinates.optDouble(0),
                    ),
                )
            }
        }
    }

    private fun importKml(content: String): List<ImportedPoint> = parseXml(content).let { document ->
        val nodes = document.getElementsByTagNameNS("*", "Placemark")
        buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                val coordinates = element.textOf("coordinates")?.trim()?.split(',') ?: continue
                if (coordinates.size < 2) continue
                add(
                    ImportedPoint(
                        name = element.textOf("name").orEmpty().ifBlank { "Punto importado" },
                        note = element.textOf("description").orEmpty(),
                        latitude = coordinates[1].trim().toDoubleOrNull() ?: continue,
                        longitude = coordinates[0].trim().toDoubleOrNull() ?: continue,
                    ),
                )
            }
        }
    }

    private fun importGpx(content: String): List<ImportedPoint> = parseXml(content).let { document ->
        val nodes = document.getElementsByTagNameNS("*", "wpt")
        buildList {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                add(
                    ImportedPoint(
                        name = element.textOf("name").orEmpty().ifBlank { "Punto importado" },
                        note = element.textOf("desc").orEmpty(),
                        latitude = element.getAttribute("lat").toDoubleOrNull() ?: continue,
                        longitude = element.getAttribute("lon").toDoubleOrNull() ?: continue,
                    ),
                )
            }
        }
    }

    private fun importCsv(content: String): List<ImportedPoint> {
        val rows = content.lineSequence().filter { it.isNotBlank() }.map(::parseCsvRow).toList()
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        val latitudeIndex = header.indexOfFirst { it in setOf("latitude", "latitud", "lat") }
        val longitudeIndex = header.indexOfFirst { it in setOf("longitude", "longitud", "lon", "lng") }
        if (latitudeIndex < 0 || longitudeIndex < 0) error("El CSV debe tener columnas latitude y longitude")
        val nameIndex = header.indexOfFirst { it in setOf("name", "nombre") }
        val noteIndex = header.indexOfFirst { it in setOf("note", "nota", "description", "descripcion") }
        return rows.drop(1).mapNotNull { row ->
            val latitude = row.getOrNull(latitudeIndex)?.trim()?.toDoubleOrNull() ?: return@mapNotNull null
            val longitude = row.getOrNull(longitudeIndex)?.trim()?.toDoubleOrNull() ?: return@mapNotNull null
            ImportedPoint(
                name = row.getOrNull(nameIndex).orEmpty().ifBlank { "Punto importado" },
                note = row.getOrNull(noteIndex).orEmpty(),
                latitude = latitude,
                longitude = longitude,
            )
        }
    }

    private fun parseXml(content: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(InputSource(StringReader(content)))

    private fun Element.textOf(name: String): String? =
        getElementsByTagNameNS("*", name).item(0)?.textContent

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private fun parseCsvRow(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            when (val character = line[index]) {
                '"' -> if (quoted && index + 1 < line.length && line[index + 1] == '"') {
                    current.append('"')
                    index++
                } else quoted = !quoted
                ',' -> if (quoted) current.append(character) else {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(character)
            }
            index++
        }
        values += current.toString()
        return values
    }
}

data class PointNavigationInfo(
    val distanceMeters: Double,
    val bearingDegrees: Double,
    val cardinal: String,
)

fun navigationToPoint(
    fromLatitude: Double,
    fromLongitude: Double,
    point: FieldPoint,
): PointNavigationInfo {
    val latitude1 = Math.toRadians(fromLatitude)
    val latitude2 = Math.toRadians(point.latitude)
    val deltaLatitude = latitude2 - latitude1
    val deltaLongitude = Math.toRadians(point.longitude - fromLongitude)
    val a = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
        cos(latitude1) * cos(latitude2) * sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
    val distance = EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    val y = sin(deltaLongitude) * cos(latitude2)
    val x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(deltaLongitude)
    val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    return PointNavigationInfo(distance, bearing, directions[((bearing + 22.5) / 45.0).toInt() % 8])
}

private const val EARTH_RADIUS_METERS = 6_371_008.8
