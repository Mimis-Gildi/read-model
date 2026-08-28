/*
 * React fixture -- the same three functions the Pure JS module supplies, rendered by React 19 instead of by hand.
 *
 * The rules this file obeys come from the harness, not from taste, and each one is load-bearing:
 *
 *   1. `attach` must render SYNCHRONOUSLY. The harness stamps `built` on the line after `attach` returns, and
 *      `root.render` in React 19 only schedules. Un-flushed, `built` would read near zero on every rung and the
 *      column would be a fiction. One [flushSync] per rung -- never per node -- puts construction and commit inside
 *      the clock, which is exactly what the pure column measures.
 *
 *   2. Folded children are HIDDEN, not unmounted. A React developer would unmount them, and that is the honest
 *      thing to say in the write-up -- but it is not the same act as the one in the next column. Vanilla's People
 *      rung constructs all 187,500 person rows and hides them in CSS; unmounting would have React construct ~7,800
 *      team rows instead and defer the rest into the reveal, breaking the comparability of BOTH rows at once. The
 *      benchmark asks "render this tree", and both columns must answer the same question.
 *
 *   3. The DOM shape is the vanilla shape, element for element. README #3 makes cross-framework comparability
 *      depend on it, `bench.css` styles it, and the harness reaches into `.node.collapsed`, `.kids` and
 *      `.row > .twist` on every module. A leaf is five elements here because it is five elements there.
 *
 * Vendored React, served from this host: a benchmark that reaches for a CDN measures the CDN. See /vendor.
 */

import React from '/vendor/react/react.mjs';
import {createRoot} from '/vendor/react/client.mjs';
import {flushSync} from '/vendor/react/react-dom.mjs';

import {start, host} from '/harness/harness.js';

/** Every level of the read model, in depth order -- the same table the vanilla module walks, for the same reason. */
const LEVELS = [
    {children: 'groups', label: (n) => n.division},
    {children: 'teams', label: (n) => n.group},

    // Teams ship folded, so the People rung constructs every person and lays out none of them. Laying them out is
    // the reveal, and it is a rung of its own.
    {children: 'people', label: (n) => n.team, collapsed: true},
    {
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    },
];

const OPEN = '▾';
const SHUT = '▸';
const LEAF = '·';

const count = (n) => n.toLocaleString();

/**
 * Host elements created during a build, counted as they are made.
 *
 * Only host tags are counted, never the [Node] component wrapper: a component element produces no DOM of its own,
 * and the column is a count of DOM, so React's total lands on the same footing as the vanilla one.
 */
let elements = 0;

const e = (type, props, ...children) => {
    elements += 1;
    return React.createElement(type, props, ...children);
};

/**
 * Every foldable node's state setter, keyed by the DOM element the harness will hand back.
 *
 * The harness folds by DOM -- `shut(box, closed)` receives an element it found with `querySelectorAll`. React folds
 * by state. This map is the bridge, and a ref callback is what fills it, so registration costs one callback per
 * FOLDABLE node (~7,800 at LOAD) rather than one per node (195,312). Weak, so an unmounted rung's entries go with it.
 */
const setters = new WeakMap();

/**
 * One node of the read model and everything beneath it, for the three depths that have children.
 *
 * Every node in this fixture is a component, leaves included -- see [Person]. That is what a React application is,
 * and it is what this column has to measure. An earlier version of this file built the 187,500 people as plain
 * `createElement` host elements on the argument that no developer gives a leaf `useState`. True, and beside the
 * point: not holding state and not being a component are different decisions, and collapsing them skipped React's
 * per-component cost for 96% of the tree. The React column then read ~1.4x vanilla where krausest's published
 * figure is 1.80x at a hundredth of the scale -- flattering in the wrong direction, and for the wrong reason.
 */
