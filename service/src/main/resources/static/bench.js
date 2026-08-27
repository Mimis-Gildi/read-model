/*
 * Vanilla JS fixture -- the baseline.
 *
 * A run is a ladder of expansions: one measured render per level of the model.
 * Starting from a clean container against the loaded data-tree, the service culled to that level.
 * The four levels and four rows load at the browser's edge capability, which is why the control-plane
 * matrix has four rows.
 *
 * Measurement boundary, set by yours truly, @rdd13r, is as follows:
 *   - fetch, JSON parse, and everything else infrastructural are OUTSIDE the clock;
 *   - the clock starts with the model already on the page ready to render;
 *   - it stops when everything is rendered measuring the container SLA boundary.
 *
 * "Rendered" has two readings for this test, and both are reported in each test:
 *   built -- the whole tree constructed and attached to the live document and measured synchronously
 *      on the line after the `attach()` returns. No layout, no paint, and no drift.
 *   painted -- the frame after that: the layout and the paint that the `attach()` provoked included.
 * The gap between them is the browser's cost of the same naked DOM one incurs by native JSON view.
 *
 * A measured render builds every node of its level -- `built` and `elements` count all of them --
 * but teams ship FOLDED, so the people beneath them are constructed and not laid out. Laying them
 * out is a separate, deliberate act; see [LEVELS] for why the two costs are being kept apart.
 *
 * Each rung is posted to the service as it completes, so a run that dies leaves a record of how far it got.
 */

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

const params = new URLSearchParams(location.search);
const dataset = params.get('dataset');
const run = params.get('run');
const module = params.get('module') || 'vanilla';

const el = (id) => document.getElementById(id);
const elTree = el('tree');
const elRun = el('run');
const elStatus = el('status');
const elRows = el('rows');

const count = (n) => n.toLocaleString();
const ms = (n) => `${n.toFixed(1)} ms`;

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
 * README #3 makes cross-framework comparability depend on every module emitting the same DOM per row.
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

/** Resolves on the frame after the one the caller's DOM work lands in (once it is painted).*/
const nextPaint = () => new Promise((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve(performance.now()))));

/**
 * Resolves once the tab is actually on screen.
 *
 * Chrome does not run `requestAnimationFrame` in a hidden tab, so [nextPaint] never settles and a ladder started
 * in the background hangs forever -- this is observed through purpose-built fixtures.
 * Waiting is also the honest behavior rather than a nicety: a background tab is throttled, so any paint number
 * measured in one would be pointless garbage.
 */

/** Set the moment the tab goes dark, so a measurement in flight knows it is spoiled. */
let darkened = false;
document.addEventListener('visibilitychange', () => darkened = darkened || document.hidden);

const onScreen = () => document.hidden
    ? new Promise((resolve) => document.addEventListener('visibilitychange', function seen() {
        if (document.hidden) return;
        document.removeEventListener('visibilitychange', seen);
        resolve();
    }))
    : Promise.resolve();

/** The tree culled to [level]. Outside every clock: transport is not part of what is measured. */
const fetchLevel = (level) => fetch(`/data/${dataset}/${level}`)
    .then((response) => response.ok
        ? response.json()
        : Promise.reject(new Error(`${response.status} for ${dataset}/${level}`)));

/**
 * One rung: build the culled tree, attach it in a single operation, and stamp twice.
 *
 * Teardown of the previous rung happens before `started`,
 * so a level is never charged for the DOM the level before it left behind.
 */
const measure = async (company, level) => {
    await onScreen();
    darkened = false;
    elTree.replaceChildren();
    elements = 0;

    // Off-document assembly, then a single `attach()`, the most practical way to add a large subtree.
    // It keeps `built` as a measure of construction rather than of repeated reflow.
    const fragment = document.createDocumentFragment();
    const started = performance.now();
    company.divisions.forEach((division) => fragment.appendChild(build(division, 0)));
    elTree.appendChild(fragment);
    const built = performance.now();

    const painted = await nextPaint();

    // A tab hidden mid-measurement stops painting! The whole dark period lands inside this one number:
    // just 60 elements once reported 106,843.9 ms that way. There is no practical way to salvage such a run,
    // so it is discarded, and the rung is climbed again once the tab is back on the active screen.
    if (!darkened) return {level, elements, built: built - started, painted: painted - started};
    elStatus.textContent = `Level ${level}: BOOM -- tab went dark mid-render -- discarded, re-running.`;
    return measure(company, level);
};

