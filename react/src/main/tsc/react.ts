/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import type {Company, CorporateDivision, CorporateGroup, Person, ProductTeam} from '/harness/read-model-harness.mjs';
/*
 * React fixture -- the same read model the ETALON renders by hand, rendered by React 19 instead.
 *
 * A component at every depth, leaves included: that is what a React application actually costs to render,
 * not the shape most convenient to fake. The DOM it commits matches pure.ts element for element, so the two
 * columns are comparable and one stylesheet dresses both.
 */
import {host, start} from '/harness/read-model-harness.mjs';
import * as Contract from '/harness/read-model-model.mjs';
import React from '/vendor/react/react.mjs';
import {createRoot, type Root} from '/vendor/react/client.mjs';
import {flushSync} from '/vendor/react/react-dom.mjs';

/** Any rung's node. A culled tree simply has empty child arrays below its level. */
type BenchNode = CorporateDivision | CorporateGroup | ProductTeam | Person;

// Intended comma expression use
// noinspection CommaExpressionJS
const also = <T>(x: T, f: (x: T) => void): T => (f(x), x);

/** One level of the ladder: how to reach the children, what to write in the row, whether it ships folded. */
interface Rung<N extends BenchNode = BenchNode, C extends BenchNode = BenchNode> {
    readonly children: ((node: N) => readonly C[]) | null;
    readonly label: (node: N) => string;
    readonly meta?: (node: N) => string;
    readonly collapsed?: boolean;
}

/**
 * CAUTION: one deliberate erasure, in one place. Each level is authored against its own node type -- checked --
 * and the walk consumes them uniformly by depth, which no sound variance rule allows. Widening here keeps the
 * cast out of `LEVELS`.
 */
const rung = <N extends BenchNode, C extends BenchNode>(level: Rung<N, C>): Rung =>
    level as unknown as Rung;

const LEVELS: readonly Rung[] = [
    rung<CorporateDivision, CorporateGroup>({
        children: (division) => division.groups.asJsReadonlyArrayView(),
        label: (division) => division.division,
    }),
    rung<CorporateGroup, ProductTeam>({
        children: (group) => group.teams.asJsReadonlyArrayView(),
        label: (group) => group.group,
    }),

    // Teams ship folded!
    rung<ProductTeam, Person>({
        children: (team) => team.people.asJsReadonlyArrayView(),
        label: (team) => team.team,
        collapsed: true,
    }),
    rung<Person, never>({
        children: null,
        label: (person) => `${person.firstName} ${person.lastName}`,
        meta: (person) => `${person.jobTitle.padEnd(26)}${person.location.padEnd(18)}${person.phone}`,
    }),
];

export const OPEN = Contract.ICON_OPEN.get();
export const SHUT = Contract.ICON_SHUT.get();
export const LEAF = Contract.ICON_LEAF.get();

const count = (quantity: number): string => quantity.toLocaleString();

const DIV = Contract.DOM_KEY_CONTAINER.get() as keyof React.JSX.IntrinsicElements;
const KIDS = ':scope > .kids';
const FOLDED_TEAMS = '.node.depth-2.collapsed';
const OPEN_TEAMS = '.node.depth-2:not(.collapsed)';

/** Elements created during a build, counted as they are made -- the same unit the ETALON reports. */
let elements = 0;

// Intended comma expression use
// noinspection CommaExpressionJS
const countedElement = (type: React.ElementType, props: Record<string, unknown> | null,
                        ...children: React.ReactNode[]) =>
    (elements += 1, React.createElement(type, props, ...children));

/** Every foldable node's state setter, keyed by the element the harness's fold and unfold hand back. */
const setters = new WeakMap<Element, (closed: boolean) => void>();

/**
 * One node of the read model and the fixture to mimic beneath it.
 *
 * Children are rendered whether folded or not: CSS hides them, React still built them -- the ETALON's DOM shape.
 */
const Node = ({node, depth}: {node: BenchNode, depth: number}) => {
    const level = LEVELS[depth]!;
    const children = level.children?.(node) ?? [];
    const [closed, setClosed] = React.useState(level.collapsed === true && children.length > 0);

    return countedElement(DIV, {
            className: `node depth-${depth}${closed ? ' collapsed' : ''}`,
            // Block body: React 19 reads a value returned from a ref callback as a cleanup function, so an
            // expression body would hand it the WeakMap and warn once per foldable node.
            ref: (nodeElement: Element | null) => {
                if (nodeElement) setters.set(nodeElement, setClosed);
            },
        },
        countedElement(DIV, {className: 'row'},
            countedElement('span', {className: 'twist'}, children.length ? (closed ? SHUT : OPEN) : LEAF),
            countedElement('span', {className: 'name'}, level.label(node)),
            countedElement('span', {className: 'meta'}, level.meta?.(node) ?? count(children.length))),
        children.length
            ? countedElement(DIV, {className: 'kids'}, children.map((kid, index) =>
                countedElement(depth === 2 ? Leaf : Node, {node: kid, depth: depth + 1, key: index})))
            : null);
};

