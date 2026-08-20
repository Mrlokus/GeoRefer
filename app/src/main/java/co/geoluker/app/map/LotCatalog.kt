package co.geoluker.app.map

import android.content.Context
import org.json.JSONObject

data class LotLocation(
    val code: String,
    val xFraction: Float,
    val yFraction: Float,
)

object LotCatalog {
    fun load(context: Context): List<LotLocation> = runCatching {
        val content = context.assets.open("lots.json").bufferedReader().use { it.readText() }
        val items = JSONObject(content).getJSONArray("lots")
        buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val x = item.getDouble("xFraction").toFloat()
                val y = item.getDouble("yFraction").toFloat()
                if (x in 0f..1f && y in 0f..1f) {
                    add(LotLocation(item.getString("code"), x, y))
                }
            }
        }
    }.getOrDefault(emptyList())
}
