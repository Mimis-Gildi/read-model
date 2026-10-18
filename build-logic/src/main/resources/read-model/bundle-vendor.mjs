// noinspection SpellCheckingInspection

/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

/*
 * A fixture's framework as the page loads it: browser ESM under /vendor/<fixture>, bundled from the npm packages, so
 * the version the fixture is typechecked against is the version it is measured on.
 *
 * The fixture's package.json is the system of record: its "vendor" key maps each output name to a module specifier.
 * No key, nothing to vendor. The build stages this script under the fixture's build directory, because every import
 * here resolves from the script's location and only there does it reach the fixture's node_modules.
 *
 * One build rather than one per file, because a framework's shared state has to be shared: React's react holds the
 * dispatcher and scheduler the task queue, and a second copy of either is a second registry that react-dom would
 * silently disagree with. Code splitting is what keeps them single -- whatever two entries both reach lands in a chunk
 * beside them. The production define is not decoration -- the development build carries every warning path, and that
 * cost would be reported as the framework's.
 */
import {mkdir, readFile, writeFile} from 'node:fs/promises';
import process from 'node:process';

const [OUT] = process.argv.slice(2);
const ENTRIES = `${import.meta.dirname}/entries`;

const {vendor} = JSON.parse(await readFile('package.json', 'utf8'));
if (!vendor) process.exit(0);

const identifiers = (namespace) => Object.keys(namespace)
    .filter((name) => name !== 'default' && /^[A-Za-z_$][\w$]*$/.test(name));

/**
 * CommonJS, React among them, and `export *` from CommonJS be a runtime copy no static import can bind to
 * -- the browser refuses the module for want of `createRoot`. Node mirrors a CommonJS module's names off its default,
 * which is how one is told apart here; its names are read at build time and written out as a destructuring export:
 * a static named surface the fixture can import, beside the default it imports as the framework.
 * ESM re-exports as it is.
 */
const entry = async (name, module) => {
    const namespace = await import(module);
    const names = identifiers(namespace);
    const commonJs = namespace.default instanceof Object && names.every((it) => namespace[it] === namespace.default[it]);
    const content = commonJs
        ? `import bundled from '${module}';\nexport default bundled;\nexport const {${names.join(', ')}} = bundled;\n`
        : `export * from '${module}';\n` + ('default' in namespace ? `export {default} from '${module}';\n` : '');
    return writeFile(`${ENTRIES}/${name}.mjs`, content).then(() => `${ENTRIES}/${name}.mjs`);
};

/** No lockfile by policy, so what was measured is only known after install; it is recorded beside the bundle. */
const packageOf = (module) => module.split('/').slice(0, module.startsWith('@') ? 2 : 1).join('/');
const versionOf = async (module) => JSON.parse(await readFile(`node_modules/${packageOf(module)}/package.json`, 'utf8')).version;

await mkdir(ENTRIES, {recursive: true});
await mkdir(OUT, {recursive: true});

const {build} = await import('esbuild');
const modules = Object.entries(vendor);

await Promise.all(modules.map(([name, module]) => entry(name, module))).then((entryPoints) => build({
    entryPoints,
    outdir: OUT,
    outExtension: {'.js': '.mjs'},
    bundle: true,
    splitting: true,
    format: 'esm',
    platform: 'browser',
    define: {'process.env.NODE_ENV': '"production"'},
}));

await Promise.all(modules.map(async ([, module]) => [module, await versionOf(module)]))
    .then((versions) => writeFile(`${OUT}/versions.json`, JSON.stringify(Object.fromEntries(versions), null, 2) + '\n'));
