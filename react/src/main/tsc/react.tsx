/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import type {Company, CorporateDivision, CorporateGroup, Person, ProductTeam} from '/harness/read-model-harness.mjs';
import {host, start} from '/harness/read-model-harness.mjs';
import * as Contract from '/harness/read-model-model.mjs';
import React from '/vendor/react/react.mjs';
import {createRoot, type Root} from '/vendor/react/client.mjs';
import {flushSync} from '/vendor/react/react-dom.mjs';
import {create} from '/vendor/react/zustand.mjs';

// Note: I am too lazy to split this toy into modules: mind the dividers below:
/*======================================================================================================================
            Functional Utilities:   this helps me live with decrepit TS.
======================================================================================================================*/

// Intended comma expression use
// noinspection CommaExpressionJS
const also = <T,>(x: T, f: (x: T) => void): T => (f(x), x);

// 569-4287

/*======================================================================================================================
            Benching Structure:     this sets the component structure for DOM benching.
======================================================================================================================*/

/** Any rung's node. A culled tree simply has empty child arrays below its level. */
type BenchNode = CorporateDivision | CorporateGroup | ProductTeam | Person;

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



/*======================================================================================================================
            Selector Constants:     this helps selecting elements the same way in fixtures.
======================================================================================================================*/

export const OPEN = Contract.ICON_OPEN.get();
export const SHUT = Contract.ICON_SHUT.get();
export const LEAF = Contract.ICON_LEAF.get();

const DIV = Contract.DOM_KEY_CONTAINER.get() as keyof React.JSX.IntrinsicElements;
const KIDS = ':scope > .kids';
const FOLDED_TEAMS = '.node.depth-2.collapsed';
const OPEN_TEAMS = '.node.depth-2:not(.collapsed)';



/*======================================================================================================================
            State Management Constructs:    this is the STATE model for React Element Types.
======================================================================================================================*/

/** What a row shows and which ids sit under it. Written once by `present`, never changed after. */
interface Entity {
    readonly id: number;
    readonly depth: number;
    readonly label: string;
    readonly meta: string;
    readonly kids: readonly number[];
}

/** The presentation, not the data: static entities plus the only thing that ever changes -- what is folded. */
interface Presentation {
    readonly roots: readonly number[];
    readonly entities: Readonly<Record<number, Entity>>;
    /** Only nodes with kids have an entry; `undefined` means it cannot fold. */
    readonly folded: Readonly<Record<number, boolean>>;
}

interface CompanyPresentationStore extends Presentation {
    readonly load: (company: Company) => void;
    readonly setFolded: (ids: readonly number[], closed: boolean) => void;
    readonly clear: () => void;
}


/*======================================================================================================================
            State Operations:       this operates on the model of the State.
======================================================================================================================*/

const count = (quantity: number): string => quantity.toLocaleString();

/** One node and everything beneath it, in document order. */
const flatten = (node: BenchNode, depth: number): readonly Entity[] => {
    const level = LEVELS[depth]!;
    const children = level.children?.(node) ?? [];
    return [
        {id: node.id, depth, label: level.label(node), meta: level.meta?.(node) ?? count(children.length), kids: children.map((child) => child.id)},
        ...children.flatMap((child) => flatten(child, depth + 1)),
    ];
};

/** The company as the view sees it. Inside the clock: this is what choosing state costs. */
const present = (company: Company): Presentation => {
    const divisions = company.divisions.asJsReadonlyArrayView();
    const nodes = divisions.flatMap((division) => flatten(division, 0));
    return {
        roots: divisions.map((division) => division.id),
        entities: Object.fromEntries(nodes.map((node) => [node.id, node])),
        folded: Object.fromEntries(nodes
            .filter((node) => node.kids.length > 0)
            .map((node) => [node.id, LEVELS[node.depth]!.collapsed === true])),
    };
};

const ABSENT: Presentation = {roots: [], entities: {}, folded: {}};

/** One `set` per call, however many ids: a chunk is one update, one reconciliation. */
const useCompanyStore = create<CompanyPresentationStore>((set) => ({
    ...ABSENT,
    load: (company) => set(present(company)),
    setFolded: (ids, closed) => set(({folded}) =>
        ({folded: {...folded, ...Object.fromEntries(ids.map((id) => [id, closed]))}})),
    clear: () => set(ABSENT),
}));



/*======================================================================================================================
            DOM and Query Operations:       FixMe: decrepit SLOP for deletion
======================================================================================================================*/




/*======================================================================================================================
            Planning DOM Operations:    this creates a collection of pending changes to STATE (and eventually DOM).
======================================================================================================================*/




