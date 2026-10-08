package com.ktouchie.quizportugues.content

import java.text.Normalizer
import kotlin.random.Random

/** Number of wrong options shown next to the right one in a multiple-choice question. */
const val DISTRACTOR_COUNT = 3

/**
 * Picks up to [DISTRACTOR_COUNT] wrong options for a multiple-choice question, same rule as the
 * web app's `buildOptions` in practice.js: candidates from [pools] in order (each pool shuffled),
 * skipping any that read the same as the correct answer or as an option already picked, ignoring
 * case and Unicode form. Different words can share a translation (sete e meia and dezanove e trinta are both
 * "seven thirty"), so without this a question could show the same option twice, or the right
 * answer as a "wrong" one.
 */
fun pickDistractors(correct: String, pools: List<List<String>>, random: Random = Random.Default): List<String> {
    val seen = mutableSetOf(correct.normalised())
    val distractors = mutableListOf<String>()
    for (pool in pools) {
        for (candidate in pool.shuffled(random)) {
            if (distractors.size == DISTRACTOR_COUNT) return distractors
            if (seen.add(candidate.normalised())) distractors += candidate
        }
    }
    return distractors
}

private fun String.normalised(): String = Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFC)
