/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import type {Company, CorporateDivision, CorporateGroup, Person, ProductTeam} from '/harness/read-model-harness.mjs';
/*
 * Pure TS fixture -- ETALON: the baseline every other framework is measured against.
 *
 * Everything about *measuring* contracts is established in the harness!
 * Here is the implementation `Fixture` -- the functional part for the fixture.
 * Pure TS, React, React App, and KobWeb make a DOM tree and fold nodes differently
 * and each provides its own implementation of the fixture instrumentation.
 *
 * The DOM adheres to the contract. And this is the first implementation of it.
 * It is synchronous and blocking.
 */
import {host, start} from '/harness/read-model-harness.mjs';
import * as Contract from '/harness/read-model-model.mjs';

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
        label: (group) => group.group
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

const DIV = Contract.DOM_KEY_CONTAINER.get() as keyof HTMLElementTagNameMap;
const KIDS = ':scope > .kids';
const TWIST = ':scope > .row > .twist';
const FOLDED_TEAMS = '.node.depth-2.collapsed';
const OPEN_TEAMS = '.node.depth-2:not(.collapsed)';

const newElement = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string): HTMLElementTagNameMap[K] =>
    Object.assign(document.createElement(tag), {className});

const newNestedElement = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string, ...children: readonly Node[]) =>
    also(newElement(tag, className), (node) => node.append(...children));

const newTextElement = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string, textContent: string) =>
    Object.assign(newElement(tag, className), {textContent});


/**
 * One node of the read model and the fixture to mimic beneath it.
 *
 * The shape is deliberately plain and identical at every level: a row of three spans plus a container for the children.
 * A culled tree simply has empty child arrays below its level,
 * so the same walk renders every rung of the ladder without knowing which rung it is on.
 */
const renderNode = (node: BenchNode, depth: number): HTMLElement => {
    const level = LEVELS[depth]!;
    const children = level.children?.(node) ?? [];
    const collapsed = level.collapsed === true && children.length > 0;

    return newNestedElement(DIV, `node depth-${depth}${collapsed ? ' collapsed' : ''}`,
        newNestedElement(DIV, 'row',
            newTextElement('span', 'twist', children.length ? (collapsed ? SHUT : OPEN) : LEAF),
            newTextElement('span', 'name', level.label(node)),
            newTextElement('span', 'meta', level.meta?.(node) ?? count(children.length))),
        ...(children.length ? [newNestedElement(DIV, 'kids', ...children.map((child) => renderNode(child, depth + 1)))] : []));
};

/**
 * The measured act: construct the whole culled tree and put it on the page in one operation.
 *
 * Off-document assembly, then a single append, the most practical way to add a large subtree. It keeps `built` a
 * measure of construction rather than of repeated reflow.
 */
export const build = (company: Company): number => {
    const fragment = also(document.createDocumentFragment(), (frag) =>
        frag.append(...company.divisions.asJsReadonlyArrayView().map((division) => renderNode(division, 0))));
    return also(fragment.querySelectorAll('*').length, () => host.get().appendChild(fragment));
};

/** Teardown of the previous rung. Called outside every clock, so a level is never charged for the one before it. */
export const reset = (): void => host.get().replaceChildren();

/** The single place a node's collapsed state lives: the class and the twist assure so. */
const shut = (nodeElement: Element, closed: boolean): void => {
    nodeElement.classList.toggle('collapsed', closed);
    nodeElement.querySelector(TWIST)!.textContent = closed ? SHUT : OPEN;
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

const toggleChunk = (selector: string, limit: number, closed: boolean): number =>
    also(planFor(selector, limit), (steps) => steps.forEach((step) => shut(step.element, closed)))
        .reduce((rows, step) => rows + step.rows, 0);

/** Unfolds teams until at least [limit] rows are revealed. Returns rows revealed. */
export const unfold = (limit: number): number => toggleChunk(FOLDED_TEAMS, limit, false);

/** Folds teams until at least [limit] rows are hidden. Returns rows hidden. */
export const fold = (limit: number): number => toggleChunk(OPEN_TEAMS, limit, true);

/** Folds every open team. Returns how many were folded -- teams, not rows: what the button reports. */
export const foldTeams = (): number =>
    also([...host.get().querySelectorAll(OPEN_TEAMS)],
        (elements) => elements.forEach((element) => shut(element, true)))
        .length;

/**
 * One listener for the whole tree: per-node would be 195,312 of them at LOAD,
 * attached inside the clock and measured as render cost.
 */
host.get().addEventListener(Contract.ON_CLICK.get(), (event) =>
    [(event.target as Element | null)?.closest('.node')]
        .filter((nodeElement): nodeElement is Element => nodeElement?.querySelector(KIDS) != null)
        .forEach((nodeElement) => shut(nodeElement, !nodeElement.classList.contains('collapsed'))));

// Placeholders

export const collapse = (): number => 0;

export const setChunkSize = (newChunkSize: number): number => newChunkSize;


/** The ETALON names itself: the column its reports land in is not something a URL gets a vote on. */
export const uiFramework = Contract.UI_FRAMEWORK_PURE_TS.get().first;

start({uiFramework, build, collapse, reset, setChunkSize, unfold, fold, foldTeams});
