package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.srs.SrsRecord
import com.ktouchie.quizportugues.srs.isReadyForTyping

/** Fraction of a checkpoint's items that must be [isReadyForTyping] before the next checkpoint
 *  unlocks. */
const val CHECKPOINT_MASTERY_THRESHOLD = 0.8

/**
 * A single step in a module's content-progression sequence (docs/MOBILE_APP_SPEC.md §9): the
 * finest-grained unit a learner masters before the next one opens. [level] places the checkpoint
 * at its CEFR position; [rank] breaks ties between checkpoints that share a level using the
 * module's own grouping order (tense for Verb Conjugation, category for every other module) so
 * two checkpoints at the same level unlock one at a time instead of all at once — e.g. an A1
 * verb's "presente" forms are a separate, earlier checkpoint from an A2 verb's "presente" forms
 * and an A1 verb's "pretérito" forms, even though all three can share an *effective* CEFR level
 * once [cefrLevelOf] takes the harder of verb/tense. [key] only distinguishes checkpoints for
 * equality/set membership (e.g. the tense or category name) — it's never read for ordering.
 */
data class Checkpoint(val level: CefrLevel, val rank: Int, val key: String) : Comparable<Checkpoint> {
    override fun compareTo(other: Checkpoint): Int =
        compareValuesBy(this, other, { it.level.ordinal }, { it.rank })
}

/**
 * Which checkpoints are open for content selection in a session. A1's earliest checkpoint is
 * always unlocked; each subsequent checkpoint (in [Checkpoint] order) unlocks once at least
 * [CHECKPOINT_MASTERY_THRESHOLD] of the *previous* checkpoint's items have crossed
 * [isReadyForTyping] — the same bar that promotes an individual item from multiple-choice to
 * typed, now also deciding when the next slice of content breadth opens. This is deliberately
 * stricter than the old tier-only gate's "seen at least once" bar: "mastered" earns the next
 * checkpoint, not merely "encountered". An empty checkpoint (no content assigned, e.g.
 * vocabulary's unused C1/C2 levels) unlocks automatically rather than permanently blocking
 * everything after it.
 *
 * @param itemsByCheckpoint every quizzable item id in the module, grouped by its [Checkpoint].
 * @param records the module's current SRS records, keyed by item id.
 */
fun unlockedCheckpoints(
    itemsByCheckpoint: Map<Checkpoint, List<String>>,
    records: Map<String, SrsRecord>,
): Set<Checkpoint> {
    val unlocked = mutableSetOf<Checkpoint>()
    for (checkpoint in itemsByCheckpoint.keys.sorted()) {
        unlocked += checkpoint
        val items = itemsByCheckpoint.getValue(checkpoint)
        if (items.isEmpty()) continue // nothing to gate on — keep unlocking
        val masteredFraction = items.count { id -> isReadyForTyping(records[id]) }.toDouble() / items.size
        if (masteredFraction < CHECKPOINT_MASTERY_THRESHOLD) break
    }
    return unlocked
}
