# Control-plane deslopification

Naming pass over `control/index.html`, `control.css`, and `Page.kt`. Propose the proper name for each row; blanks mean not yet decided.

## Page shell

| Current | Proposed                      | Why                                                                      |
|---------|-------------------------------|--------------------------------------------------------------------------|
| `.wrap` | `.controlPageRootContentArea` | "wrap" is meaningless filler; this is the page's centered content column |

## Header / connection status

| Current | Proposed | Why |
|---|---|---|
| `#conn` | `#connectionStatus` | "conn" is an unexplained abbreviation |
| `#conn-text` | `#connectionState` | holds "connecting"/"live"/"stale" — a state label, not just "text" |
| `.dot` | `.statusDot` | ties it explicitly to the connection status it decorates |
| `.tagline` | *(unchanged)* | already accurate |

## Launch section

| Current (id/class) | `Page.kt` property | Proposed id/class | Proposed property | Why |
|---|---|---|---|---|
| `#uiFramework` | `module` | *(unchanged)* | `uiFramework` | property name doesn't match its own element's id — leftover from the old "Module" naming |
| `#datasetKey` | `dataset` | *(unchanged)* | *(unchanged)* | already accurate |
| `#launch` (button) | `launch` | *(unchanged)* | `launchButton` | disambiguates from `launched`/`launchRun()` — this one specifically is the button |
| `#launched` (p) | `launched` | *(unchanged)* | *(unchanged)* | already accurate |
| `.controls`, `.field` | — | *(unchanged)* | — | already accurate |

## Results section

| Current (id) | `Page.kt` property | Proposed id | Proposed property | Why |
|---|---|---|---|---|
| `#matrix` (table) | `table` | *(unchanged)* | `matrix` | property says "table" (generic) when the id already says what table it is |
| `#head` (`<tr>` in `<thead>`) | `head` | `#columns` | `columns` | "head" inside a `<thead>` just restates the parent tag; this row is the column titles |
| `#foot` (`<tfoot>`) | `foot` | `#totals` | `totals` | same problem — this is the totals row |
| `#rows` (`<tbody>`) | `rows` | *(unchanged, source from `Contract.DOM_KEY_ROWS`)* | *(unchanged)* | name is fine; only the hardcoded-string duplication needs fixing |
| `.table-scroll`, `.empty` | — | *(unchanged)* | — | already accurate |

## Already fine, not touching

`.panel`, `header`, `footer`, `h1 .mono`.
