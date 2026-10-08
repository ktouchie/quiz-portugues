package com.ktouchie.quizportugues.content

import com.ktouchie.quizportugues.srs.SrsRecord

/** Fraction of a tier's items that must have been reviewed correctly at least once before the
 *  next tier unlocks. */
const val TIER_UNLOCK_THRESHOLD = 0.8

/**
 * Which CEFR tiers are open for content selection in a session (docs/MOBILE_APP_SPEC.md §9).
 *
 * A1 is always unlocked. Each subsequent tier unlocks once at least [TIER_UNLOCK_THRESHOLD] of
 * the previous tier's items have `repetitions > 0` — the same "seen at least once correctly" bar
 * gamification.js/Daos.kt's `countMastered()` already uses, not the stricter typing-readiness bar
 * in `srs/Production.kt` (crossing into production is a per-item decision once an item is
 * reachable; tier unlock is a coarser "is this breadth of content available yet" gate). An empty
 * tier (no content assigned yet, e.g. vocabulary's C1/C2 today) unlocks automatically rather than
 * permanently blocking every tier after it.
 *
 * @param itemsByLevel every quizzable item id in the module, grouped by its CEFR level.
 * @param records the module's current SRS records, keyed by item id.
 */
fun unlockedTiers(
    itemsByLevel: Map<CefrLevel, List<String>>,
    records: Map<String, SrsRecord>,
): Set<CefrLevel> {
    val unlocked = mutableSetOf<CefrLevel>()
    for (level in CefrLevel.entries) {
        unlocked += level
        val items = itemsByLevel[level].orEmpty()
        if (items.isEmpty()) continue // nothing to gate on — keep unlocking
        val seenFraction = items.count { id -> (records[id]?.repetitions ?: 0) > 0 }.toDouble() / items.size
        if (seenFraction < TIER_UNLOCK_THRESHOLD) break
    }
    return unlocked
}
