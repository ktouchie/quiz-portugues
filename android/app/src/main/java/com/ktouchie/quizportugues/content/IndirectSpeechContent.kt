package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONArray

/**
 * A single direct/indirect speech pair from indirect_speech.json. Unlike every other module,
 * indirect_speech.json is a flat JSON array (no categories) — see [parseIndirectSpeechEntries].
 */
data class IndirectSpeechEntry(
    val direct: String,
    val context: String,
    val verbDirect: String,
    val answer: String,
    val rule: String,
    val indirectFull: String?,
    val english: String?,
)

/** Parses indirect_speech.json's raw asset content — a flat `[{direct, context, verb_direct,
 *  answer, rule, indirect_full, english, hint}, ...]` array, no category grouping. */
fun parseIndirectSpeechEntries(rawJson: String): List<IndirectSpeechEntry> {
    val root = JSONArray(rawJson)
    val result = mutableListOf<IndirectSpeechEntry>()

    for (i in 0 until root.length()) {
        val entry = root.getJSONObject(i)
        result.add(
            IndirectSpeechEntry(
                direct = entry.getString("direct"),
                context = entry.getString("context"),
                verbDirect = entry.getString("verb_direct"),
                answer = entry.getString("answer"),
                rule = entry.getString("rule"),
                indirectFull = if (entry.isNull("indirect_full")) null else entry.optString("indirect_full").ifBlank { null },
                english = if (entry.isNull("english")) null else entry.optString("english").ifBlank { null },
            ),
        )
    }

    return result
}

fun loadIndirectSpeechEntries(assets: AssetManager): List<IndirectSpeechEntry> {
    val json = assets.open("indirect_speech.json").bufferedReader().use { it.readText() }
    return parseIndirectSpeechEntries(json)
}
