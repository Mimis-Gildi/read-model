(function () {
    'use strict';

    const BLANK = '-';

    const elTable = document.getElementById('matrix');
    const elHead = document.getElementById('head');
    const elRows = document.getElementById('rows');
    const elFoot = document.getElementById('foot');
    const elEmpty = document.getElementById('empty');
    const elConn = document.getElementById('conn');
    const elConnText = document.getElementById('conn-text');
    const elLaunched = document.getElementById('launched');
    const elModule = document.getElementById('module');
    const elDataset = document.getElementById('dataset');

    function setConn(state, text) {
        elConn.setAttribute('data-state', state);
        elConnText.textContent = text;
    }

    function shortId(id) {
        if (!id) return BLANK;
        return String(id).slice(0, 8);
    }

    // The reference 'Par' column is styled differently.
    function isPar(columnName) {
        return String(columnName).trim().toLowerCase() === 'par';
    }

    // Fills one picker from the service ignoring empty options.
    function fillOptions(select, options, textOf) {
        const usable = Array.isArray(options) ? options.filter(function (opt) {
            return opt && opt.key != null;
        }) : [];
        if (usable.length === 0) return;

        const previous = select.value;
        select.textContent = '';

        usable.forEach(function (optionCandidate) {
            const htmlOption = document.createElement('option');
            htmlOption.value = String(optionCandidate.key);
            htmlOption.textContent = textOf(optionCandidate);
            select.appendChild(htmlOption);
        });

        // the socket re-sends on every reconnection, and dropped connection must not move the selection
        if (previous) select.value = previous;
    }

    function renderVocabulary(vocabulary) {
        if (!vocabulary) return;
        fillOptions(elModule, vocabulary.modules, function (m) {
            return String(m.label);
        });
        fillOptions(elDataset, vocabulary.datasets, function (d) {
            return (typeof d.nodes === 'number')
                ? String(d.label) + ' - ' + d.nodes.toLocaleString()
                : String(d.label);
        });
    }

    // The last matrix is always kept so the dataset picker can re-render.
    let latest = null;

    // The rows for the dataset the user is looking at.
    function forSelectedDataset(rows) {

        const selected = elDataset.value;
        return selected ? rows.filter(function (row) {
            return row && row.dataset === selected;
        }) : rows;
    }

    // Milliseconds, grouped so the thousand mark reads as the "second" boundary.
    function toTimeMsText(n) {
        return Number(n).toLocaleString('en-US', {minimumFractionDigits: 1, maximumFractionDigits: 1});
    }

    function valueArrayOf(cell) {
        return (cell && Array.isArray(cell.values)) ? cell.values : [];
    }

    // A body cell shows its readings as they were measured: build and paint.
    function cellToText(cell) {
        const values = valueArrayOf(cell);
        return values.length === 0 ? BLANK : values.map(toTimeMsText).join(' / ');
    }

    // A foot cell is all the fragments added together to show total time.
    function totalTimeMsText(cell) {
        const values = valueArrayOf(cell);
        return values.length === 0 ? BLANK : toTimeMsText(values.reduce(function (sum, n) {
            return sum + Number(n);
        }, 0));
    }

    // Shared by the body and the totals foot.
    function rowElement(row, columns, text) {
        const tr = document.createElement('tr');

        const th = document.createElement('th');
        th.scope = 'row';
        th.textContent = (row && row.title != null) ? String(row.title) : BLANK;
        tr.appendChild(th);

        const cells = (row && Array.isArray(row.cells)) ? row.cells : [];
        columns.forEach(function (name, i) {
            const td = document.createElement('td');
            if (isPar(name)) td.className = 'par';
            td.textContent = text(cells[i]);
            tr.appendChild(td);
        });

        return tr;
    }

    // Renders whatever arrived as it arrived.
    function render(matrix) {
        elHead.textContent = '';
        elRows.textContent = '';
        elFoot.textContent = '';

        const columns = (matrix && Array.isArray(matrix.columns)) ? matrix.columns : [];
        const rows = forSelectedDataset((matrix && Array.isArray(matrix.rows)) ? matrix.rows : []);
        const totals = forSelectedDataset((matrix && Array.isArray(matrix.totals)) ? matrix.totals : []);

        if (columns.length === 0 || rows.length === 0) {
            elTable.hidden = true;
            elEmpty.style.display = '';
            return;
        }
        elTable.hidden = false;
        elEmpty.style.display = 'none';

        const corner = document.createElement('th');
        corner.className = 'corner';
        corner.scope = 'col';
        elHead.appendChild(corner);

        columns.forEach(function (name) {
            const th = document.createElement('th');
            th.scope = 'col';
            if (isPar(name)) th.className = 'par';
            th.textContent = (name === null || name === undefined) ? BLANK : String(name);
            elHead.appendChild(th);
        });

        rows.forEach(function (row) {
            elRows.appendChild(rowElement(row, columns, cellToText));
        });

        totals.forEach(function (row) {
            elFoot.appendChild(rowElement(row, columns, totalTimeMsText));
        });
    }

    // launch ----------------------------------------------------------------

    document.getElementById('launch').addEventListener('click', function () {
        const moduleId = elModule.value;
        const dataset = elDataset.value;
        if (!moduleId || !dataset) return;

        // The mount is the module key, and the same string Routing.kt mounts each fixture at.
        const base = '/' + moduleId + '/';

        const runId = (window.crypto && window.crypto.randomUUID)
            ? window.crypto.randomUUID()
            : String(Date.now()) + '-' + Math.random().toString(16).slice(2);

        const url = base
            + '?run=' + encodeURIComponent(runId)
            + '&module=' + encodeURIComponent(moduleId)
            + '&dataset=' + encodeURIComponent(dataset);

        window.open(url, '_blank');

        elLaunched.textContent = 'launched ' + moduleId + ' / ' + dataset + ' as run ';
        const b = document.createElement('b');
        b.textContent = shortId(runId);
        elLaunched.appendChild(b);
    });

    // socket ----------------------------------------------------------------

    let socket = null;
    let attempt = 0;
    let retryTimer = null;

    function connect() {
        const proto = (location.protocol === 'https:') ? 'wss:' : 'ws:';
        setConn('wait', attempt === 0 ? 'connecting' : 'reconnecting');

        try {
            socket = new WebSocket(proto + '//' + location.host + '/ws');
        } catch (err) {
            setConn('stale', 'offline');
            scheduleRetry();
            return;
        }

        socket.addEventListener('open', function () {
            attempt = 0;
            setConn('live', 'live');
        });

        socket.addEventListener('message', function (ev) {
            let msg;
            try {
                msg = JSON.parse(ev.data);
            } catch (err) {
                return;
            }
            if (!msg || typeof msg !== 'object') return;
            if (msg.type === 'vocabulary') {
                renderVocabulary(msg);
                render(latest);
                return;
            }
            if (msg.type !== 'matrix') return; // awaiting status footer.
            latest = msg;
            render(latest);
        });

        socket.addEventListener('close', function () {
            setConn('stale', 'offline');
            scheduleRetry();
        });

        socket.addEventListener('error', function () {
            try {
                socket.close();
            } catch (err) {
            }
        });
    }

    function scheduleRetry() {
        if (retryTimer) return;
        const delay = Math.min(1000 * Math.pow(2, attempt), 15000);
        attempt += 1;
        retryTimer = setTimeout(function () {
            retryTimer = null;
            connect();
        }, delay);
    }

    // Switching dataset is a view change.
    elDataset.addEventListener('change', function () {
        render(latest);
    });

    render(null);
    connect();
})();
