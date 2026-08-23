package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** A single Portuguese/English word pair from vocabulary.json, within its category. */
data class VocabularyEntry(
    val category: String,
    val portuguese: String,
    val english: String,
)

/**
 * Parses vocabulary.json's raw asset content — `{ category: { portuguese: english, ... }, ... }`
 * — into a flat list of [VocabularyEntry].
 */
fun parseVocabularyEntries(rawJson: String): List<VocabularyEntry> {
    val root = JSONObject(rawJson)
    val result = mutableListOf<VocabularyEntry>()

    for (category in root.keys()) {
        val words = root.getJSONObject(category)
        for (portuguese in words.keys()) {
            result.add(
                VocabularyEntry(
                    category = category,
                    portuguese = portuguese,
                    english = words.getString(portuguese),
                ),
            )
        }
    }

    return result
}

fun loadVocabularyEntries(assets: AssetManager): List<VocabularyEntry> {
    val json = assets.open("vocabulary.json").bufferedReader().use { it.readText() }
    return parseVocabularyEntries(json)
}
