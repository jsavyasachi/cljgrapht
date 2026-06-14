# cljgrapht

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/cljgrapht.svg)](https://clojars.org/net.clojars.savya/cljgrapht)
[![test](https://github.com/jsavyasachi/cljgrapht/actions/workflows/test.yml/badge.svg)](https://github.com/jsavyasachi/cljgrapht/actions/workflows/test.yml)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/cljgrapht)](https://cljdoc.org/d/net.clojars.savya/cljgrapht)

An idiomatic Clojure graph library backed by [JGraphT](https://jgrapht.org/):
build graphs over plain Clojure values and run JGraphT's fast, mature algorithm
catalog, getting plain Clojure data back.

## Stack

<a href="https://clojure.org"><img src="https://img.shields.io/badge/Clojure-5881D8?style=flat&logo=clojure&logoColor=fff" alt="Clojure" /></a>
<a href="https://jgrapht.org"><img src="https://img.shields.io/badge/JGraphT-1.5.3-4A86E8?style=flat" alt="JGraphT" /></a>

## Why

Graph work is the textbook case where Python's `networkx` is slow: it's pure
Python, and graph algorithms are irregular and pointer-chasing, so they never
vectorize into a C core the way numpy workloads do. On the JVM the same
algorithms run on JIT-compiled code with real threads.

`loom` and `ubergraph` are the established pure-Clojure graph libraries, and if
they cover your needs they're a great fit. `cljgrapht` takes a different tack:
instead of implementing algorithms in Clojure, it wraps [JGraphT](https://jgrapht.org/),
so you get its large, actively-developed algorithm catalog (shortest paths,
centrality, flow, matching, coloring, isomorphism, and more) running on the JVM,
behind a Clojure-shaped API. Reach for it when you want an algorithm the
pure-Clojure libraries don't ship, or JGraphT's performance on large graphs.
Vertices are any Clojure value; results come back as vectors, sets, and maps.

This is a performance wrapper, not a persistent data structure: graphs are
JGraphT's native mutable objects. Constructors and mutators return the graph for
threading, but they mutate in place.

Requires **JDK 11+** (JGraphT 1.5.x).

## Install

Leiningen (`project.clj`):

```clojure
[net.clojars.savya/cljgrapht "0.1.3"]
```

tools.deps (`deps.edn`):

```clojure
net.clojars.savya/cljgrapht {:mvn/version "0.1.3"}
```

## Usage

```clojure
(require '[cljgrapht.core :as g]
         '[cljgrapht.algo :as a])

;; Build a weighted directed graph from edge data.
(def road
  (g/weighted-digraph [[:a :b 1.0] [:a :c 4.0] [:b :c 1.0] [:c :d 1.0]]))

;; Cheapest route, as Clojure data.
(a/shortest-path road :a :d)
;; => {:path [:a :b :c :d] :weight 3.0}

;; Undirected social graph; who is most central?
(def social (g/graph [[:alice :bob] [:bob :carol] [:bob :dave] [:carol :dave]]))

(a/betweenness-centrality social)
;; => {:alice 0.0 :bob 2.0 :carol 0.0 :dave 0.0}

;; Dependency graph: order tasks, or detect a cycle.
(def deps (g/digraph [[:compile :test] [:compile :package] [:test :deploy]
                      [:package :deploy]]))

(a/topological-sort deps) ;; => [:compile :test :package :deploy]
(a/cycle? deps)           ;; => false
```

### What's in `cljgrapht.algo`

- Shortest paths: `shortest-path`, `shortest-path-length`,
  `all-pairs-shortest-path-length`
- Connectivity: `connected-components`, `strongly-connected-components`
- Ordering & cycles: `topological-sort`, `cycle?`, `vertices-on-cycles`
- Spanning: `minimum-spanning-tree`
- Centrality: `betweenness-centrality`, `closeness-centrality`, `pagerank`

## Performance

cljgrapht runs JGraphT's JIT-compiled Java algorithms instead of implementing
them in Clojure, so it's substantially faster on non-trivial graphs. Random
weighted digraphs, [criterium](https://github.com/hugoduncan/criterium)
`quick-bench`, Clojure 1.12.5 / JDK 17, mean time, graph construction excluded
from the algorithm rows. Source: [`bench/bench.clj`](bench/bench.clj).

**2,000 vertices / ~10k edges**

| Task | loom 1.0.2 | ubergraph 0.9.0 | cljgrapht |
|---|---|---|---|
| Build from edge list | 201 ms | 27 ms | 4.7 ms |
| Weighted shortest path (Dijkstra) | 7.3 ms | 5.2 ms | 0.27 ms |
| Connected components | 6.0 ms | 22 ms | 1.9 ms |

**10,000 vertices / ~50k edges**

| Task | loom 1.0.2 | ubergraph 0.9.0 | cljgrapht |
|---|---|---|---|
| Build from edge list | 963 ms | 182 ms | 27 ms |
| Weighted shortest path (Dijkstra) | 20 ms | 8.1 ms | 1.1 ms |
| Connected components | 28 ms | 95 ms | 13 ms |

This is the expected tradeoff of a native-Java engine, not a knock on loom or
ubergraph: both are pure-Clojure libraries with persistent, immutable graphs,
which cljgrapht gives up for speed. Reach for cljgrapht when graph size or
algorithm depth is the constraint.

## License

Copyright © 2026 Savyasachi

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/),
the same license JGraphT is available under.
