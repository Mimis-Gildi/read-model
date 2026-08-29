# `read-model` the Lifer and Laggard Way!

This is an extraction of a fun and enlightening buildathon at my co-mentoring community in response to a revealing
question a laggard engineer asked me during a technical interview. The question revealed a glaring understanding gap
for software engineering fundamentals. It proved all assumptions about the company's level of engineering competence.
And the cog demonstrated accepting all the following assumptions as absolute facts in corporate software development:

1. React is the main (and only) framework for building user interfaces with.
2. "Too much data" will be ubiquitously presented to any `read model` component.
3. Architecture has nothing to do with data size on presentation (or engineering).

While that engineer cannot imagine that neither of the three will ever be true at places like, say, Google or 
Tastytrade -- he does give us a remarkable, albeit useless, engineering problem to play with. And there's never
greater fun than torturing libraries and frameworks to destruction. So here we will go do so with joy. In the 
end, we should have some useful metrics about various engineering dependency choices data-abused at laggards.

IMPORTANT: I'm not capturing or applying any of the adventures our community had with this build-and-break.
Instead, I am producing clean and laser-focused benchmark fixture set to probe exact SLA aspects of tech.

## Where does Owl-stuffing break the browser

Everything happens in Chrome Browser Version 152.0.7977.65 (Official Build) (arm64).

1. (Par) Google's own JSON Pretty-print: clean DOM read model for simple data view.
2. My Pure JS -- leanest possible test for where does owl-stuffing break the browser.
3. React Core -- leanest possible React, stripped down to only SOUND architectural decisions.
4. React Full -- lifer-typical React app, internal event driven: the real laggard ceiling. 
5. `varabyte/kobweb` -- the best composable implementation of client-side read model.
6. (Optionally) if I find time, having the working fixture, i will throw more here. 

The article that prompted this torture-fest is right here: 
[Web Showdown 2026: Read Model](https://mimis-gildi.github.io/riddle-me-this/adventures/2026/08/02/web-showdown.html "Riddler's Blog.").

And the results will also be published at the Riddler's Blog. Maybe even video.

**_I have thrown out DOM-less solutions for now -- way different animal._**

## Running Works Prep Outcome

Claude is generally useless slop -- junior at best, 
but I will still attempt to make some use of it, 
at least for some HTML.

After a few attempts I concluded Claude Code just doesn't save me any time -- it take longer 
rewriting its trash than making everything from scratch by myself. Anything above junior or 
trivial just impossible to get help with. Go ahead and criticize. You're welcome to use it.
What worked for me instead: fetch me those sources, get me those docs, or make me some coffee.

Edit or delete `.claude/settings.json` to remove my restrictions on it.

## The Actual Constraints that Mattered
