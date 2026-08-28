/*
 * The measurement harness. Shared, verbatim, by every module under test. So, this is yappy!
 *
 * This file exists so that the clocks, the guards and the ladder are not written three times DIFFERENTLY.
 * If each fixture carried its own variants, the maintenance and consistency tax would defeat the purpose of the bench.
 * I attempt "one of everything measured" here, so a number in the React column and a number in the Pure JS column differ
 * ONLY where the frameworks do.
 *
 * Explanation to the clocked runs -- it's a ladder of expansions:
 *   1. One (1) measured render per level of the model.
 *   2. Starting from a clean container against the already LOADED data tree (the service culled to that level).
 *   3. The four (4) levels and four rows load at the browser's edge capability (why control plane has 4 rows).
 *
 * Measurement boundary set as follows:
 *   - fetch, JSON parse, and everything else considered "infrastructural" are OUTSIDE the clock;
 *   - the clock starts with the model and all the data already on the page ready to render;
 *   - it stops when everything is rendered measuring the container SLA boundary (Bloated Owl, see article).
 * (https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html)
 *
 * "Rendered" has two meanings for this test (intrinsic to the DOM pattern), and both are reported in each test:
 *   - built -- the whole tree constructed and attached to the live document and measured synchronously on the line
 *              after the `attach()` completes. There's no layout, paint, or drift costs here - just framework.
 *   - painted -- the frame after that with the layout and the paint that the `attach()` provoked included.
 *
 * The gap between them is the browser's cost of the same naked DOM one incurs by native JSON view.
 *
 * A measured render builds every node of its level counting `built` and `elements` for all.
 * IMPORTANT: The teams ship FOLDED -- the people beneath them are constructed and NOT laid out.
 * (You can twist-open any team by yourself here.)
 * Laying them out is a separate and deliberate act -- the other cost of a framework (i.e., component design).
 *
 * Each rung is posted to the service as it completes, so a run that dies leaves a record of how far it got.
 * And the "Expand" is CHUNKED by 20,000 each. The reason is Chrome's ceilings: 100k expands and 200k dies.
 *
 * There are expectations we challenge here. Google says 100k nodes is the performance ceiling and 200k is the edge.
 * React people claim 10k and 20k respectively. So we will go FAR beyond that with this lean approach.
 *
 * Thus the reveal is the FIFTH rung and the only one that reports more than once as it unfolds in 20k chunks and posts
 * the accumulation (sum) after every measurement, so the cell on the board always holds the last chunk that survived
 * expansion added in. When it dies it will stop adding. See [reveal].
 *
 * ---
 *
 * What the module supplies; see [start]:
 *
 *   attach(company, host) -> elements: builds the culled tree and puts it on the page, and it's inside the clock
 *                              measuring the DOM construction (build). It returns the number of elements it created.
 *   reset(host) -> tears the previous rung down outside any clock: same DOM. React will uniquely crash here also!
 *   shut(box, closed) -> folds or unfolds a single node.
 *   batch(work) OPTIONAL -> runs a chunk's worth of [shut] calls and guarantees they have completed by the time returned.
 *                              This defaults to calling `work()` for any synchronous folding.
 *
 * All concerns: when to render, what to time, what to report, what to do about a hidden tab -- are first contracted HERE.
 *
 * The harness drives DOM in [reveal] and [collapseAll] through `.node.collapsed`, `.kids` and `.row > .twist`.
 * `bench.css` is a companion to this lean measurement mechanism. Its reasons are documented in the file.
 */
const params = new URLSearchParams(location.search);
const dataset = params.get('dataset');
const run = params.get('run');

const module = params.get('module');

/**
 * Rows revealed per chunk, letting the browser recover between chunks. Both overridable on the URL for experimenting.
 *
 * 20,000 is chosen to sit just below the "React won't crash" boundary. The boundary is 100,000 for Browser JS and Node.
 */
const REVEAL_STEP = Number(params.get('step')) || 20_000;   // Experimentation derived
const REVEAL_PAUSE = Number(params.get('pause')) || 16;     // Experimentation derived

const el = (id) => document.getElementById(id);
const elTree = el('tree');
const elRun = el('run');
const elStatus = el('status');
const elRows = el('rows');

const count = (n) => n.toLocaleString();
const ms = (n) => `${n.toFixed(1)} ms`;

/** The meat: module under test, handed over by [start]. */
let fixture = null;

