package com.ktouchie.quizportugues.content

import android.content.res.AssetManager
import org.json.JSONObject

/** Mirrors config.js' PERSONS — index into a tense's 5-element conjugation array. */
val PERSONS = listOf("eu", "tu", "ele/ela/você", "nós", "eles/elas/vocês")

/**
 * The 10 conjugation tenses tracked in verbs.json, in the order they appear there. Mirrors
 * config.js' TENSE_LABELS keys.
 */
val VERB_TENSES = listOf(
    "presente",
    "pretérito",
    "imperfeito",
    "condicional",
    "pretérito mais-que-perfeito",
    "perfeito_composto",
    "futuro",
    "imperativo",
    "conjuntivo",
    "infinitivo pessoal",
)

enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    ;

    companion object {
        fun fromJson(value: String): Difficulty = when (value) {
            "beginner" -> BEGINNER
            "intermediate" -> INTERMEDIATE
            "advanced" -> ADVANCED
            else -> error("Unknown difficulty: $value")
        }
    }
}

/**
 * A single verb's full conjugation entry from verbs.json. Entries that only carry
 * `participios_passados` (no full conjugation table) are not represented here — see
 * [loadVerbEntries] — they're out of scope for the v1 Verb Conjugation module
 * (docs/MOBILE_APP_SPEC.md §14).
 */
data class VerbEntry(
    val verb: String,
    val regular: Boolean,
    val difficulty: Difficulty,
    val english: String,
    /** tense name -> 5 conjugated forms, one per [PERSONS] index. */
    val tenses: Map<String, List<String>>,
    /** tense name -> example sentences, one per [PERSONS] index. Only populated for a subset of
     *  tenses (presente/pretérito) on ~16 high-frequency verbs; empty map otherwise. */
    val examples: Map<String, List<String>>,
)

/**
 * Parses verbs.json's raw asset content into [VerbEntry] instances, one per verb that has a full
 * conjugation table (i.e. carries a "difficulty" field). Verbs that only supply
 * `participios_passados` are skipped.
 */
fun parseVerbEntries(rawJson: String): Map<String, VerbEntry> {
    val root = JSONObject(rawJson)
    val result = LinkedHashMap<String, VerbEntry>()

    for (verb in root.keys()) {
        val obj = root.getJSONObject(verb)
        if (!obj.has("difficulty")) continue // participios_passados-only entry — out of scope for v1

        val tenses = LinkedHashMap<String, List<String>>()
        for (tense in VERB_TENSES) {
            if (!obj.has(tense)) continue
            val arr = obj.getJSONArray(tense)
            tenses[tense] = List(arr.length()) { arr.getString(it) }
        }

        val examples = LinkedHashMap<String, List<String>>()
        if (obj.has("exemplos")) {
            val exemplos = obj.getJSONObject("exemplos")
            for (tense in exemplos.keys()) {
                val arr = exemplos.getJSONArray(tense)
                examples[tense] = List(arr.length()) { arr.getString(it) }
            }
        }

        result[verb] = VerbEntry(
            verb = verb,
            regular = obj.getBoolean("regular"),
            difficulty = Difficulty.fromJson(obj.getString("difficulty")),
            english = obj.getString("english"),
            tenses = tenses,
            examples = examples,
        )
    }

    return result
}

fun loadVerbEntries(assets: AssetManager): Map<String, VerbEntry> {
    val json = assets.open("verbs.json").bufferedReader().use { it.readText() }
    return parseVerbEntries(json)
}
