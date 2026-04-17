/**
 * @typedef {'presente'|'pretérito'|'imperfeito'|'condicional'|'pretérito mais-que-perfeito'|'perfeito_composto'|'futuro'|'imperativo'|'conjuntivo'|'infinitivo pessoal'} Tense
 */

export const PERSONS = ['eu', 'tu', 'ele/ela/você', 'nós', 'eles/elas/vocês'];

export const TENSE_LABELS = {
    'presente':                    'presente',
    'pretérito':                   'pretérito',
    'imperfeito':                  'imperfeito',
    'condicional':                 'condicional',
    'pretérito mais-que-perfeito': 'pretérito mais-que-perfeito',
    'perfeito_composto':           'perfeito composto',
    'futuro':                      'futuro',
    'imperativo':                  'imperativo',
    'conjuntivo':                  'conjuntivo',
    'infinitivo pessoal':          'infinitivo pessoal',
};

export const STORAGE_KEYS = {
    verbs:  'bestScore_verbs',
    vocab:  'bestScore_vocab',
    gender: 'bestScore_gender',
    theme:  'theme',
};