const Node = ({node, depth}) => {
    const level = LEVELS[depth];
    const children = node[level.children];
    const [closed, setClosed] = React.useState(Boolean(level.collapsed) && children.length > 0);

    return e('div',
        {
            className: `node depth-${depth}${closed ? ' collapsed' : ''}`,
            // A block body on purpose: React 19 treats a value returned from a ref callback as a cleanup function,
            // so an expression body would hand it the WeakMap and earn a console warning per foldable node --
            // thousands of them, inside the clock.
            ref: (box) => {
                if (box) setters.set(box, setClosed);
            },
        },
        e('div', {className: 'row'},
            e('span', {className: 'twist'}, children.length ? (closed ? SHUT : OPEN) : LEAF),
            e('span', {className: 'name'}, level.label(node)),
            e('span', {className: 'meta'}, count(children.length))),
        // Rendered whether folded or not -- see rule 2 in the header. CSS hides them; React still built them.
        children.length ? e('div', {className: 'kids'}, children.map(
            (kid, index) => React.createElement(
                depth === 2 ? Person : Node,
                {node: kid, depth: depth + 1, key: index}))) : null);
};

/**
 * A person: the leaf, and a component like every other node here.
 *
 * No state -- there is nothing to fold -- but a component all the same, because that is how the tree would be
 * written and therefore what React actually costs to render it. 187,500 of these is where the React column's real
 * number lives.
 *
 * Keyed by index at the call site deliberately. The list is built once, never reordered, never filtered and never
 * has an item removed, which is precisely the case React's own guidance carves out for index keys. The alternative
 * -- a composite string per node -- would be 187,500 string concatenations inside the clock, work a real
 * application would not do, and a result anyone would be right to call rigged.
 */
const Person = ({node}) => e('div', {className: 'node depth-3'},
    e('div', {className: 'row'},
        e('span', {className: 'twist'}, LEAF),
        e('span', {className: 'name'}, LEVELS[3].label(node)),
        e('span', {className: 'meta'}, LEVELS[3].meta(node))));

/**
 * The measured act: build the element tree and commit it in a single synchronous flush.
 *
 * The root is created in [reset], outside the clock, so `attach` is construction and commit and nothing else.
 */
let root = null;

const attach = (company, into) => {
    elements = 0;
    root = root ?? createRoot(into);
    flushSync(() => root.render(
        company.divisions.map((division, index) => React.createElement(Node, {node: division, depth: 0, key: index}))));
    return elements;
};

/**
 * Teardown of the previous rung. Called outside every clock, so a level is never charged for the one before it --
 * and unmounting rather than re-rendering means the next rung is a build, not a diff against the rung before it.
 */
const reset = (into) => {
    root?.unmount();
    root = createRoot(into);
};

/**
 * Fold or unfold one node, by state.
 *
 * The harness calls this up to 20,000 times inside one clock during a reveal. React 19 batches updates dispatched
 * outside its own event handlers, so those toggles collapse into a single reconciliation over an already-mounted
 * tree -- which is the React number this whole column exists to produce.
 *
 * One consequence to read the board with: because the batch commits after the synchronous loop returns, React's
 * `built` on the Reveal rung is the cost of DISPATCHING the toggles, and the reconciliation lands in `painted`.
 * Pure JS' split does not shift that way, so the Reveal rung is compared on `painted`. The alternative -- a
 * [flushSync] per node -- would be 20,000 separate synchronous renders, a number no React application would ever
 * produce and a slower one than the framework deserves.
 */
const shut = (box, closed) => setters.get(box)?.(closed);

/**
 * A chunk of folding, flushed before the harness stamps it.
 *
 * [shut] only DISPATCHES: react.dev states plainly that a render is scheduled rather than run on the spot, so a
 * chunk's reconciliation would otherwise happen at a moment React picks -- possibly after the harness's double
 * `requestAnimationFrame` had already taken its reading. That would put part of the cost outside every cell, where
 * the only trace of it is a wall clock disagreeing with the board.
 *
 * One flush per chunk, so the batching is intact: 20,000 toggles still collapse into one reconciliation, which is
 * the React number this column exists to produce. It is now inside the clock rather than somewhere after it.
 */
const batch = (work) => flushSync(work);

/**
 * Collapse and expand by click, delegated to the container -- one listener for the whole tree rather than 195,312.
 *
 * Current state is read off the class rather than kept in a second place: the class is what React just rendered,
 * so it cannot disagree with the state that produced it.
 */
host.addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (box && setters.has(box)) shut(box, !box.classList.contains('collapsed'));
});

start({attach, reset, shut, batch});
