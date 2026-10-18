/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import {readFileSync} from 'node:fs';
import {createRequire, registerHooks} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {JSDOM} from 'jsdom';

/** npm runs scripts from the fixture root. */
const FIXTURE = pathToFileURL(`${process.cwd()}/`);
const HARNESS = new URL('../harness/build/dist/js/productionLibrary/', FIXTURE);
const VENDOR = /^\/vendor\/[^/]+\/(.+)\.mjs$/;

/** Under test a vendored module is its npm package: the bundle is shipped for the browser. */
const {vendor = {}}: { vendor?: Record<string, string> } =
    JSON.parse(readFileSync(new URL('package.json', FIXTURE), 'utf8'));

let generation = 0;

// noinspection JSUnusedGlobalSymbols
/**
 * The ESM cache cannot be cleared.
 */
export const resetModules = (): void => void generation++;

const mapped = (specifier: string): string =>
    specifier === '/test/setup.ts' ? import.meta.url
        : specifier.startsWith('/harness/') ? new URL(specifier.slice('/harness/'.length), HARNESS).href
            : vendor[VENDOR.exec(specifier)?.[1] ?? ''] ?? specifier;

/** Node strips types but refuses JSX. */
const tsx = (url: string): string =>
    createRequire(FIXTURE)('esbuild').transformSync(readFileSync(fileURLToPath(url), 'utf8'),
        {loader: 'tsx', format: 'esm', sourcefile: fileURLToPath(url)}).code;

registerHooks({
    resolve(specifier, context, nextResolve) {
        const resolved = nextResolve(mapped(specifier), context);
        return resolved.url.startsWith('file:') && !resolved.url.includes('/node_modules/') && resolved.url !== import.meta.url
            ? {...resolved, url: `${resolved.url}?generation=${generation}`}
            : resolved;
    },
    load(url, context, nextLoad) {
        return url.startsWith('file:') && new URL(url).pathname.endsWith('.tsx')
            ? {format: 'module', source: tsx(url), shortCircuit: true}
            : nextLoad(url, context);
    },
});

const {window} = new JSDOM('<!doctype html><html lang="en"><body></body></html>', {url: 'http://localhost/'});
const browser = window as unknown as Record<string, unknown>;
Object.getOwnPropertyNames(window)
    .filter((name) => !name.startsWith('_') && !(name in globalThis))
    .forEach((name) => Object.defineProperty(globalThis, name, {configurable: true, get: () => browser[name]}));
