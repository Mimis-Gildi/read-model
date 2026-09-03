/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 */

module.exports = {
    preset: 'ts-jest/presets/default-esm',
    testEnvironment: 'jest-environment-jsdom', // Hopefully, Browser/DOM
    moduleNameMapper: {
        // Maps harness' ESM modules to the same paths
        '^/harness/read-model-harness\\.mjs$': '<rootDir>/../harness/build/dist/js/productionLibrary/read-model-harness.mjs',

        // Ignore extension spread
        '^(\\.\\.?\\/.+)\\.js$': '$1',
    },
    transform: {
        '^.+\\.tsx?$': [
            'ts-jest',
            {
                useESM: true,
            },
        ],
    },
};
