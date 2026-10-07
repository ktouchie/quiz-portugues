import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { padZero, updateBestScore, startTimer, stopTimer } from '../common.js';

describe('padZero', () => {
    it('pads single digit with leading zero', () => expect(padZero(5)).toBe('05'));
    it('pads zero', () => expect(padZero(0)).toBe('00'));
    it('leaves two-digit numbers unchanged', () => expect(padZero(10)).toBe('10'));
    it('leaves larger numbers unchanged', () => expect(padZero(99)).toBe('99'));
});

describe('updateBestScore', () => {
    beforeEach(() => localStorage.clear());

    it('saves the first score as best', () => {
        updateBestScore('test_key', 5);
        expect(localStorage.getItem('test_key')).toBe('5');
    });

    it('replaces best score when new score is higher', () => {
        localStorage.setItem('test_key', '5');
        updateBestScore('test_key', 10);
        expect(localStorage.getItem('test_key')).toBe('10');
    });

    it('does not replace best score when new score is lower', () => {
        localStorage.setItem('test_key', '10');
        updateBestScore('test_key', 5);
        expect(localStorage.getItem('test_key')).toBe('10');
    });

    it('does not replace best score when score is equal', () => {
        localStorage.setItem('test_key', '7');
        updateBestScore('test_key', 7);
        expect(localStorage.getItem('test_key')).toBe('7');
    });
});

describe('timer', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('increments elapsedTime each second', () => {
        const state = { timerInterval: null, elapsedTime: 0, timerDisplay: { innerText: '' } };
        startTimer(state);
        vi.advanceTimersByTime(3000);
        expect(state.elapsedTime).toBe(3);
    });

    it('stops incrementing after stopTimer', () => {
        const state = { timerInterval: null, elapsedTime: 0, timerDisplay: { innerText: '' } };
        startTimer(state);
        vi.advanceTimersByTime(2000);
        stopTimer(state);
        vi.advanceTimersByTime(2000);
        expect(state.elapsedTime).toBe(2);
        expect(state.timerInterval).toBeNull();
    });

    it('clears previous interval on restart', () => {
        const state = { timerInterval: null, elapsedTime: 0, timerDisplay: { innerText: '' } };
        startTimer(state);
        vi.advanceTimersByTime(2000);
        startTimer(state); // restart
        vi.advanceTimersByTime(1000);
        expect(state.elapsedTime).toBe(3);
    });
});
