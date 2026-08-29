package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** A single preposition+article contraction entry from contractions.json, within its category. */
data class ContractionEntry(
    val category: String,
    val prep: String,
    val article: String,
    val answer: String,
    val example: String?,
    val english: String?,
    val hint: String?,
)

/**
 * Parses contractions.json's raw asset content — `{ category: [{parts: [prep, article], answer,
 * example, english, hint}, ...], ... }` — into a flat list of [ContractionEntry].
 */
fun parseContractionEntries(rawJson: String): List<ContractionEntry> {
    val root = JSONObject(rawJson)
    val result = mutableListOf<ContractionEntry>()

    for (category in root.keys()) {
        val entries = root.getJSONArray(category)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val parts = entry.getJSONArray("parts")
            result.add(
                ContractionEntry(
                    category = category,
                    prep = parts.getString(0),
                    article = parts.getString(1),
                    answer = entry.getString("answer"),
                    example = if (entry.isNull("example")) null else entry.optString("example").ifBlank { null },
                    english = if (entry.isNull("english")) null else entry.optString("english").ifBlank { null },
                    hint = if (entry.isNull("hint")) null else entry.optString("hint").ifBlank { null },
                ),
            )
        }
    }

    return result
}

fun loadContractionEntries(assets: AssetManager): List<ContractionEntry> {
    val json = assets.open("contractions.json").bufferedReader().use { it.readText() }
    return parseContractionEntries(json)
}
