package co.geoluker.app.points

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PointsRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): List<FieldPoint> = runCatching {
        val json = preferences.getString(KEY_POINTS, null) ?: return emptyList()
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    FieldPoint(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        note = item.optString("note"),
                        latitude = item.getDouble("latitude"),
                        longitude = item.getDouble("longitude"),
                        createdAtMillis = item.getLong("createdAtMillis"),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(points: List<FieldPoint>) {
        val array = JSONArray()
        points.forEach { point ->
            array.put(
                JSONObject()
                    .put("id", point.id)
                    .put("name", point.name)
                    .put("note", point.note)
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("createdAtMillis", point.createdAtMillis),
            )
        }
        preferences.edit().putString(KEY_POINTS, array.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "geoluker_points"
        const val KEY_POINTS = "saved_points"
    }
}
