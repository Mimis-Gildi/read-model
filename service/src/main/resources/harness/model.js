/*
 * The contract (`bench.css` styles it): queries `.node.collapsed`, `.kids` and `.row > .twist`.
 */

export const LEVELS = [
    {children: 'groups', label: (n) => n.division},
    {children: 'teams', label: (n) => n.group},

    // Teams ship folded!
    {children: 'people', label: (n) => n.team, collapsed: true},
    {
        children: null,
        label: (n) => `${n.firstName} ${n.lastName}`,
        meta: (n) => `${n.jobTitle.padEnd(26)}${n.location.padEnd(18)}${n.phone}`,
    },
];

export const OPEN = '▾';
export const SHUT = '▸';
export const LEAF = '·';

export const count = (n) => n.toLocaleString();
