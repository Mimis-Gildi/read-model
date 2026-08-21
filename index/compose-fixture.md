# Remaining Plan

Fluent-functional gaps in `service/build.gradle.kts`:

1. L138-160: imperative if/else with side-effecting launch in both branches, duplicated
   `launchBrowserOnSocketBackoffRetry(...)` call shape. Collapse to one
   `val invokeBrowser: () -> Unit = ...` chosen by branch, one launch at the end.
   Compounding it: the outer `if` is used as a statement while both branches yield values
   (`Desktop` vs `DisposableHandle`, unified to `Any`) — the expression's value dangles.

2. L149-153: `when ... in OsMatcher(...)` then `.let {}` just to pass to the same call as the
   Desktop branch — could unify both platform strategies into a single lazy value resolved once,
   rather than two independent launch blocks. `OsMatcher` also allocates three instances per
   evaluation and inverts reading direction via `operator contains` to save nothing over a
   plain predicate.

3. L203 `.also{}` used for a log side-effect on a Unit-returning `withContext` — `also` returns
   the receiver so it "works," but it's `Unit.also{}`; plain sequencing would be more honest.

4. L164: the lazy chain is forced eagerly. `launchBrowserOnRunProvider.get()` inside the
   configuration block collapses a carefully-built `Provider` into a `Boolean` at the one moment
   laziness was the point. `onlyIf { }`, or wiring the provider into the dependency, keeps the
   chain intact. Largest mindset violation in the file.

5. L207-214: the recursion silently discards three of its own parameters. `maxAttempts`,
   `minDelayMs` and `maxDelayMs` are not propagated, so every recursive call re-reads the
   defaults — a caller passing `maxAttempts = 3` binds it on attempt 1 and reverts to 50 from
   attempt 2 onward. The signature lies. Not style; a defect.

6. L128-131: `convergeBrowserLauncher` cancels a scope it does not own. The receiver is one
   `Job`; the effect is cancelling `browserLauncherScope` for everyone. Correct today only
   because exactly one coroutine exists. The name promises Job-scoped convergence and delivers
   scope-wide termination.

7. L136/L142: `URI` round-tripped through `String` to rebuild itself —
   `URI.create(controlPlaneAppLocalUrl)` then `URI(controlPlane.toString())`.
   `browse(controlPlane)` is the same value with two conversions removed.

8. L194-200: try/catch synthesising a Boolean flag. `runCatching { Socket(...).use {} }.isFailure`
   states it in one expression. `closed` is also inverted naming — computed from *failure to
   connect* — and L202 and L207 branch on that same predicate twice, so one condition drives two
   separate `if` statements. A single `when` over the probe result expresses
   "expired → recurse / opened → done" once.

9. L153/L156: `it` crossing two lambda boundaries. The `.let {}` receives the command list; three
   lines later `commandLine(it)` resolves to that same outer `it` because `exec {}` binds a
   receiver, not a parameter. Compiles, and is opaque. Name the list.

Closed:
- `GlobalScope` / `@DelicateCoroutinesApi` replaced by an owned `CoroutineScope` with a
  completion hook.
- Provider chain no longer reads backwards: presence is now a key lookup via
  `environmentVariablesPrefixedBy`, so an empty-but-set variable counts as set.
