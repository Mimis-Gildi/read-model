# `read-model` the Laggard Way!

This is an extraction of a fun and enlightening buildathon at my co-mentoring community in response to a revealing
question a laggard engineer asked me during a technical interview. The question revealed a glaring understanding gap
for software engineering fundamentals. It proved all assumptions about the company's level of engineering competence.
And the cog demonstrated accepting all the following assumptions as absolute facts in corporate software development:

1. React is the main framework for building user interfaces with.
2. "Too much data" will be presented to any `read model` component.
3. Architecture has nothing to do with data size on presentation.

While that engineer cannot imagine that neither of the three will ever be true at places like, say, Google or 
Tastytrade -- he does give us a remarkable albeit useless engineering problem to play with. And there's never
greater fun than torturing libraries and frameworks to destruction. So here we will go do so with joy. In the 
end, we should have some useful metrics about various engineering dependency choices data-abused at laggards.

1. Pure JavaScript in Chrome Browser -- where does owl-stuffing break the browser, at best.
2. React -- where does owl-stuffing break the browser, and how does it compare to the Pure JS.
3. Optionally, several other practical `read-model` choices -- how do hacker tools take abuse.

The article that prompted this torture-fest is right here: 
[Web Showdown 2026: Read Model](https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html "Riddler's Blog.").

And the results will also be published at the Riddler's Blog.

## Running Works

Claude is generally a useless slop, but we will still make an attempt to have it useful, at least in some aspects,
at least for getting coffee, tea, and other trivial tasks. Here are dumb assumptions Claude made right from the go:

What is being counted
1. That the ramp numbers (1,000 / 5,000 / 15,000 / 100,000) are live DOM nodes, not row items. I flagged this as an open fork once, then quietly proceeded as if it were nodes.
2. That "live DOM node" is the primary metric at all, rather than time, memory, or frames.
3. That every framework emits the same DOM shape per row, so node counts are comparable. I called this a precondition, then assumed it was solved.

What counts as a result
4. That "successful render" is observable and self-evident. I never asked what marks a render complete — paint? commit? some sentinel in the DOM?
5. That "destruction" is a single detectable event, and that OOM-kill, unresponsive-tab, and merely-unusable-jank are distinguishable and attributable to the framework.
6. That the SLA-ish figures in the article (Chrome 50k/100k, React ~10k) are ground truth rather than the priors you're putting under test. I've quoted them back at you as facts more than once.

How the number is obtained
7. That measurement is in-browser and self-reported by the page — performance.now() around an expand, roughly — rather than driven and recorded externally.
8. That it's automated, not a human watching a screen.
9. That a run is a single run. No warmup, no repetitions, no variance, no discarding first-load effects — I've never mentioned distribution once.
10. That one machine and one browser build is the fixture.

What's inside the measurement boundary
11. Contradictory, and this is the worst one: for the dataset design I assumed the server is an inert fixture outside the loop; then in that scaffold review I asserted server CPU, gzip, and cache headers would "corrupt bench numbers." I can't have both. I never decided where  
    the boundary is — wire time in or out.
12. That the frame arrives over HTTP rather than being generated in the browser.
13. That each framework runs a production build, not a dev build.
14. That the dataset is byte-identical across frameworks and across runs.

The two that actually block everything: where the measurement boundary sits (does transport count?), and what event stops the clock. Your production-fidelity requirement pushes hard on #11 — a real app pays for the network, so excluding it may be the wrong call.

## The Actual Constraints that Matter
