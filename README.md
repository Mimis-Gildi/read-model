# `read-model` the Lifer and Laggard Way!

This is an extraction of a fun and enlightening buildathon at my co-mentoring community in response to a revealing
question a Lifer-company engineer asked me. 

> "How do you deal with too much data in your React UX?" (He meant`read model`, but he didn't know the term.)

You DON'T! You _**architect**_ and **design** first.

> "Of course you do! We ALWAYS do!"

**His question revealed a glaring software engineering fundamentals understanding gap.** 

A brilliant Wizard once told me that it is **"What should never be -- and IS!"**

He was running DevOps at a mid-size Lifer-company I competence-coached for a year. 

## Lifer's Professional Context

The _"always do"_ proved all the assumptions I had about the company's level of engineering competence.
The cog demonstrated acceptance to all the following assumptions. 
After years in the Lifer-IT department, like in an isolated little lake, 
the "distinguished" believes these are "absolute facts" in corporate software development:

1. React is the main (or only) framework for building user interfaces with.
2. "Too much data" will be ubiquitously presented to any `read model` component.
3. Architecture has nothing to do with data size on presentation (or engineering).

Having coached competence to frontpage news at 11 such companies, I can explain all three.
Point 1: Competence is constrained -- "go to meeting" is the bulk of developer's time spent,
not learning and mastering what's available. This is also constrained by superficial barriers
to adoption of new frameworks and libraries: "blacklist" and "whitelist" antipatterns.
Point 2: Collaboration is stifled by design -- "backend" and "frontend" are silos: separate
engineers and entire teams with conflicting KPIs. Point 3: Architecture is a napkin-drawing shop everyone
ignores. What's on the napkin is not what's in production. And there is no other authority to
think about system architecture.

## Belief System of a Lifer

While that Lifer-engineer cannot imagine that neither of his three laws will ever exist at places like, 
say, Google or Tastytrade -- he does give us a remarkable, albeit useless, engineering problem to play with. 

And there's never greater fun than torturing libraries and frameworks to destruction. No Lifer-engineer
will ever read this repository or sources here: everything outside the company doesn't exist to them.
Even if they had read this and ran the code here, the numbers we attained in our experiment, like more than 
300_000 browser-expanded nodes, would have no meaning to them. 

This is useful for startups and small companies needing to deal with Lifer customers: know that Owl-stuffing 
is a reality at laggards, and you need to produce clever ways to turn ALL costs back on them.

**IMPORTANT:** I'm not capturing or applying any of the adventures our community had with this build-and-break.
Instead, I am producing a clean and laser-focused benchmark fixture set to probe for exact SLAs.
This is your absolute ceiling.

## Where does Owl-stuffing Break the Browser (DOM):

Everything happens in Chrome Browser Version 152.0.7977.65 (Official Build) (arm64).

1. **Par:** Google's own JSON Pretty-print: clean DOM read model for a simple data view.
2. My Pure JS -- the leanest possible test for where owl-stuffing always breaks the browser.
3. React Core -- the leanest possible React, stripped down to only sound implementation decisions.
4. React Full -- lifer-typical React app, internal event driven: the real laggard ceiling. 
5. `varabyte/kobweb` (Full) -- the best Composable implementation of a client-side read model.

The article that prompted this torture-fest is right here: 
[Web Showdown 2026: Read Model](https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html "Riddler's Blog.").

And the results will also be published at the Riddler's Blog. Maybe even video.

**_I have thrown out DOM-less solutions for now -- way different animal._**

## Running Works Prep Outcome

You may want to run Claude Code or another agentic tool to make better sense of the results.
Especially in comparison to your own codebase. Or to add the element you have in your code.
For that add your CLAUDE.md and remove all of my restrictions in `.claude/settings.json`.

Claude is "useless-slop" to me in my coding work. I am not biased, in fact the opposite: I'd
coached dozens of teams to agentic tooling excellence. I simply can't afford the time it takes 
to engineer an effective and worthwhile prompt for AI to slop what I do better and faster by myself.
Interpolating brick is a junior developer at best, but it can do many other useful chores for me.

This is why I didn't provide an agentic setup for you here. 

## The Actual 2026 Constraints that Matter for YOU

1. Chrome Browser can Manage 300_000 nodes, 1_200_000 elements DOM for you (Not 100_000 socialized by Google).
2. Real-life `read model` rich UX applications should target the "Human User" ergonomic boundary instead.
3. Real-life DOM-based UX is all comparable and equivalent at around ~1_200 nodes. 
4. Real-life DOM UX takes a steep dive at 20_000 - 30_000 nodes (not 10_000 stated by the React team.) 

**Size is the DERIVATIVE of your design -- not its driver.**

_**For large data business cases use dedicated tooling, not the presentation layer.**_