/**
 * Run a chunk of folding and do not return until it has landed.
 *
 * A module that folds by touching the DOM is finished the moment its loop returns: i.e, `work()`.
 * IMPORTANT: a module that folds by asking a framework to re-render is NOT!
 * EXAMPLE: react.dev is explicit that a render is only "scheduled", and the harness stamps a chunk with a double
 * `requestAnimationFrame` on the assumption that the work is done by then. That assumption is the module's to "accept."
 * The module gets the hook and decides -- React supplies `flushSync`; my Pure JS needs nothing.
 *
 * One flush per chunk, never per node: per node would be too many separate synchronous renders -- a problem:
 * - such numbers no real React application should ever produce (except at a laggard who asked me this question)
 * - and the slowdown is cascaded and exponential, formally "DOM Size Performance Cliff."
 *
 * That drop-off is caused by "Layout Thrashing," and it is "Forced Synchronous Layout Wall" in browser docs. As I will
 * show on Demoscene, an article and maybe a video for this -- the synthetic "Main Thread Starvation" makes any benching
 * totally useless because the browser process container is already in the compromised state: not a framework artifact!
 *
 * This is WHY I chose to survive the cliff by chunking and recovering in the first place.
 */
const batch = (work) => fixture.batch ? fixture.batch(work) : work();

/** The pause between reveal chunks. Deliberately outside every clock to allow the browser to recover off of the Cliff. */
const breathe = () => new Promise((resolve) => setTimeout(resolve, REVEAL_PAUSE));

/** Resolves on the frame after the one the caller's DOM work completes (once it is painted).*/
const nextPaint = () => new Promise((resolve) =>
    requestAnimationFrame(() => requestAnimationFrame(() => resolve(performance.now()))));

/**
 * IMPORTANT: Saving Grace - a way to survive the runaway "Main Thread Starvation" that'd kill the experiment.
 *
 * Resolves once the tab is actually on screen. An opportunity to inject some recovery code.
 *
 * Chrome does not run `requestAnimationFrame` in a hidden tab, so [nextPaint] never settles and a ladder started
 * in the background hangs forever -- this is observed through purpose-built fixtures I'd experimented with prior.
 * Waiting is also the honest behavior rather than a nicety because a background tab is throttled, so any paint number
 * measured in one would be pointless garbage. With that in mind, the machine's performance is also not uniform.
 *
 * Measurements are RELATIVE to one another.
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

/** The tree culled to [level] outside the clock because transport is not part of this experiment. */
const fetchLevel = (level) => fetch(`/data/${dataset}/${level}`)
    .then((response) => response.ok
        ? response.json()
        : Promise.reject(new Error(`${response.status} for ${dataset}/${level}`)));

/**
 * One rung:
 * 1. Build the culled tree.
 * 2. Attach it in a single operation.
 * 3. And, timestamp twice.
 *
 * Teardown of the previous rung happens BEFORE `started` -- time NOT from here.
 * This level is NEVER charged for the DOM the level before it has left behind.
 */
const measure = async (company, level) => {
    await onScreen();
    darkened = false;

    const cleared = performance.now();
    fixture.reset(elTree);
    teardown += performance.now() - cleared;

    const started = performance.now();
    const elements = fixture.attach(company, elTree);
    const built = performance.now();

    const painted = await nextPaint();

    // IMPORTANT: a tab hidden mid-measurement stops painting! Any benchmark attempts are a moot point then.
    // I once had 60 elements reporting 106,843.9 ms this way. I found no practical way to salvage that.
    // AND: This is the best recovery point I discovered: just fold and unfold over live caches again.
    if (!darkened) return {elements, built: built - started, painted: painted - started};
    elStatus.textContent = `Level ${level}: BOOM -- tab went dark mid-render -- discarded, re-running.`;
    return measure(company, level);
};

/** Count model nodes on the served tree instead of the service-derived value trusted. */
const census = (company) => company.divisions.reduce(
    (total, division) => total + 1 + division.groups.reduce(
        (g, group) => g + 1 + group.teams.reduce((t, team) => t + 1 + team.people.length, 0), 0), 0);

/**
 * The "rung" in the result row. These five "level" names materialize in the fixture's HTML exactly once, and in the
 * first cell of each row, and they are the same strings the service files a report under. Reading them back for fidelity.
 */
const rungOf = (row) => row.cells[0].textContent;

const fill = (row, nodes, result) =>
    [count(nodes), count(result.elements), ms(result.built), ms(result.painted)]
        .forEach((value, column) => row.cells[column + 1].textContent = value);

/**
 * The ladder's rungs are read off the Ktor (service) result matrix in the depth order.
 *
 * The contract is in `model.js`reused everywhere.
 */
const ladderRows = () => Array.from(elRows.querySelectorAll('tr[id^="level-"]'));

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
 * Post a finished rung at the time no measurement is going on.
 */
