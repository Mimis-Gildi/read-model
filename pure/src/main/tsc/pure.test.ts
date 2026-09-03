/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import { beforeEach, test, expect, describe, it, jest } from '@jest/globals';

beforeEach(() => {
    // Need to reset Jest's module registry completely for a clean DOM cuz apparently immediate side effects bootstrap.
    jest.resetModules();

    document.body.innerHTML = '';

    // Sude effects! Must match the Kotlin body on Harness!
    const tree = document.createElement('div');
    tree.id = 'tree';
    document.body.appendChild(tree);

    const run = document.createElement('div');
    run.id = 'run';
    document.body.appendChild(run);

    const status = document.createElement('div');
    status.id = 'status';
    document.body.appendChild(status);

    const rows = document.createElement('div');
    rows.id = 'rows';
    document.body.appendChild(rows);

    // 4. Create the helper buttons the harness hooks listeners onto
    const btnStart = document.createElement('button');
    btnStart.id = 'start';
    document.body.appendChild(btnStart);

    const btnExpand = document.createElement('button');
    btnExpand.id = 'expandAll';
    document.body.appendChild(btnExpand);

    const btnCollapse = document.createElement('button');
    btnCollapse.id = 'collapseAll';
    document.body.appendChild(btnCollapse);

    const datasetKey = document.createElement('div');
    datasetKey.id = 'datasetKey';
    document.body.appendChild(datasetKey);

    const revealRow = document.createElement('tr');
    revealRow.id = 'reveal';
    document.body.appendChild(revealRow);

    // ToDo: Did I get it all?
});

test('Application loads and runs without crashing', async () => {
    // Try to dynamically evaluate AFTER the DOM elements exist.
    const app = await import('./pure');
    expect(app).toBeDefined();
});

// Can I get anything from this fixture?
describe('It should correctly show twists', () => {
    it('should show ▾ for open', async () => {
        const { OPEN } = await import('./pure');
        expect(OPEN).toBe('▾');
    });
    it('should show ▸ for shut', async () => {
        const { SHUT } = await import('./pure');
        expect(SHUT).toBe('▸');
    });
    it('should show · for leaf', async () => {
        const { LEAF } = await import('./pure');
        expect(LEAF).toBe('·');
    });
});
