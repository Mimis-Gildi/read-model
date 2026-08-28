/*
 * The read model's shape and the glyphs of a row -- shared, verbatim, by every module under test.
 *
 * This is not a module's own table. The walk it drives produces the row `bench.css` styles and the row the harness
 * queries through `.node.collapsed`, `.kids` and `.row > .twist`; README #3 makes cross-framework comparability
 * depend on every module emitting that one shape. Held here, a third module cannot quietly answer a different
 * question while looking like it answers the same one.
 *
 * What a module still owns is how it COUNTS what it made -- vanilla counts `document.createElement`, React counts
 * host tags and deliberately not the component wrapper -- so the counter stays in each fixture.
 */

/** Every level of the read model, in depth order. The tree walk is driven off this. */
export const LEVELS = [
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

export const OPEN = '▾';
export const SHUT = '▸';
export const LEAF = '·';

export const count = (n) => n.toLocaleString();
