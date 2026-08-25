package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** A single fill-in-the-blank sentence from ser_estar_ficar.json, within its category. */
data class SerEstarFicarEntry(
    val category: String,
    val sentence: String,
    val answer: String,
    val hint: String?,
    val english: String?,
)

/**
 * Parses ser_estar_ficar.json's raw asset content — `{ category: [{sentence, answer, hint,
 * english}, ...], ... }` — into a flat list of [SerEstarFicarEntry]. Unlike verbs/gender, the
 * wrong-answer hint is per-item content data here, not a category-level lookup table.
 */
fun parseSerEstarFicarEntries(rawJson: String): List<SerEstarFicarEntry> {
    val root = JSONObject(rawJson)
    val result = mutableListOf<SerEstarFicarEntry>()

    for (category in root.keys()) {
        val entries = root.getJSONArray(category)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            result.add(
                SerEstarFicarEntry(
                    category = category,
                    sentence = entry.getString("sentence"),
                    answer = entry.getString("answer"),
                    hint = if (entry.isNull("hint")) null else entry.optString("hint").ifBlank { null },
                    english = if (entry.isNull("english")) null else entry.optString("english").ifBlank { null },
                ),
            )
        }
    }

    return result
}

fun loadSerEstarFicarEntries(assets: AssetManager): List<SerEstarFicarEntry> {
    val json = assets.open("ser_estar_ficar.json").bufferedReader().use { it.readText() }
    return parseSerEstarFicarEntries(json)
}
