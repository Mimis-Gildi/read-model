/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

/*
 * React as the page loads it: browser ESM under /vendor/react, bundled from the npm packages, so the version the
 * fixture is typechecked against is the version it is measured on.
 *
 * One build rather than one per file, because React's shared state has to be shared: react holds the dispatcher and
 * scheduler the task queue, and a second copy of either is a second registry that react-dom would silently disagree
 * with. Code splitting is what keeps them single -- whatever two entries both reach lands in a chunk beside them.
 * The production define is not decoration -- the development build carries every warning path, and that cost would
 * be reported as React's.
 */
import {build} from 'esbuild';
import {mkdir, writeFile} from 'node:fs/promises';

const OUT = 'build/vendor/react';
const ENTRIES = 'build/vendor/entries';

/**
 * React ships CommonJS, and `export *` from CommonJS is a runtime copy no static import can bind to -- the browser
 * refuses the module for want of `createRoot`. The names are read off the package here, at build time, and written
 * out as a destructuring export: a static named surface the fixture can import, beside the default it imports as React.
 */
const surfaceOf = async (module) => Object.keys(await import(module))
    .filter((name) => name !== 'default' && /^[A-Za-z_$][\w$]*$/.test(name))
    .join(', ');

const entry = async (module, name) => writeFile(`${ENTRIES}/${name}.mjs`,
    `import bundled from '${module}';\n`
    + `export default bundled;\n`
    + `export const {${await surfaceOf(module)}} = bundled;\n`)
    .then(() => `${ENTRIES}/${name}.mjs`);

await mkdir(ENTRIES, {recursive: true});

await Promise.all([
    entry('react', 'react'),
    entry('react-dom', 'react-dom'),
    entry('react-dom/client', 'client'),
]).then((entryPoints) => build({
    entryPoints,
    outdir: OUT,
    outExtension: {'.js': '.mjs'},
    bundle: true,
    splitting: true,
    format: 'esm',
    platform: 'browser',
    define: {'process.env.NODE_ENV': '"production"'},
}));