/*======================================================================================================================
            Composing DOM Operations:   this writes changes to STATE then BLOCKS to measure time.
======================================================================================================================*/




/*======================================================================================================================
            Rendering: NEW call on a React DOM Element Type.
======================================================================================================================*/

let root: Root = createRoot(host.get());




/*======================================================================================================================
            Components:     utilities to manage DOM scheduling components.
======================================================================================================================*/




/*======================================================================================================================
            Components:     React components for rendering the tree
======================================================================================================================*/

/** The three spans of the DOM contract, shared by every rung. No twist handler here: whoever can fold, passes one. */
const Row = ({entity, folded, onClick}: { entity: Entity, folded?: boolean, onClick?: () => void }) =>
    <div className="row" onClick={onClick}>
        <span className="twist">{folded === undefined ? LEAF : folded ? SHUT : OPEN}</span>
        <span className="name">{entity.label}</span>
        <span className="meta">{entity.meta}</span>
    </div>;

/** Nothing to fold, so nothing to subscribe to: the parent hands the entity down. */
const Leaf = React.memo(({entity}: { entity: Entity }) =>
    <div className={`node depth-${entity.depth}`}>
        <Row entity={entity}/>
    </div>);

/** Kids that can fold are `Node`s, the rest `Leaf`s -- a culled rung's teams included. */
const child = (entity: Entity) => entity.kids.length > 0
    ? <Node key={entity.id} id={entity.id}/>
    : <Leaf key={entity.id} entity={entity}/>;

/**
 * Subscribes to its own fold and to `entities`, which never changes after `load`, so a toggle re-renders this node
 * alone. Folded kids are not hidden, they are not mounted.
 */
const Node = React.memo(({id}: { id: number }) => {
    const entities = useCompanyStore((s) => s.entities);
    const folded = useCompanyStore((s) => s.folded[id]);
    const entity = entities[id]!;
    const toggle = () => useCompanyStore.getState().setFolded([id], !folded);

    return <div className={`node depth-${entity.depth}${folded ? ' collapsed' : ''}`}>
        <Row entity={entity} folded={folded} onClick={folded === undefined ? undefined : toggle}/>
        {folded === false &&
            <div className="kids">
                {entity.kids.map((kid) => child(entities[kid]!))}
            </div>}
    </div>;
});

/** The whole presentation, from its roots. */
const Tree = () => {
    const roots = useCompanyStore((s) => s.roots);
    const entities = useCompanyStore((s) => s.entities);
    return <>{roots.map((id) => child(entities[id]!))}</>;
};




/*======================================================================================================================
            Harness Verbs:      functions expected by the Fixture Harness (Kotlin -> TS)
======================================================================================================================*/

/** Placeholder for refactoring */
export const setChunkSize = (newChunkSize: number): number => newChunkSize;

/**
 * The measured act: present the company into the store, then render the tree and BLOCK until React commits it.
 * `flushSync` is what makes the line after it a meaningful `built` stamp. Counted like the ETALON counts: every element.
 */
export const build = (company: Company): number => {
    useCompanyStore.getState().load(company);
    flushSync(() => root.render(<Tree/>));
    return host.get().querySelectorAll('*').length;
};
/** Placeholder for refactoring */
export const collapse = (): number => 0;

/**
 * Teardown of the previous rung, outside every clock: unmounting makes the next build a build, not a diff.
 * Unmount before clear: a mounted Node reading a cleared store dies on `entities[id]!`.
 */
export const reset = (): void => {
    root.unmount();
    root = createRoot(host.get());
    useCompanyStore.getState().clear();
};

/** Unfolds teams until at least [limit] rows are revealed. Returns rows revealed. */
export const unfold = (limit: number): number => limit;
    // toggleChunk(FOLDED_TEAMS, limit, (refs) => useRevealStore.getState().unfold(refs));

/** Folds teams until at least [limit] rows are hidden. Returns rows hidden. */
export const fold = (limit: number): number => limit;
    // toggleChunk(OPEN_TEAMS, limit, (refs) => useRevealStore.getState().fold(refs));

/** FixMe: Deprecated; delete from Model and Harness. */
export const foldTeams = (): number => 0;



/*======================================================================================================================
            Harness Entrypoint:      functions expected by the Fixture Harness (Kotlin -> TS)
======================================================================================================================*/

export const uiFramework = Contract.UI_FRAMEWORK_REACT.get().first;

start({uiFramework, build, collapse, reset, setChunkSize, unfold, fold, foldTeams});

