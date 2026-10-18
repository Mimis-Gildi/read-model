/*
 * Copyright 2026 @rdd13r (Vadim Kuhay)
 * All rights reserved except as granted by the Apache License, Version 2.0; see LICENSE.
 */

import {registerHooks} from 'node:module';
import {JSDOM} from 'jsdom';

const HARNESS = new URL('../../../../harness/build/dist/js/productionLibrary/', import.meta.url);

let generation = 0;

/**
 * The ESM cache cannot be cleared, so it is outgrown instead: every fixture module resolved after this call is a fresh
 * instance keyed by the generation, and the modules it imports while loading land in the same generation.
 * This module is exempt, or the test would reset a copy of the counter the hook never reads.
 */
export const resetModules = (): void => void generation++;

registerHooks({
    resolve(specifier, context, nextResolve) {
        const resolved = nextResolve(
            specifier.startsWith('/harness/') ? new URL(specifier.slice('/harness/'.length), HARNESS).href : specifier,
            context);
        return resolved.url.startsWith('file:') && !resolved.url.includes('/node_modules/') && resolved.url !== import.meta.url
            ? {...resolved, url: `${resolved.url}?generation=${generation}`}
            : resolved;
    },
});

/** Node already owns some of these names (Event, navigator, URL...); those stay Node's. */
const {window} = new JSDOM('<!doctype html><html lang="en"><body></body></html>', {url: 'http://localhost/'});
const browser = window as unknown as Record<string, unknown>;
Object.getOwnPropertyNames(window)
    .filter((name) => !name.startsWith('_') && !(name in globalThis))
    .forEach((name) => Object.defineProperty(globalThis, name, {configurable: true, get: () => browser[name]}));
