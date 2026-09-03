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
import type {
    Company,
    CorporateDivision,
    CorporateGroup,
    Dataset,
    KtList,
    Person,
    ProductTeam,
} from '/harness/read-model-harness.mjs';

/*
 * CAUTION: the harness hands a fixture the PARSED JSON, never the Kotlin instances. The generated declarations
 * describe instances -- getters, methods, `KtList` -- so they are projected here onto what kotlinx.serialization
 * actually puts on the wire: arrays for lists, the constant's name for an enum, methods dropped.
 * Derived, never transcribed. The model stays Kotlin's to change.
 */
type Wire<T> =
    T extends KtList<infer E> ? readonly Wire<E>[] :
        T extends Dataset ? Dataset['name'] :
            T extends string | number | boolean ? T :
                { readonly [K in keyof T as T[K] extends (...args: never[]) => unknown ? never : K]: Wire<T[K]> };

type WireCompany = Wire<Company>;
type WireDivision = Wire<CorporateDivision>;
type WireGroup = Wire<CorporateGroup>;
type WireTeam = Wire<ProductTeam>;
type WirePerson = Wire<Person>;

/** Any rung's node. A culled tree simply has empty child arrays below its level. */
type BenchNode = WireDivision | WireGroup | WireTeam | WirePerson;

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
    rung<WireDivision, WireGroup>({children: (n) => n.groups, label: (n) => n.division}),
    rung<WireGroup, WireTeam>({children: (n) => n.teams, label: (n) => n.group}),

    // Teams ship folded!
    rung<WireTeam, WirePerson>({children: (n) => n.people, label: (n) => n.team, collapsed: true}),
    rung<WirePerson, never>({
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    }),
];

const OPEN = '▾';
const SHUT = '▸';
const LEAF = '·';

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
const build = (node: BenchNode, depth: number): HTMLElement => {
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
        children.forEach((child) => kids.appendChild(build(child, depth + 1)));
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
const attach = (company: WireCompany, into: Element): number => {
    elements = 0;
    const fragment = document.createDocumentFragment();
    company.divisions.forEach((division) => fragment.appendChild(build(division, 0)));
    into.appendChild(fragment);
    return elements;
};

/** Teardown of the previous rung. Called outside every clock, so a level is never charged for the one before it. */
const reset = (into: Element): void => into.replaceChildren();

/** The single place a node's collapsed state lives: the class and the twist assure so. */
const shut = (box: Element, closed: boolean): void => {
    box.classList.toggle('collapsed', closed);
    box.querySelector(':scope > .row > .twist')!.textContent = closed ? SHUT : OPEN;
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

start({attach, reset, shut});