const post = (row, result) => socket.readyState === WebSocket.OPEN && socket.send(JSON.stringify({
    type: 'report', run: run, module: module, dataset: dataset,
    rung: rungOf(row), elements: result.elements, built: result.built, painted: result.painted,
}));

const buttons = () => [el('start'), el('expandAll'), el('collapseAll')];

/** Model nodes of whatever the ladder last put on the page for reveal to lays these out. */
let nodesOnScreen = 0;

/**
 * Teardown the ladder: the time [measure] spent in `fixture.reset`; i.e., React core weakness.
 */
let teardown = 0;

/**
 * The ladder: level 0 through 3, each fetched then measured, reported as it completes.
 */
const ladder = async () => {
    buttons().forEach((button) => button.disabled = true);
    elRows.querySelectorAll('td:not(:first-child)').forEach((cell) => cell.textContent = '–');
    teardown = 0;

    if (document.hidden) elStatus.textContent =
        'Waiting: bring this tab to the front -- paint cannot be measured in a background tab.';

    for (const [level, row] of ladderRows().entries()) {
        elStatus.textContent = `Level ${level}: fetching…`;
        const company = await fetchLevel(level);
        elStatus.textContent = `Level ${level}: rendering…`;
        const result = await measure(company, level);
        nodesOnScreen = census(company);
        fill(row, nodesOnScreen, result);
        post(row, result);
    }

    elStatus.textContent = `Ladder complete. ${ms(teardown)} of teardown, billed to no rung.`;
    buttons().forEach((button) => button.disabled = false);
};

/**
 * Collapse everything below the divisions.
 */
const collapseAll = async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    const started = performance.now();
    // Selecting `.kids` and stepping up beats `.node:has(> .kids)`: it is a flat class lookup
    // rather than a relational match evaluated against every one of the nodes.
    batch(() => elTree.querySelectorAll('.kids').forEach((kids) => fixture.shut(kids.parentElement, true)));
    const toggled = performance.now();
    const painted = await nextPaint();

    elStatus.textContent = `Collapsed: ${ms(toggled - started)} toggling, ${ms(painted - started)} to paint`;
    button.disabled = false;
};

/**
 * The reveal: unfold the whole tree in survivable chunks, measuring till finish or the browser crash.
 */
const reveal = async () => {
    buttons().forEach((each) => each.disabled = true);

    // Document order; the child counts are here.
    const folded = Array.from(elTree.querySelectorAll('.node.collapsed'))
        .map((box) => ({box: box, rows: box.querySelector(':scope > .kids').children.length}));

    const row = el('reveal');
    let at = 0;
    let revealed = 0;
    let built = 0;
    let painted = 0;

    while (at < folded.length) {
        await onScreen();
        darkened = false;

        const started = performance.now();
        batch(() => {
            for (let rows = 0; at < folded.length && rows < REVEAL_STEP; at += 1) {
                rows += folded[at].rows;
                revealed += folded[at].rows;
                fixture.shut(folded[at].box, false);
            }
        });
        const toggled = performance.now();
        const stamp = await nextPaint();

        // Most important point here.
        if (darkened) {
            elStatus.textContent =
                `Reveal: BOOM -- tab went dark after ${count(revealed)} rows -- accumulation abandoned.`;
            buttons().forEach((each) => each.disabled = false);
            return;
        }

        built += toggled - started;
        painted += stamp - started;

        const result = {elements: revealed, built: built, painted: painted};
        fill(row, nodesOnScreen, result);
        post(row, result);
        elStatus.textContent = `Reveal: ${count(revealed)} rows, ${ms(painted)} to paint` +
            (at < folded.length ? ` -- ${count(folded.length - at)} nodes still folded…` : '');

        await breathe();
    }

    elStatus.textContent = `Reveal complete: ${count(revealed)} rows, ${ms(painted)} to paint.`;
    buttons().forEach((each) => each.disabled = false);
};

/**
 * Hand the harness a module to run. The only entry point.
 *
 * The fixture owns nothing but its three functions;
 * - the buttons,
 * - the status line,
 * - and the socket.
 *
 * Every module is driven by identical code.
 */
export const start = (module) => {
    fixture = module;

    el('start').addEventListener('click', ladder);
    el('expandAll').addEventListener('click', reveal);
    el('collapseAll').addEventListener('click', collapseAll);

    elRun.textContent = run || '–';
    el('datasetKey').textContent = dataset || '–';
    elStatus.textContent = dataset ? 'Ready.' : 'No dataset on the URL.';
    el('start').disabled = !dataset;
};

/** The container everything renders into. */
export const host = elTree;
