/*
 * Copyright $YEAR @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import type {Company, CorporateDivision, CorporateGroup, Person, ProductTeam} from '/harness/read-model-harness.mjs';
/*
 * Pure TS fixture -- ETALON: the baseline every other module is measured against.
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
        children: (n) => n.groups.asJsReadonlyArrayView(),
        label: (n) => n.division,
    }),
    rung<CorporateGroup, ProductTeam>({
        children: (n) => n.teams.asJsReadonlyArrayView(),
        label: (n) => n.group
    }),

    // Teams ship folded!
    rung<ProductTeam, Person>({
        children: (n) => n.people.asJsReadonlyArrayView(),
        label: (n) => n.team,
        collapsed: true,
    }),
    rung<Person, never>({
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    }),
];

export const OPEN = Contract.ICON_OPEN.get();
export const SHUT = Contract.ICON_SHUT.get();
export const LEAF = Contract.ICON_LEAF.get();

const count = (n: number): string => n.toLocaleString();

const newElement = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string): HTMLElementTagNameMap[K] =>
    Object.assign(document.createElement(tag), {className});

const newNestedElement = <K extends keyof HTMLElementTagNameMap>(tag: K, className: string, ...children: readonly Node[]) =>
    also(Object.assign(document.createElement(tag), {className}), (node) => node.append(...children));

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

    return newNestedElement(Contract.DOM_KEY_CONTAINER.get() as keyof HTMLElementTagNameMap, `node depth-${depth}${collapsed ? ' collapsed' : ''}`,
        newNestedElement(Contract.DOM_KEY_CONTAINER.get() as keyof HTMLElementTagNameMap, 'row',
            newTextElement('span', 'twist', children.length ? (collapsed ? SHUT : OPEN) : LEAF),
            newTextElement('span', 'name', level.label(node)),
            newTextElement('span', 'meta', level.meta?.(node) ?? count(children.length))),
        ...(children.length ? [newNestedElement(Contract.DOM_KEY_CONTAINER.get() as keyof HTMLElementTagNameMap, 'kids', ...children.map((child) => renderNode(child, depth + 1)))] : []));
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
    nodeElement.querySelector(':scope > .row > .twist')!.textContent = closed ? SHUT : OPEN;
};

/** Rows hidden beneath a folded node -- the unit [unfold] counts and returns, matching the harness's chunk size. */
const rowsOf = (nodeElement: Element): number => nodeElement.querySelector(':scope > .kids')?.childElementCount ?? 0;

/** Unfolding is a side effect and this meta captures its metadata. */
interface StepForFoldingSideeffect {
    readonly foldableElementWithChildren: Element;
    readonly rows: number;
}

/** Chunking plan to Fold / Un-Fold Side Effect within BUDGET. */
interface UnfoldPlan {
    readonly running: number;
    readonly steps: readonly StepForFoldingSideeffect[];
}


const unfoldPlanProducer = (nodeElements: readonly Element[], limit: number): readonly StepForFoldingSideeffect[] =>
    nodeElements
        .map((nodeElement): StepForFoldingSideeffect => ({foldableElementWithChildren: nodeElement, rows: rowsOf(nodeElement)}))
        .reduce<UnfoldPlan>((plan, step) =>
                plan.running >= limit ? plan : {running: plan.running + step.rows, steps: [...plan.steps, step]},
            {running: 0, steps: []})
        .steps;

/** Unfolds folded nodes, in document order, until at least [limit] rows are revealed. Returns rows actually revealed. */
export const unfold = (limit: number): number =>
    also(unfoldPlanProducer([...host.get().querySelectorAll('.node.collapsed')], limit),
        (steps) => steps.forEach((step) => shut(step.foldableElementWithChildren, false)))
        .reduce((revealed, step) => revealed + step.rows, 0);

/** Folds unfolded team nodes, in document order, until at least [limit] rows are hidden. Returns rows hidden. */
export const fold = (limit: number): number => {
    let hidden = 0;
    for (const nodeElement of host.get().querySelectorAll('.node.depth-2:not(.collapsed)')) {
        if (hidden >= limit) break;
        hidden += rowsOf(nodeElement);
        shut(nodeElement, true);
    }
    return hidden;
};

/** Folds every unfolded node that has children. Returns how many were folded. */
export const foldAll = (): number => {
    let folded = 0;
    for (const kids of host.get().querySelectorAll('.kids')) {
        const nodeElement = kids.parentElement!;
        if (!nodeElement.classList.contains('collapsed')) folded += 1;
        shut(nodeElement, true);
    }
    return folded;
};

/**
 * Collapse and expand, delegated to the container.
 *
 * One listener for the whole tree: per-node would be 195,312 of them at LOAD,
 * attached inside the clock and measured as render cost.
 */
host.get().addEventListener('click', (event) => {
    const nodeElement = (event.target as Element | null)?.closest('.node');
    if (!nodeElement || !nodeElement.querySelector(':scope > .kids')) return;
    shut(nodeElement, !nodeElement.classList.contains('collapsed'));
});

// Placeholders

export const collapse = (): number => {
    return 0
}

export const setChunkSize = (newChunkSize: number): number => {
    return newChunkSize
}


start({build, collapse, reset, setChunkSize, unfold, fold, foldAll});
