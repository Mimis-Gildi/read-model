/*
 * React fixture, matching pure.js element for element and the same Fixture contract Harness.kt calls:
 * build(company), reset(), unfold(limit), fold().
 */
import React from '/vendor/react/react.mjs';
import {createRoot} from '/vendor/react/client.mjs';
import {flushSync} from '/vendor/react/react-dom.mjs';

import {host, start} from '/harness/read-model-harness.mjs';

const LEVELS = [
    {children: (n) => n.groups.asJsReadonlyArrayView(), label: (n) => n.division},
    {children: (n) => n.teams.asJsReadonlyArrayView(), label: (n) => n.group},
    {children: (n) => n.people.asJsReadonlyArrayView(), label: (n) => n.team, collapsed: true},
    {children: null, label: (n) => `${n.firstName} ${n.lastName}`, meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`},
];

const OPEN = '▾';
const SHUT = '▸';
const LEAF = '·';

const count = (n) => n.toLocaleString();

/** Elements created during a build, counted as they are made -- same unit pure.js reports. */
let elements = 0;

const e = (type, props, ...children) => {
    elements += 1;
    return React.createElement(type, props, ...children);
};

/** Every foldable node's state setter, keyed by the DOM element the harness's fold/unfold will hand back. */
const setters = new WeakMap();

/**
 * One node and everything beneath it. A component at every depth, leaves included -- see [Person] --
 * because that is what a React application actually costs to render, not the shape most convenient to fake.
 */
const Node = ({node, depth}) => {
    const level = LEVELS[depth];
    const children = level.children ? level.children(node) : [];
    const [closed, setClosed] = React.useState(Boolean(level.collapsed) && children.length > 0);

    return e('div',
        {
            className: `node depth-${depth}${closed ? ' collapsed' : ''}`,
            // Block body: React 19 treats a value returned from a ref callback as a cleanup function, so an
            // expression body would hand it the WeakMap and log a warning per foldable node.
            ref: (box) => {
                if (box) setters.set(box, setClosed);
            },
        },
        e('div', {className: 'row'},
            e('span', {className: 'twist'}, children.length ? (closed ? SHUT : OPEN) : LEAF),
            e('span', {className: 'name'}, level.label(node)),
            e('span', {className: 'meta'}, level.meta ? level.meta(node) : count(children.length))),
        // Rendered whether folded or not: CSS hides them, React still built them -- matching pure.js's DOM shape.
        children.length ? e('div', {className: 'kids'}, children.map(
            (kid, index) => e(depth === 2 ? Person : Node, {node: kid, depth: depth + 1, key: index}))) : null);
};

/**
 * The leaf. No state -- there is nothing to fold -- but a component all the same, since that is how the tree
 * would be written and what it actually costs React to render.
 *
 * Keyed by index deliberately: the list is built once, never reordered or filtered, exactly the case React's
 * own guidance carves out for index keys.
 */
const Person = ({node}) => e('div', {className: 'node depth-3'},
    e('div', {className: 'row'},
        e('span', {className: 'twist'}, LEAF),
        e('span', {className: 'name'}, LEVELS[3].label(node)),
        e('span', {className: 'meta'}, LEVELS[3].meta(node))));

/** The root, created outside every clock so build() is construction and commit and nothing else. */
let root = null;

/**
 * The measured act: build the element tree and commit it in one synchronous flush.
 *
 * `flushSync` is required: React 19's `root.render` only schedules, and the harness stamps `built` the line
 * after this returns. Un-flushed, `built` would read near zero on every rung.
 */
const build = (company) => {
    elements = 0;
    flushSync(() => root.render(
        company.divisions.asJsReadonlyArrayView().map((division, index) => e(Node, {node: division, depth: 0, key: index}))));
    return elements;
};

/** Teardown of the previous rung, outside every clock: unmounting makes the next build a build, not a diff. */
const reset = () => {
    root?.unmount();
    root = createRoot(host.get());
};

/** Rows hidden beneath a folded node -- matches pure.js's unit, one .kids child per row. */
const rowsOf = (box) => box.querySelector(':scope > .kids')?.childElementCount ?? 0;

/**
 * Unfolds folded nodes, in document order, until at least [limit] rows are revealed. Returns rows revealed.
 *
 * One `flushSync` around the whole chunk, not one per node: React 19 batches state updates dispatched outside
 * its own event handlers, so up to 20,000 toggles collapse into a single reconciliation over an already-mounted
 * tree -- the number this column exists to produce. A flush per node would be thousands of separate synchronous
 * renders, work no React application would ever do.
 */
const unfold = (limit) => {
    let revealed = 0;
    flushSync(() => {
        for (const box of host.get().querySelectorAll('.node.collapsed')) {
            if (revealed >= limit) break;
            revealed += rowsOf(box);
            setters.get(box)?.(false);
        }
    });
    return revealed;
};

/** Folds every unfolded node that has children. Returns how many were folded. */
const fold = () => {
    let folded = 0;
    flushSync(() => {
        for (const kids of host.get().querySelectorAll('.kids')) {
            const box = kids.parentElement;
            if (!box.classList.contains('collapsed')) folded += 1;
            setters.get(box)?.(true);
        }
    });
    return folded;
};

/**
 * Collapse and expand by click, delegated to the container -- one listener for the whole tree rather than
 * one per node. Current state is read off the class React just rendered, so it cannot disagree with itself.
 */
host.get().addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (box && setters.has(box)) setters.get(box)(!box.classList.contains('collapsed'));
});

root = createRoot(host.get());
start({build, reset, unfold, fold});
