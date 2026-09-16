# `read-model` the Lifer and Laggard Way!

This is an extraction of a fun and enlightening buildathon at my co-mentoring community, in response to a revealing
question an engineer at a Lifer company asked me:

> "How do you deal with too much data in your React UX?" (He meant `read model`, but he didn't know the term.)

You DON'T! You _**architect**_ and **design** first.

> "Of course you do! We ALWAYS do!"

**His question revealed a gap in software engineering fundamentals.**

A brilliant Wizard once called this **"What should never be -- and IS!"**

He was running DevOps at a mid-size Lifer company I competence-coached for a year.

## Lifer's Professional Context

The _"always do"_ confirmed the assumptions I had about the company's level of engineering competence.
After years in an isolated IT department, like a little lake, an engineer can come to believe these are
"absolute facts" of corporate software development:

1. React is the main (or only) framework for building user interfaces.
2. "Too much data" will routinely be presented to any `read model` component.
3. Architecture has nothing to do with data size on presentation (or engineering).

Having coached competence at 11 such companies, I can explain all three.

1. Competence is constrained: meetings take the bulk of a developer's time, not learning and mastering what's
   available. "Blacklist" and "whitelist" antipatterns add superficial barriers to adopting new frameworks and libraries.
2. Collaboration is stifled by design: "backend" and "frontend" are silos, with separate engineers and entire teams
   holding conflicting KPIs.
3. Architecture is a napkin-drawing shop everyone ignores. What's on the napkin is not what's in production, and
   no other authority thinks about system architecture.

## Belief System of a Lifer

None of these three "laws" hold at places like, say, Google or Tastytrade. Still, the question gives us a remarkable
engineering problem to play with.

And there's never greater fun than torturing libraries and frameworks to destruction. The numbers we reached in our
experiment, like more than 300_000 browser-expanded nodes, rarely matter inside such a company.

They matter to startups and small companies serving Lifer customers: Owl-stuffing is a reality at laggards,
and you need clever ways to turn ALL of its costs back on them.

**IMPORTANT:** I'm not capturing or applying any of the adventures our community had with this build-and-break.
Instead, I am producing a clean and laser-focused benchmark fixture set to probe for exact SLAs.
This is your absolute ceiling.

## Where does Owl-stuffing Break the Browser (DOM)?

Everything runs in Chrome Browser Version 152.0.7977.65 (Official Build) (arm64).

1. **Par:** Google's own JSON Pretty-print -- a clean DOM read model for a simple data view.
2. **Pure TS** (ETALON) -- the leanest possible test for where owl-stuffing always breaks the browser.
3. **React Core** -- the leanest possible React, stripped down to only sound implementation decisions.
4. **`varabyte/kobweb`** (Compose HTML core) -- a Composable implementation of a client-side read model.

The article that prompted this torture-fest is right here:
[Web Showdown 2026: Read Model](https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html "Riddler's Blog.").

The results will also be published at the Riddler's Blog. Maybe even video.

**_I have left DOM-less solutions out for now -- way different animal._**

## Running Works Prep Outcome

You may want to run Claude Code or another agentic tool to make better sense of the results,
especially in comparison to your own codebase, or to add an element you have in your code.
For that, add your own CLAUDE.md and remove my restrictions in `.claude/settings.json`.

I don't provide an agentic setup here: mine is tuned to how I work, and yours should be tuned to how you work.

## The Actual 2026 Constraints that Matter for YOU

1. Chrome can manage a DOM of 300_000 nodes and 1_200_000 elements for you (not the 100_000 Google socializes).
2. Real-life `read model` rich UX applications should target the "Human User" ergonomic boundary instead.
3. Real-life DOM-based UX is all comparable and equivalent at around ~1_200 nodes.
4. Real-life DOM UX takes a steep dive at 20_000 - 30_000 nodes (not the 10_000 stated by the React team).

**Size is the DERIVATIVE of your design -- not its driver.**

_**For large data business cases use dedicated tooling, not the presentation layer.**_

## P.S. Is React the dog everyone says it is?!

**Run my code!** -- _you will be surprised._

... _maybe it's not the framework?_

