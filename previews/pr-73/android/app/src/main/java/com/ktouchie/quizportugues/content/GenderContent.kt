package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** A single masculine/feminine/plural word entry from gender_quiz.json, within its category. */
data class GenderEntry(
    val category: String,
    val masculine: String,
    val feminine: String?,
    val plural: String,
    val english: String,
)

/**
 * Parses gender_quiz.json's raw asset content — `{ category: [{masculine, feminine, plural,
 * english}, ...], ... }` — into a flat list of [GenderEntry]. `feminine` is `null` for a handful
 * of invariable-gender words (e.g. "mão", "pão") — mirrors `gender_quiz.js`'s `feminine !== null`
 * check.
 */
fun parseGenderEntries(rawJson: String): List<GenderEntry> {
    val root = JSONObject(rawJson)
    val result = mutableListOf<GenderEntry>()

    for (category in root.keys()) {
        val words = root.getJSONArray(category)
        for (i in 0 until words.length()) {
            val word = words.getJSONObject(i)
            result.add(
                GenderEntry(
                    category = category,
                    masculine = word.getString("masculine"),
                    feminine = if (word.isNull("feminine")) null else word.getString("feminine"),
                    plural = word.getString("plural"),
                    english = word.getString("english"),
                ),
            )
        }
    }

    return result
}

fun loadGenderEntries(assets: AssetManager): List<GenderEntry> {
    val json = assets.open("gender_quiz.json").bufferedReader().use { it.readText() }
    return parseGenderEntries(json)
}
