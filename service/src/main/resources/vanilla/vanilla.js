/*
 * Vanilla JS fixture -- the baseline every other module is measured against.
 *
 * Everything about *measuring* lives in the harness. What is left here is the only thing that differs between the
 * modules under test: how a tree of the read model becomes DOM, and how one node folds. Three functions, handed to
 * `start`, and nothing else -- so a number in this column and a number in React's differ where the frameworks do.
 *
 * The DOM this emits is the contract, not an implementation detail: README #3 makes cross-framework comparability
 * depend on every module emitting the same shape per row, and the harness queries `.node.collapsed`, `.kids` and
 * `.row > .twist` on all of them. Change the shape here and every module has to follow.
 */

import {start, host} from '/harness/harness.js';

/** Every level of the read model, in depth order. The tree walk is driven off this, not off of ifs. */
const LEVELS = [
    {children: 'groups', label: (n) => n.division},
    {children: 'teams', label: (n) => n.group},

    // Teams ship folded. At levels 0-2 the culled tree gives a team no people, so there is nothing to fold there;
    // the rule is a no-op -- potential crash on optimal implementation can happen only on level 3.
    // This choice keeps the top rung survivable even at the LOAD levels, expected to produce 200k nodes and a
    // million elements. Destruction is by a button to press rather than an accident that destroys the run.
    // The two costs are thus separated with the People row is an actual bomb:
    // `built` constructs and counts every person,
    // laying People out belongs to the reveal.
    {children: 'people', label: (n) => n.team, collapsed: true},
    {
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        // The rest of the record in the one span the row already has, padded into columns.
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    },
];

const OPEN = '▾';
const SHUT = '▸';
const LEAF = '·';

const count = (n) => n.toLocaleString();

/** Elements created during a build and counted as they are made. */
let elements = 0;

const make = (tag, className) => {
    elements += 1;
    const node = document.createElement(tag);
    node.className = className;
    return node;
};

const text = (tag, className, value) => {
    const node = make(tag, className);
    node.textContent = value;
    return node;
};

/**
 * One node of the read model, and everything beneath it.
 *
 * The shape is deliberately plain and identical at every level:
 * - a row of three spans plus a container for the children.
 *
 * A culled tree simply has empty child arrays below its level,
 * so the same walk renders every rung of the ladder without knowing which rung it is on.
 */
const build = (node, depth) => {
    const level = LEVELS[depth];
    const children = level.children ? node[level.children] : [];

    const closed = level.collapsed && children.length > 0;
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
const attach = (company, into) => {
    elements = 0;
    const fragment = document.createDocumentFragment();
    company.divisions.forEach((division) => fragment.appendChild(build(division, 0)));
    into.appendChild(fragment);
    return elements;
};

/** Teardown of the previous rung. Called outside every clock, so a level is never charged for the one before it. */
const reset = (into) => into.replaceChildren();

/** The single place a node's collapsed state lives: the class and the twist assure so. */
const shut = (box, closed) => {
    box.classList.toggle('collapsed', closed);
    box.querySelector(':scope > .row > .twist').textContent = closed ? SHUT : OPEN;
};

/**
 * Collapse and expand, delegated to the container.
 *
 * One listener for the whole tree instead of one per node: at LOAD that is the difference between 1 listener and
 * 195,312 of them, and attaching those would be measured as render cost.
 */
host.addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (!box || !box.querySelector(':scope > .kids')) return;
    shut(box, !box.classList.contains('collapsed'));
});

start({attach, reset, shut});
