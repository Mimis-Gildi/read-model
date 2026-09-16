/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

/*
 * React as the page loads it: browser ESM under /vendor/react, bundled from the npm packages, so the version the
 * fixture is typechecked against is the version it is measured on.
 *
 * Four files rather than one, because React's shared state has to be shared: react.mjs holds the dispatcher and
 * scheduler.mjs the task queue, and a second copy of either is a second registry that react-dom would silently
 * disagree with. The production define is not decoration -- the development build carries every warning path,
 * and that cost would be reported as React's.
 */
import {build} from 'esbuild';

const OUT = 'build/vendor/react';

/** Keeps siblings as siblings: `react` imported from client.mjs resolves to the file lying next to it, not to a copy. */
const siblings = (modules) => ({
    name: 'siblings',
    setup: (esbuild) => Object.entries(modules).forEach(([module, file]) =>
        esbuild.onResolve({filter: new RegExp(`^${module}$`)}, () => ({path: file, external: true}))),
});

/** Re-exported both ways: the named surface the fixture destructures, and the default the fixture imports as React. */
const bundle = (module, file, external) => build({
    stdin: {
        contents: `export * from '${module}';\nexport {default} from '${module}';`,
        resolveDir: '.',
        loader: 'js',
    },
    outfile: `${OUT}/${file}`,
    bundle: true,
    format: 'esm',
    platform: 'browser',
    define: {'process.env.NODE_ENV': '"production"'},
    plugins: [siblings(external)],
});

await Promise.all([
    bundle('scheduler', 'scheduler.mjs', {}),
    bundle('react', 'react.mjs', {}),
    bundle('react-dom', 'react-dom.mjs', {'react': './react.mjs', 'scheduler': './scheduler.mjs'}),
    bundle('react-dom/client', 'client.mjs',
        {'react': './react.mjs', 'react-dom': './react-dom.mjs', 'scheduler': './scheduler.mjs'}),
]);
