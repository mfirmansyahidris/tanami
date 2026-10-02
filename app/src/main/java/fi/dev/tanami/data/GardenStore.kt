package fi.dev.tanami.data

import android.content.Context

class GardenStore(context: Context) {
    private val preferences = context.getSharedPreferences("tanami_garden", Context.MODE_PRIVATE)

    fun loadPlants(): List<GardenEntry> = preferences.getStringSet(KEY_PLANTS, emptySet())
        ?.mapNotNull { encoded ->
            val parts = encoded.split("|", limit = 2)
            val startedAt = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            GardenEntry(
                plantId = parts[0],
                startedAt = startedAt,
                lastWateredAt = readCareTime(parts[0], CARE_WATER),
                lastFedAt = readCareTime(parts[0], CARE_FEED)
            )
        }
        .orEmpty()

    fun addPlant(id: String, startedAt: Long = System.currentTimeMillis()) {
        val updated = loadPlants().filterNot { it.plantId == id }.map { "${it.plantId}|${it.startedAt}" }
            .toSet() + "$id|$startedAt"
        preferences.edit().putStringSet(KEY_PLANTS, updated).apply()
    }

    fun removePlant(id: String) {
        val updated = loadPlants().filterNot { it.plantId == id }
            .map { "${it.plantId}|${it.startedAt}" }.toSet()
        val editor = preferences.edit().putStringSet(KEY_PLANTS, updated)
        preferences.all.keys.filter { it.startsWith("$CARE_PREFIX$id.") }.forEach { key -> editor.remove(key) }
        editor.apply()
    }

    fun recordCare(id: String, care: String, at: Long = System.currentTimeMillis()) {
        require(care in setOf(CARE_WATER, CARE_FEED))
        preferences.edit().putLong(careKey(id, care), at).apply()
    }

    fun loadCountry(): String = preferences.getString(KEY_COUNTRY, DEFAULT_COUNTRY) ?: DEFAULT_COUNTRY

    fun saveCountry(country: String) {
        preferences.edit().putString(KEY_COUNTRY, country).apply()
    }

    private fun readCareTime(id: String, care: String): Long? =
        if (preferences.contains(careKey(id, care))) preferences.getLong(careKey(id, care), 0L) else null

    private fun careKey(id: String, care: String) = "$CARE_PREFIX$id.$care"

    private companion object {
        const val KEY_PLANTS = "plants"
        const val KEY_COUNTRY = "country"
        const val DEFAULT_COUNTRY = "Indonesia"
        const val CARE_PREFIX = "care."
        const val CARE_WATER = "water"
        const val CARE_FEED = "feed"
    }
}

data class GardenEntry(
    val plantId: String,
    val startedAt: Long,
    val lastWateredAt: Long? = null,
    val lastFedAt: Long? = null
)
