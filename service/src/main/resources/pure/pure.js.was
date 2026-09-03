/*
 * Pure JS fixture -- ETALON: the baseline every other module is measured against.
 *
 * Everything about *measuring* lives in the `harness.js`!, and here is the implementation difference between them.
 * Pure JS implementation and React make a DOM tree and fold nodes differently.
 *
 * The DOM here is the contract: benchmark compatibility depends on modules emitting the same shape per row,
 * and the harness queries `.node.collapsed`, `.kids` and `.row > .twist` are comparable on all of them.
 */
import {start, host} from '/harness/harness.js';
import {LEVELS, OPEN, SHUT, LEAF, count} from '/harness/model.js';

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
 * One node of the read model and the fixture to mimic beneath it.
 *
 * The shape is deliberately plain and identical at every level: a row of three spans plus a container for the children.
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
 * One listener for the whole tree: per-node would be 195,312 of them at LOAD, attached inside the clock and
 * measured as render cost.
 */
host.addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (!box || !box.querySelector(':scope > .kids')) return;
    shut(box, !box.classList.contains('collapsed'));
});

start({attach, reset, shut});
