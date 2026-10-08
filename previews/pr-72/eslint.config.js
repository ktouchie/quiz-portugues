import js from '@eslint/js';

export default [
    js.configs.recommended,
    {
        languageOptions: {
            ecmaVersion: 2022,
            sourceType: 'module',
            globals: {
                window: 'readonly',
                document: 'readonly',
                localStorage: 'readonly',
                fetch: 'readonly',
                console: 'readonly',
                alert: 'readonly',
                location: 'readonly',
                setTimeout: 'readonly',
                clearTimeout: 'readonly',
                setInterval: 'readonly',
                clearInterval: 'readonly',
                SpeechSynthesisUtterance: 'readonly',
                speechSynthesis: 'readonly',
            },
        },
        rules: {
            'no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
            'no-console': 'off',
            'eqeqeq': 'error',
            'no-var': 'error',
            'prefer-const': 'error',
        },
    },
    {
        files: ['tests/**/*.test.js'],
        languageOptions: {
            globals: {
                describe: 'readonly',
                it: 'readonly',
                expect: 'readonly',
                beforeEach: 'readonly',
                afterEach: 'readonly',
                vi: 'readonly',
            },
        },
    },
    {
        ignores: ['node_modules/'],
    },
];
