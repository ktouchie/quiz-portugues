/**
 * Web Speech API wrapper for Portuguese TTS.
 * Gracefully no-ops when SpeechSynthesis is unavailable or pt-PT voice not found.
 */

let _ptVoice = null;
let _checked = false;

function _findVoice() {
    if (_checked) return _ptVoice;
    _checked = true;
    if (typeof speechSynthesis === 'undefined') return null;

    const voices = speechSynthesis.getVoices();
    _ptVoice = voices.find(v => v.lang === 'pt-PT') ||
               voices.find(v => v.lang.startsWith('pt')) ||
               null;
    return _ptVoice;
}

/**
 * @returns {boolean} true if TTS with a Portuguese voice is available
 */
export function isAudioAvailable() {
    if (typeof speechSynthesis === 'undefined') return false;
    _findVoice();
    return _ptVoice !== null;
}

/**
 * Speak text using the best available Portuguese voice.
 * @param {string} text
 */
export function speak(text) {
    if (typeof speechSynthesis === 'undefined') return;
    speechSynthesis.cancel();
    const utt = new SpeechSynthesisUtterance(text);
    utt.lang = 'pt-PT';
    const voice = _findVoice();
    if (voice) utt.voice = voice;
    speechSynthesis.speak(utt);
}

/**
 * Register a handler to refresh voice cache once voices load asynchronously.
 * Call once at page init.
 */
export function initAudio() {
    if (typeof speechSynthesis === 'undefined') return;
    speechSynthesis.addEventListener('voiceschanged', () => {
        _checked = false;
        _findVoice();
    }, { once: true });
    _findVoice();
}
