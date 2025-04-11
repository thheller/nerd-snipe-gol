## nerd snipe game of life

Taken from https://github.com/andersmurphy/hyperlith/tree/master/examples/game_of_life

Triggered by https://www.reddit.com/r/Clojure/comments/1jvsvzb/why_i_always_use_clojurescript_in_my_frontend/

Please open issues here if you want to discuss any further. I hate reddit comment threads.

- Server is not optimized in any way. Not the point of this demo. Doesn't even do compression.
- Server deployment/build stuff is likely broken. I did not touch it.
- CLJS can be built via 2 builds, one showing just basic interop, one "high level" abstraction using shadow-grove

Development workflow basically same as described [here](https://code.thheller.com/blog/shadow-cljs/2024/10/18/fullstack-cljs-workflow-with-shadow-cljs.html).

```bash
# once, for http-kit, dunno why it is git url, original was
clojure -X:deps prep

# run development server
clj -X:dev repl/start
```

This starts a nrepl server via shadow-cljs. You can connect to it remotely via `.shadow-cljs/nrepl.port`.

Then open http://localhost:9630/builds and start a build, or uncomment the line in `src/dev/repl.clj` to start build automatically. `watch` is fine, `release` if you want smaller build and slightly faster perf. Don't start both builds at the same time, they share the same output files since I was lazy and didn't want to create 2 different "pages".

- `:app` build is basically just what you'd do in JS, no libraries, just plain CLJS. Wouldn't be any faster in plain JS, build would just be smaller. Stands at ~20.3 KB gzipped as is.
- `:grove-app` is using [shadow-grove](https://github.com/thheller/shadow-grove), only for the reason that I was curious. Holds up pretty well. Overkill for this. ~33.26 KB gzipped.

Either version beats the original by roughly 40x.

For the curious a basic build report can be generated via
```
npx shadow-cljs run shadow.cljs.build-report grove-app report.html
open report.html
```

Yes, the baseline cost is higher. I'm fine with that. It literally doesn't matter. I accidentally left this running all day while away, and it ended up downloading 1.7gb total. A bit of JS really makes no difference overall. Trying to code golf this smaller is irrelevant. It is much more relevant what your code does and hand-crafted code should always beat "generic" solutions. 

Sending 2500 divs as HTML it crazy for **this**. Parsing that alone already takes 10x longer than my solution in total. Diffing that is just bonkers, suffering the same ["What the heck just happened"](https://github.com/thheller/shadow-grove/blob/master/doc/what-the-heck-just-happened.md) dilemma as react in general. This wasn't even an attempt at writing the most optimized version possible, just the most straightforward thing I could think of.

Again, I'm in no way saying that D*/HTMX or whatever else in this world is bad. Pick the best fitting abstraction, never commit to just one thing.