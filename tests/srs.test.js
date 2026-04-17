import { describe, it, expect, beforeEach, vi } from 'vitest';
import { sm2, loadSRSState, saveSRSState, getItemSRS, updateItemSRS, getDueItems, isItemDue } from '../srs.js';

const HOUR = 3_600_000;
const DAY = 86_400_000;

describe('sm2 algorithm', () => {
    function freshItem() {
        return { interval: 0, repetitions: 0, easeFactor: 2.5, nextReview: 0 };
    }

    it('sets interval=1 on first correct answer (quality 4)', () => {
        const item = freshItem();
        sm2(item, 4);
        expect(item.interval).toBe(1);
        expect(item.repetitions).toBe(1);
    });

    it('sets interval=3 on second correct answer', () => {
        const item = freshItem();
        sm2(item, 4);
        sm2(item, 4);
        expect(item.interval).toBe(3);
        expect(item.repetitions).toBe(2);
    });

    it('grows interval geometrically after second repetition', () => {
        const item = freshItem();
        sm2(item, 5);
        sm2(item, 5);
        const prevInterval = item.interval;
        sm2(item, 5);
        expect(item.interval).toBeGreaterThan(prevInterval);
    });

    it('resets on quality < 3', () => {
        const item = freshItem();
        sm2(item, 5);
        sm2(item, 5);
        sm2(item, 2); // fail
        expect(item.repetitions).toBe(0);
        expect(item.interval).toBe(1);
    });

    it('schedules nextReview roughly interval days from now', () => {
        const before = Date.now();
        const item = freshItem();
        sm2(item, 4);
        const after = Date.now();
        expect(item.nextReview).toBeGreaterThanOrEqual(before + DAY - HOUR);
        expect(item.nextReview).toBeLessThanOrEqual(after + DAY + HOUR);
    });

    it('keeps easeFactor >= 1.3', () => {
        const item = freshItem();
        for (let i = 0; i < 10; i++) sm2(item, 0);
        expect(item.easeFactor).toBeGreaterThanOrEqual(1.3);
    });
});

describe('SRS state persistence', () => {
    beforeEach(() => localStorage.clear());

    it('loadSRSState returns empty object when nothing stored', () => {
        expect(loadSRSState('test')).toEqual({});
    });

    it('round-trips state through save/load', () => {
        const state = { 'key1': { interval: 3, repetitions: 2, easeFactor: 2.5, nextReview: 12345 } };
        saveSRSState('test', state);
        expect(loadSRSState('test')).toEqual(state);
    });

    it('loadSRSState returns empty object on malformed JSON', () => {
        localStorage.setItem('test', 'not-json');
        expect(loadSRSState('test')).toEqual({});
    });
});

describe('getItemSRS', () => {
    it('returns default item when key is new', () => {
        const state = {};
        const item = getItemSRS(state, 'new-key');
        expect(item.interval).toBe(0);
        expect(item.repetitions).toBe(0);
        expect(item.easeFactor).toBe(2.5);
    });

    it('returns existing item when key exists', () => {
        const state = { 'k': { interval: 7, repetitions: 3, easeFactor: 2.3, nextReview: 9999 } };
        expect(getItemSRS(state, 'k').interval).toBe(7);
    });
});

describe('getDueItems / isItemDue', () => {
    it('returns keys with nextReview in the past', () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-04-17T12:00:00Z'));
        const state = {
            'due':     { interval: 1, repetitions: 1, easeFactor: 2.5, nextReview: Date.now() - DAY },
            'future':  { interval: 3, repetitions: 2, easeFactor: 2.5, nextReview: Date.now() + DAY },
            'new':     { interval: 0, repetitions: 0, easeFactor: 2.5, nextReview: 0 },
        };
        expect(getDueItems(state)).toEqual(['due']);
        expect(isItemDue(state, 'due')).toBe(true);
        expect(isItemDue(state, 'future')).toBe(false);
        expect(isItemDue(state, 'new')).toBe(false);
        vi.useRealTimers();
    });
});

describe('updateItemSRS', () => {
    beforeEach(() => localStorage.clear());

    it('persists updated SRS state', () => {
        const state = {};
        updateItemSRS(state, 'k', 4, 'srs_test');
        const loaded = loadSRSState('srs_test');
        expect(loaded['k']).toBeDefined();
        expect(loaded['k'].repetitions).toBe(1);
    });
});
