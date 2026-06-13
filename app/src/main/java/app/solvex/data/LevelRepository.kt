package app.solvex.data

import android.content.Context
import app.solvex.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

object LevelRepository {

    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/oussamaessid/SolvexData/refs/heads/main/solvex_levels_compact.json"
    private const val CACHE_FILE = "solvex_levels_compact.json"

    @Volatile private var cache: List<GameLevel>? = null

    // Appelé depuis un background thread (Dispatchers.IO)
    fun load(context: Context): List<GameLevel> {
        cache?.let { return it }

        // 1. Essayer le réseau
        val networkJson = try {
            URL(REMOTE_URL).readText()
        } catch (_: Exception) { null }

        if (networkJson != null) {
            context.filesDir.resolve(CACHE_FILE).writeText(networkJson)
            val parsed = parseJson(networkJson)
            cache = parsed
            return parsed
        }

        // 2. Fallback sur le cache local
        val cacheFile = context.filesDir.resolve(CACHE_FILE)
        if (cacheFile.exists()) {
            val parsed = parseJson(cacheFile.readText())
            cache = parsed
            return parsed
        }

        return emptyList()
    }

    fun getAll(): List<GameLevel> = cache ?: emptyList()

    fun count(): Int = cache?.size ?: 0

    private fun parseJson(json: String): List<GameLevel> {
        val arr = JSONObject(json).getJSONArray("levels")
        return (0 until arr.length()).map { parseLevel(arr.getJSONObject(it)) }
    }

    private fun parseLevel(obj: JSONObject) = GameLevel(
        id          = obj.getInt("id"),
        levelNumber = obj.getInt("levelNumber"),
        size        = obj.getInt("size"),
        clues       = parseGrid(obj.getJSONArray("clues")),
        constraints = parseConstraints(obj.getJSONArray("constraints")),
        solution    = parseGrid(obj.getJSONArray("solution"))
    )

    private fun parseGrid(arr: JSONArray): List<List<CellElement>> =
        (0 until arr.length()).map { r ->
            val row = arr.getJSONArray(r)
            (0 until row.length()).map { c ->
                when (row.getInt(c)) {
                    1    -> CellElement.FIRE
                    2    -> CellElement.WATER
                    else -> CellElement.EMPTY
                }
            }
        }

    private fun parseConstraints(arr: JSONArray): List<Constraint> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Constraint(
                r1   = o.getInt("r1"),
                c1   = o.getInt("c1"),
                r2   = o.getInt("r2"),
                c2   = o.getInt("c2"),
                type = if (o.getString("type") == "EQUAL") ConstraintType.EQUAL else ConstraintType.DIFFERENT
            )
        }
}
