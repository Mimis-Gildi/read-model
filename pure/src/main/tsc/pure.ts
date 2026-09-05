/*
 * Copyright $YEAR @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 *
 * Fully Refactored: $REFACTORED%.
 * The remainder is validated prototyping slop,
 *   provisionally accepted and temporary.
 */

/*
 * Pure TS fixture -- ETALON: the baseline every other module is measured against.
 *
 * Everything about *measuring* lives in the harness!, and here is the implementation difference between them.
 * Pure TS implementation and React make a DOM tree and fold nodes differently.
 *
 * The DOM here is the contract: benchmark compatibility depends on modules emitting the same shape per row,
 * and the harness queries `.node.collapsed`, `.kids` and `.row > .twist` are comparable on all of them.
 */
import {host, start} from '/harness/read-model-harness.mjs';
import type {Company, CorporateDivision, CorporateGroup, Person, ProductTeam} from '/harness/read-model-harness.mjs';

/** Any rung's node. A culled tree simply has empty child arrays below its level. */
type BenchNode = CorporateDivision | CorporateGroup | ProductTeam | Person;

/** One level of the ladder: how to reach the children, what to write in the row, whether it ships folded. */
interface Rung<N extends BenchNode = BenchNode, C extends BenchNode = BenchNode> {
    readonly children: ((node: N) => readonly C[]) | null;
    readonly label: (node: N) => string;
    readonly meta?: (node: N) => string;
    readonly collapsed?: boolean;
}

/*
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
    rung<CorporateGroup, ProductTeam>({children: (n) => n.teams.asJsReadonlyArrayView(), label: (n) => n.group}),

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

export const OPEN = '▾';
export const SHUT = '▸';
export const LEAF = '·';

const count = (n: number): string => n.toLocaleString();

/** Elements created during a build and counted as they are made. */
let elements = 0;

const make = (tag: string, className: string): HTMLElement => {
    elements += 1;
    const node = document.createElement(tag);
    node.className = className;
    return node;
};

const text = (tag: string, className: string, value: string): HTMLElement => {
    const node = make(tag, className);
    node.textContent = value;
    return node;
};

/**
 * One node of the read model and the fixture to mimic beneath it.
 *
 * The shape is deliberately plain and identical at every level: a row of three spans plus a container for the children.
 * A culled tree simply has empty child arrays below its level,
 * so the same walk renders every rung of the ladder without knowing which rung it is on.
 */
const renderNode = (node: BenchNode, depth: number): HTMLElement => {
    const level = LEVELS[depth]!;
    const children = level.children ? level.children(node) : [];

    const closed = level.collapsed === true && children.length > 0;
    const box = make('div', `node depth-${depth}${closed ? ' collapsed' : ''}`);
    const row = make('div', 'row');

    row.appendChild(text('span', 'twist', children.length ? (closed ? SHUT : OPEN) : LEAF));
    row.appendChild(text('span', 'name', level.label(node)));
    row.appendChild(text('span', 'meta', level.meta ? level.meta(node) : count(children.length)));
    box.appendChild(row);

    if (children.length) {
        const kids = make('div', 'kids');
        children.forEach((child) => kids.appendChild(renderNode(child, depth + 1)));
        box.appendChild(kids);
    }
    return box;
};

/**
 * The measured act: construct the whole culled tree and put it on the page in one operation.
 *
 * Off-document assembly, then a single append, the most practical way to add a large subtree. It keeps `built` a
 * measure of construction rather than of repeated reflow.
 */
const build = (company: Company): number => {
    elements = 0;
    const fragment = document.createDocumentFragment();
    company.divisions.asJsReadonlyArrayView().forEach((division) => fragment.appendChild(renderNode(division, 0)));
    host.get().appendChild(fragment);
    return elements;
};

/** Teardown of the previous rung. Called outside every clock, so a level is never charged for the one before it. */
const reset = (): void => host.get().replaceChildren();

/** The single place a node's collapsed state lives: the class and the twist assure so. */
const shut = (box: Element, closed: boolean): void => {
    box.classList.toggle('collapsed', closed);
    box.querySelector(':scope > .row > .twist')!.textContent = closed ? SHUT : OPEN;
};

/** Rows hidden beneath a folded node -- the unit [unfold] counts and returns, matching the harness's chunk size. */
const rowsOf = (box: Element): number => box.querySelector(':scope > .kids')?.childElementCount ?? 0;

/** Unfolds folded nodes, in document order, until at least [limit] rows are revealed. Returns rows actually revealed. */
const unfold = (limit: number): number => {
    let revealed = 0;
    for (const box of host.get().querySelectorAll('.node.collapsed')) {
        if (revealed >= limit) break;
        revealed += rowsOf(box);
        shut(box, false);
    }
    return revealed;
};

/** Folds every unfolded node that has children. Returns how many were folded. */
const fold = (): number => {
    let folded = 0;
    for (const kids of host.get().querySelectorAll('.kids')) {
        const box = kids.parentElement!;
        if (!box.classList.contains('collapsed')) folded += 1;
        shut(box, true);
    }
    return folded;
};

/**
 * Collapse and expand, delegated to the container.
 *
 * One listener for the whole tree: per-node would be 195,312 of them at LOAD, attached inside the clock and
 * measured as render cost.
 */
host.get().addEventListener('click', (event) => {
    const box = (event.target as Element | null)?.closest('.node');
    if (!box || !box.querySelector(':scope > .kids')) return;
    shut(box, !box.classList.contains('collapsed'));
});

start({build, reset, unfold, fold});
