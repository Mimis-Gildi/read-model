/*
 * Vanilla JS fixture -- the baseline.
 *
 * Measurement boundary, as set by @rdd13r:
 *   - fetch, JSON parse, and everything else infrastructural are OUTSIDE the clock;
 *   - the clock starts on the button, with the model already in hand;
 *   - it stops when everything is rendered.
 *
 * "Rendered" has two honest readings, so both are reported rather than one being chosen here:
 *   built   -- the whole tree constructed and attached to the live document, measured
 *              synchronously on the line after the attach returns. No layout, no paint.
 *   painted -- the frame after that, so layout and paint the attach provoked are included.
 * The gap between them is the browser's cost of the same DOM, and it is the number the article
 * is arguably about.
 */

/**
 * Every level of the read model, in depth order. The tree walk is driven off this, not off ifs.
 *
 * `collapsed` is the level whose children start hidden -- teams, so the people are built into the
 * document but not laid out. That keeps "all rendered" literally true (every node is in the DOM
 * and counted) while sparing the browser 187,500 boxes it was never asked to show at once.
 */
const LEVELS = [
    {children: 'groups', label: (n) => n.division},
    {children: 'teams', label: (n) => n.group},
    {children: 'people', label: (n) => n.team, collapsed: true},
    {
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        // The rest of the record in the one span the row already has, padded into columns.
        // The page is monospace, so padding buys alignment for nothing: a leaf shows its whole
        // record at the same element count as a container row, which is what keeps the levels
        // comparable and the variable under test the tree rather than the ornament.
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    },
];

const OPEN = '▾';
const SHUT = '▸';
const LEAF = '·';

const params = new URLSearchParams(location.search);
const dataset = params.get('dataset');
const run = params.get('run');

const el = (id) => document.getElementById(id);
const elTree = el('tree');
const elRender = el('render');
const elStatus = el('status');

const count = (n) => n.toLocaleString();

/** Elements created during a build. Counted as they are made -- no post-hoc DOM walk to pay for. */
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
 * The shape is deliberately plain and identical at every level -- a row of two or three spans
 * plus a container for the children -- because README #3 makes cross-framework comparability
 * depend on every module emitting the same DOM per row.
 *
 * Built fully expanded: the point of the exercise is how many live nodes the browser will hold,
 * so nothing is withheld from the first render.
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

/** Resolves on the frame after the one the caller's DOM work lands in -- i.e. once it is painted. */
const nextPaint = () => new Promise((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve(performance.now()))));

const render = async (company) => {
    elRender.disabled = true;
    elStatus.textContent = 'Rendering…';
    elTree.replaceChildren();
    elements = 0;

    // Off-document assembly, then a single attach: the honest vanilla way to add a large subtree,
    // and the one that keeps "built" a measure of construction rather than of repeated reflow.
    const fragment = document.createDocumentFragment();
    const started = performance.now();
    company.divisions.forEach((division) => fragment.appendChild(build(division, 0)));
    elTree.appendChild(fragment);
    const built = performance.now();

    const painted = await nextPaint();

    el('built').textContent = `${(built - started).toFixed(1)} ms`;
    el('painted').textContent = `${(painted - started).toFixed(1)} ms`;
    el('domNodes').textContent = count(elements);
    elStatus.textContent = 'Rendered.';
    [elRender, el('expandAll'), el('collapseAll')].forEach((button) => button.disabled = false);
};

/** Model nodes, counted off the served tree rather than derived -- the service already derives it. */
const census = (company) => company.divisions.reduce(
    (total, division) => total + 1 + division.groups.reduce(
        (g, group) => g + 1 + group.teams.reduce((t, team) => t + 1 + team.people.length, 0), 0), 0);

/**
 * Collapse and expand, delegated to the container.
 *
 * One listener for the whole tree instead of one per node: at LOAD that is the difference between
 * 1 listener and 195,312 of them, and attaching those would be measured as render cost.
 */
elTree.addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (!box || !box.querySelector(':scope > .kids')) return;
    shut(box, !box.classList.contains('collapsed'));
});

/** The single place a node's collapsed state lives: the class and the twist never disagree. */
const shut = (box, closed) => {
    box.classList.toggle('collapsed', closed);
    box.querySelector(':scope > .row > .twist').textContent = closed ? SHUT : OPEN;
};

/**
 * Expand everything, or collapse everything below the divisions.
 *
 * Collapsing every node still leaves the twelve division rows on screen -- there is nothing above
 * them to hide them -- so "collapse all" and "all but level one" are the same gesture.
 *
 * Both walk the whole tree. That walk is a real cost at LOAD, and it is deliberately outside the
 * measured render: it is an interaction, not a first paint, and mixing the two would make the
 * headline number mean nothing.
 */
const bulk = (closed) => async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    const started = performance.now();
    // Selecting `.kids` and stepping up beats `.node:has(> .kids)`: it is a flat class lookup
    // rather than a relational match evaluated against every one of the nodes.
    elTree.querySelectorAll('.kids').forEach((kids) => shut(kids.parentElement, closed));
    const toggled = performance.now();

    // The stamp that matters. Toggling 7,500 classes is trivial; laying out and painting the
    // 187,500 rows it reveals is not, and all of that lands after `toggled`. Reporting the toggle
    // alone under-read BENCH by 160x and put "489 ms" on a stall that froze the renderer past 45s.
    const painted = await nextPaint();

    elStatus.textContent = `${closed ? 'Collapsed' : 'Expanded'}: ` +
        `${(toggled - started).toFixed(1)} ms toggling, ${(painted - started).toFixed(1)} ms to paint`;
    button.disabled = false;
};

el('expandAll').addEventListener('click', bulk(false));
el('collapseAll').addEventListener('click', bulk(true));

/* Outside the clock: the page fetches on load, and only then offers the button. */
fetch(`/data/${dataset}`)
    .then((response) => response.ok ? response.json() : Promise.reject(new Error(`${response.status} for ${dataset}`)))
    .then((company) => {
        el('runId').textContent = run || '–';
        el('datasetKey').textContent = company.dataset;
        el('modelNodes').textContent = count(census(company));
        elStatus.textContent = 'Ready.';
        elRender.disabled = false;
        elRender.addEventListener('click', () => render(company));
    })
    .catch((error) => {
        elStatus.textContent = `Failed: ${error.message}`;
    });