/**
 * The bottom rung. No state -- there is nothing to fold -- but a component all the same, since that is how the
 * tree would be written and what it actually costs React to render.
 *
 * Keyed by index deliberately: the list is built once, never reordered or filtered, exactly the case React's own
 * guidance carves out for index keys.
 */
const Leaf = ({node}: {node: BenchNode, depth: number}) => countedElement(DIV, {className: 'node depth-3'},
    countedElement(DIV, {className: 'row'},
        countedElement('span', {className: 'twist'}, LEAF),
        countedElement('span', {className: 'name'}, LEVELS[3]!.label(node)),
        countedElement('span', {className: 'meta'}, LEVELS[3]!.meta!(node))));

/** The root, created outside every clock, so build is construction and commit and nothing else. */
let root: Root = createRoot(host.get());

/**
 * The measured act: build the element tree and commit it in one synchronous flush.
 *
 * `flushSync` is required: React 19's `root.render` only schedules, and the harness stamps `built` the line
 * after this returns. Un-flushed, `built` would read near zero on every rung.
 */
// Intended comma expression use
// noinspection CommaExpressionJS
const rendered = (render: () => void): number => (elements = 0, flushSync(render), elements);

export const build = (company: Company): number =>
    rendered(() => root.render(company.divisions.asJsReadonlyArrayView()
        .map((division, index) => countedElement(Node, {node: division, depth: 0, key: index}))));

/** Teardown of the previous rung, outside every clock: unmounting makes the next build a build, not a diff. */
export const reset = (): void => {
    root.unmount();
    root = createRoot(host.get());
};

/** Rows hidden beneath a folded node -- the unit the harness's chunk size is counted in. */
const rowsOf = (nodeElement: Element): number => nodeElement.querySelector(KIDS)?.childElementCount ?? 0;


interface FoldStep {
    readonly element: Element;
    readonly rows: number;
}

interface FoldPlan {
    readonly running: number;
    readonly steps: readonly FoldStep[];
}

/** Nodes matching [selector], in document order, up to and including the one that carries the total past [limit]. */
const planFor = (selector: string, limit: number): readonly FoldStep[] =>
    [...host.get().querySelectorAll(selector)]
        .map((element): FoldStep => ({element, rows: rowsOf(element)}))
        .reduce<FoldPlan>((plan, step) =>
                plan.running >= limit ? plan : {running: plan.running + step.rows, steps: [...plan.steps, step]},
            {running: 0, steps: []})
        .steps;

/**
 * One flush around the whole chunk, not one per node: React 19 batches state updates dispatched outside its own
 * event handlers, so up to twenty thousand toggles collapse into a single reconciliation over an already-mounted
 * tree -- the number this column exists to produce. A flush per node would be thousands of separate synchronous
 * renders, work no React application would ever do.
 */
const toggleChunk = (selector: string, limit: number, closed: boolean): number =>
    also(planFor(selector, limit), (steps) =>
        flushSync(() => steps.forEach((step) => setters.get(step.element)?.(closed))))
        .reduce((rows, step) => rows + step.rows, 0);

/** Unfolds teams until at least [limit] rows are revealed. Returns rows revealed. */
export const unfold = (limit: number): number => toggleChunk(FOLDED_TEAMS, limit, false);

/** Folds teams until at least [limit] rows are hidden. Returns rows hidden. */
export const fold = (limit: number): number => toggleChunk(OPEN_TEAMS, limit, true);

/** Folds every open team. Returns how many were folded -- teams, not rows: what the button reports. */
export const foldTeams = (): number =>
    also([...host.get().querySelectorAll(OPEN_TEAMS)], (teams) =>
        flushSync(() => teams.forEach((team) => setters.get(team)?.(true))))
        .length;

/**
 * One listener for the whole tree: per-node would be 195,312 of them at LOAD,
 * attached inside the clock and measured as render cost.
 *
 * Current state is read off the class React just rendered, so it cannot disagree with itself.
 */
host.get().addEventListener(Contract.ON_CLICK.get(), (event) =>
    [(event.target as Element | null)?.closest('.node')]
        .filter((nodeElement): nodeElement is Element => nodeElement != null && setters.has(nodeElement))
        .forEach((nodeElement) => setters.get(nodeElement)!(!nodeElement.classList.contains('collapsed'))));

// Placeholders

export const collapse = (): number => 0;

export const setChunkSize = (newChunkSize: number): number => newChunkSize;


/** This fixture is React, and says so itself: the column its reports land in is not something a URL gets a vote on. */
export const uiFramework = Contract.UI_FRAMEWORK_REACT.get().first;

start({uiFramework, build, collapse, reset, setChunkSize, unfold, fold, foldTeams});
