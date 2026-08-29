package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** A single fill-in-the-blank subjunctive prompt from subjunctive_quiz.json, within its category. */
data class SubjunctiveEntry(
    val category: String,
    val prompt: String,
    val answer: String,
    val trigger: String?,
    val hint: String?,
    val english: String?,
    /** The infinitive shown in parentheses within [prompt] (e.g. "vir" in "___ (vir)"), used to
     *  find "same verb, different form" multiple-choice distractors. Null if the prompt doesn't
     *  match the expected "___ (infinitive)" pattern. */
    val infinitive: String?,
)

private val INFINITIVE_PATTERN = Regex("""\(([^)]+)\)""")

/** Mirrors subjunctive_quiz.js's renderQuestion() regex — the infinitive hint shown in parentheses
 *  right after the blank. */
fun extractSubjunctiveInfinitive(prompt: String): String? =
    INFINITIVE_PATTERN.find(prompt)?.groupValues?.get(1)

/**
 * Parses subjunctive_quiz.json's raw asset content — `{ category: [{prompt, answer, trigger, hint,
 * english}, ...], ... }` — into a flat list of [SubjunctiveEntry].
 */
fun parseSubjunctiveEntries(rawJson: String): List<SubjunctiveEntry> {
    val root = JSONObject(rawJson)
    val result = mutableListOf<SubjunctiveEntry>()

    for (category in root.keys()) {
        val entries = root.getJSONArray(category)
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val prompt = entry.getString("prompt")
            result.add(
                SubjunctiveEntry(
                    category = category,
                    prompt = prompt,
                    answer = entry.getString("answer"),
                    trigger = if (entry.isNull("trigger")) null else entry.optString("trigger").ifBlank { null },
                    hint = if (entry.isNull("hint")) null else entry.optString("hint").ifBlank { null },
                    english = if (entry.isNull("english")) null else entry.optString("english").ifBlank { null },
                    infinitive = extractSubjunctiveInfinitive(prompt),
                ),
            )
        }
    }

    return result
}

fun loadSubjunctiveEntries(assets: AssetManager): List<SubjunctiveEntry> {
    val json = assets.open("subjunctive_quiz.json").bufferedReader().use { it.readText() }
    return parseSubjunctiveEntries(json)
}
