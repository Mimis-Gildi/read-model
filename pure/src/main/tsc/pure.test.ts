// noinspection JSUnusedGlobalSymbols

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import {beforeEach, describe, expect, it, jest, test} from '@jest/globals';
import {format as prettyFormat, plugins} from 'pretty-format';
import type {Company} from '/harness/read-model-harness.mjs';


/** Indented DOM dump for eyeballing a built tree -- same serializer Jest uses for its own element diffs. */
const dom: (node: Node) => string = (node: Node): string => prettyFormat(node, {plugins: [plugins.DOMElement]});

const list = <T>(items: readonly T[]) =>
    ({asJsReadonlyArrayView: (): readonly T[] => items});
const person = (id: number) =>
    ({id, firstName: `F${id}`, lastName: `L${id}`, jobTitle: 'Eng', location: 'NYC', phone: '555-0000'});
const team = (id: number, peopleCount: number) =>
    ({
        id, team: `Team${id}`,
        people: list(Array.from({length: peopleCount}, (_, personIndex) => person(personIndex)))
    });
const group = (id: number, ...teams: ReturnType<typeof team>[]) =>
    ({id, group: `Group${id}`, teams: list(teams)});
const division = (id: number, ...groups: ReturnType<typeof group>[]) =>
    ({id, division: `Division${id}`, groups: list(groups)});
const company = (...divisions: ReturnType<typeof division>[]): Company =>
    ({dataset: 'SMOKE', divisions: list(divisions)} as unknown as Company);

/** One division, one group, teams with the given people counts -- enough shape to exercise every rung. */
const companyOfTeams = (...peopleCounts: number[]): Company =>
    company(division(0, group(0, ...peopleCounts.map((peopleCount, teamIndex) =>
        team(teamIndex, peopleCount)))));

/** Descends exactly one level per class, exact parent/child nesting (not just presence anywhere in the DOM). */
const descend: (root: Element, ...classNames: string[]) => Element = (root: Element, ...classNames: string[]): Element =>
    classNames.reduce((ancestor: Element, className: string, depth: number): Element =>
        ancestor.querySelector(depth === 0 ? `:scope > .node.${className}` : `:scope > .kids > .node.${className}`)!, root);


beforeEach(() => {
    // pure.ts runs side effects (host.get(), the click listener) at import time, so a stale module keeps a stale DOM.
    jest.resetModules();

    document.body.innerHTML = '';

    // Must match the ids Harness.kt looks up by getElementById.
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
});

test('Application loads and runs without crashing', async () => {
    const app = await import('./pure');
    expect(app).toBeDefined();
});

describe('It should correctly show twists', () => {
    it('should show ▾ for open', async () => {
        const {OPEN} = await import('./pure');
        expect(OPEN).toBe('▾');
    });
    it('should show ▸ for shut', async () => {
        const {SHUT} = await import('./pure');
        expect(SHUT).toBe('▸');
    });
    it('should show · for leaf', async () => {
        const {LEAF} = await import('./pure');
        expect(LEAF).toBe('·');
    });
});

describe('build', () => {

    /** 1 division + 1 group + 1 team + 2 people = 5 nodes, 3 of them non-leaf. */
    it('creates 5 elements per node, plus one .kids per non-leaf node', async () => {
        const {build} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        const result = build(companyOfTeams(2));
        expect(result).toBe(5 * 5 + 3);

        const leaf = descend(host.get(), 'depth-0', 'depth-1', 'depth-2.collapsed', 'depth-3');
        expect(leaf).not.toBeNull();
        console.log(dom(leaf))

        const rootChildren = host.get().children
        expect(rootChildren.length).toBe(1);
    });

    it('mounts the tree under host', async () => {
        const {build} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        build(companyOfTeams(1));
        const leaf = descend(host.get(), 'depth-0', 'depth-1', 'depth-2.collapsed', 'depth-3');
        expect(leaf).not.toBeNull();
        expect(host.get().children.length).toBe(1);
    });

    it('ships a non-empty team folded', async () => {
        const {build, SHUT} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        build(companyOfTeams(2));
        const team = host.get().querySelector('.depth-2')!;
        expect(team.classList.contains('collapsed')).toBe(true);
        expect(team.querySelector(':scope > .row > .twist')?.textContent).toBe(SHUT);
    });

    it('renders an empty team as a leaf, not folded', async () => {
        const {build, LEAF} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        build(companyOfTeams(0));
        const team = host.get().querySelector('.depth-2')!;
        expect(team.classList.contains('collapsed')).toBe(false);
        expect(team.querySelector(':scope > .row > .twist')?.textContent).toBe(LEAF);
    });
});

describe('reset', () => {
    it('empties the host', async () => {
        const {build, reset} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        expect(host.get().children.length).toBe(0)

        const finalCount = build(companyOfTeams(7, 5, 3));
        expect(finalCount).toBe(105)
        expect(host.get().children.length).toBe(1)

        reset();
        expect(host.get().children.length).toBe(0);
    });
});

describe('unfold', () => {
    it('reveals whole teams, rounding up past the limit', async () => {
        const {build, unfold} = await import('./pure');
        const resultingCount = build(companyOfTeams(9, 7, 5));
        console.log(resultingCount)
        expect(unfold(15)).toBe(16);
    });

    it('returns 0 once every team is already open', async () => {
        const {build, unfold} = await import('./pure');
        build(companyOfTeams(3));
        unfold(100);
        expect(unfold(100)).toBe(0);
    });
});

describe('fold', () => {
    it('folds every open container once, then nothing left to fold', async () => {
        const {build, unfold, foldAll} = await import('./pure');
        build(companyOfTeams(2, 2));
        unfold(2); // opens 1 of the 2 teams
        expect(foldAll()).toBe(3); // division + group + the opened team
        expect(foldAll()).toBe(0);
    });
});

describe('click toggling', () => {
    it('toggles a node that has kids', async () => {
        const {build} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        build(companyOfTeams(1));
        const division = host.get().querySelector('.depth-0')!;
        expect(division.classList.contains('collapsed')).toBe(false);
        (division.querySelector(':scope > .row > .twist') as HTMLElement).click();
        expect(division.classList.contains('collapsed')).toBe(true);
    });

    it('ignores a click on a leaf node', async () => {
        const {build} = await import('./pure');
        const {host} = await import('/harness/read-model-harness.mjs');
        build(companyOfTeams(1));
        const person = host.get().querySelector('.depth-3')!;
        (person.querySelector(':scope > .row > .twist') as HTMLElement).click();
        expect(person.classList.contains('collapsed')).toBe(false);
    });
});