/** Model nodes on the served tree, counted rather than derived -- the service already derives it. */
const census = (company) => company.divisions.reduce(
    (total, division) => total + 1 + division.groups.reduce(
        (g, group) => g + 1 + group.teams.reduce((t, team) => t + 1 + team.people.length, 0), 0), 0);

const report = (result, nodes) => {
    const row = el(`level-${result.level}`);
    [count(nodes), count(result.elements), ms(result.built), ms(result.painted)]
        .forEach((value, column) => row.cells[column + 1].textContent = value);
};

/**
 * The socket to the control plane. Opened once on `load` so that no rung ever pays for a handshake.
 *
 * The fixture sends measurements and ignores everything coming the other way because the matrix is for
 * the control plane to render, not for the thing that's being measured.
 *
 * Reporting per rung rather than per run is the whole point: a fixture that dies on LOAD, the dataset designed to crash
 * a wanting framework like React, never sends a summary because of the expected crash, so the last rung the server heard
 * about is the failure point. That only works if the report leaves before the next, larger rung is attempted.
 */
const socket = new WebSocket(`ws://${location.host}/ws`);

/**
 * Post a finished rung.
 *
 * Called after the paint stamp and not between the stamps: `send` is inexpensive but not free, and a measurement is not
 * allowed to contain the cost of reporting itself. Dropped silently if the socket is not open -- a run must survive the
 * recorder being absent, and the numbers are on the fixture's screen either way.
 */
const post = (result) => socket.readyState === WebSocket.OPEN && socket.send(JSON.stringify({
    type: 'report', run: run, module: module, dataset: dataset,
    level: result.level, elements: result.elements, built: result.built, painted: result.painted,
}));

const buttons = () => [el('start'), el('expandAll'), el('collapseAll')];

/**
 * The ladder: level 0 through 3, each fetched then measured, reported as it completes.
 *
 * Sequential on purpose. Four concurrent fetches would overlap a 25 MB parse with a measured render and charge the
 * render metrics for it noticeably.
 */
const ladder = async () => {
    buttons().forEach((button) => button.disabled = true);
    elRows.querySelectorAll('td:not(:first-child)').forEach((cell) => cell.textContent = '–');

    if (document.hidden) elStatus.textContent =
        'Waiting: bring this tab to the front -- paint cannot be measured in a background tab.';

    for (const level of LEVELS.keys()) {
        elStatus.textContent = `Level ${level}: fetching…`;
        const company = await fetchLevel(level);
        elStatus.textContent = `Level ${level}: rendering…`;
        const result = await measure(company, level);
        report(result, census(company));
        post(result);
    }

    elStatus.textContent = 'Ladder complete.';
    buttons().forEach((button) => button.disabled = false);
};

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
elTree.addEventListener('click', (event) => {
    const box = event.target.closest('.node');
    if (!box || !box.querySelector(':scope > .kids')) return;
    shut(box, !box.classList.contains('collapsed'));
});

/**
 * Expand everything or collapse everything below the divisions.
 *
 * Collapsing every node still leaves the twelve division rows on the screen: the lowest layer of 'Divisions' because
 * there is nothing above them to hide/collapse them. So, "collapse all" and "all but level one" are the same gesture.
 *
 * Reported with both stamps. Toggling classes is trivial, but laying out and painting the rows it reveals is not, and
 * all the cost lands after the toggle. The toggle alone under-read BENCH by 160x and put "489 ms" on a stall that froze
 * the renderer past 45 seconds. Thus goes the cost of the paint.
 */
const bulk = (closed) => async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    const started = performance.now();
    // Selecting `.kids` and stepping up beats `.node:has(> .kids)`: it is a flat class lookup
    // rather than a relational match evaluated against every one of the nodes.
    elTree.querySelectorAll('.kids').forEach((kids) => shut(kids.parentElement, closed));
    const toggled = performance.now();
    const painted = await nextPaint();

    elStatus.textContent = `${closed ? 'Collapsed' : 'Expanded'}: ` +
        `${ms(toggled - started)} toggling, ${ms(painted - started)} to paint`;
    button.disabled = false;
};

el('start').addEventListener('click', ladder);
el('expandAll').addEventListener('click', bulk(false));
el('collapseAll').addEventListener('click', bulk(true));

elRun.textContent = run || '–';
el('datasetKey').textContent = dataset || '–';
elStatus.textContent = dataset ? 'Ready.' : 'No dataset on the URL.';
el('start').disabled = !dataset;
