(function () {
    'use strict';

    const BLANK = '—';

    const elTable = document.getElementById('matrix');
    const elHead = document.getElementById('head');
    const elRows = document.getElementById('rows');
    const elEmpty = document.getElementById('empty');
    const elConn = document.getElementById('conn');
    const elConnText = document.getElementById('conn-text');
    const elLaunched = document.getElementById('launched');
    const elDataset = document.getElementById('dataset');

    function setConn(state, text) {
        elConn.setAttribute('data-state', state);
        elConnText.textContent = text;
    }

    function shortId(id) {
        if (!id) return BLANK;
        return String(id).slice(0, 8);
    }

    // The reference 'Par' column is styled apart from the measured ones.
    function isPar(columnName) {
        return String(columnName).trim().toLowerCase() === 'par';
    }

    // Fills the dataset picker from the service. Same tolerance as render(): a ragged frame must
    // not empty the control the operator is holding, so a frame carrying nothing usable is ignored
    // rather than obeyed.
    function renderDatasets(vocabulary) {
        const datasets = (vocabulary && Array.isArray(vocabulary.datasets)) ? vocabulary.datasets : [];
        if (datasets.length === 0) return;

        const previous = elDataset.value;
        elDataset.textContent = '';

        datasets.forEach(function (dataset) {
            if (!dataset || dataset.key == null) return;
            const option = document.createElement('option');
            option.value = String(dataset.key);
            option.textContent = (typeof dataset.nodes === 'number')
                ? String(dataset.label) + ' — ' + dataset.nodes.toLocaleString()
                : String(dataset.label);
            elDataset.appendChild(option);
        });

        // the socket re-sends on every reconnect; a dropped connection must not move the selection
        if (previous) elDataset.value = previous;
    }

    // The last matrix the service sent, kept so the dataset picker can re-render without waiting
    // for the next frame. The service still owns every number in it; this is a held copy, not a model.
    let latest = null;

    // The rows for the dataset the operator is looking at. An empty selection -- before the
    // vocabulary lands, or on a ragged frame -- shows everything, the same tolerance as the rest
    // of the page: never hide data because a control has not been populated yet.
    function forSelectedDataset(rows) {
        const selected = elDataset.value;
        return selected ? rows.filter(function (row) { return row && row.dataset === selected; }) : rows;
    }

    // Renders whatever arrived. Never throws on a ragged or partial matrix: the socket is also
    // the debugging channel, and a view that dies on bad input hides the very frame you need.
    function render(matrix) {
        elHead.textContent = '';
        elRows.textContent = '';

        const columns = (matrix && Array.isArray(matrix.columns)) ? matrix.columns : [];
        const rows = forSelectedDataset((matrix && Array.isArray(matrix.rows)) ? matrix.rows : []);

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
            const tr = document.createElement('tr');

            const th = document.createElement('th');
            th.scope = 'row';
            th.textContent = (row && row.title != null) ? String(row.title) : BLANK;
            tr.appendChild(th);

            const cells = (row && Array.isArray(row.cells)) ? row.cells : [];
            columns.forEach(function (name, i) {
                const td = document.createElement('td');
                if (isPar(name)) td.className = 'par';
                const value = cells[i];
                td.textContent = (value === null || value === undefined || value === '')
                    ? BLANK
                    : String(value);
                tr.appendChild(td);
            });

            elRows.appendChild(tr);
        });
    }

    // launch ----------------------------------------------------------------

    document.getElementById('launch').addEventListener('click', function () {
        const moduleSel = document.getElementById('module');
        const opt = moduleSel.options[moduleSel.selectedIndex];
        const moduleId = moduleSel.value;
        const base = opt.getAttribute('data-url');
        const dataset = document.getElementById('dataset').value;

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
                return; // not a frame this page understands
            }
            if (!msg || typeof msg !== 'object') return;
            if (msg.type === 'vocabulary') {
                renderDatasets(msg);
                render(latest); // the selection may have just appeared; the rows must follow it
                return;
            }
            if (msg.type !== 'matrix') return; // room for heartbeat, crash, and whatever comes next
            latest = msg;                      // whole matrix every time: replace, never merge
            render(latest);
        });

        socket.addEventListener('close', function () {
            setConn('stale', 'offline');
            scheduleRetry();
        });

        socket.addEventListener('error', function () {
            try {
                socket.close();
            } catch (err) { /* the close handler schedules the retry */
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

    // Switching dataset is a view change, not a request: everything needed is already here.
    elDataset.addEventListener('change', function () { render(latest); });

    render(null);
    connect();
})();
