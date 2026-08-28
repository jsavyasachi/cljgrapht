# cljgrapht

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.savya/cljgrapht.svg)](https://clojars.org/net.clojars.savya/cljgrapht)
[![cljdoc](https://cljdoc.org/badge/net.clojars.savya/cljgrapht)](https://cljdoc.org/d/net.clojars.savya/cljgrapht)
[![test](https://github.com/jsavyasachi/cljgrapht/actions/workflows/test.yml/badge.svg)](https://github.com/jsavyasachi/cljgrapht/actions/workflows/test.yml)

A Clojure graph library that uses [JGraphT](https://jgrapht.org/). You build
graphs over plain Clojure values, run JGraphT's algorithms, and get plain
Clojure data back.

## Stack

<a href="https://clojure.org"><img src="https://img.shields.io/badge/Clojure-5881D8?style=flat&logo=clojure&logoColor=fff" alt="Clojure" /></a>
<a href="https://clojure.org/guides/deps_and_cli"><img src="https://img.shields.io/badge/deps.edn-5881D8?style=flat&logo=clojure&logoColor=fff" alt="deps.edn" /></a>
<a href="https://clojure.github.io/tools.build/"><img src="https://img.shields.io/badge/tools.build-5881D8?style=flat&logo=clojure&logoColor=fff" alt="tools.build" /></a>
<a href="https://jgrapht.org"><img src="https://img.shields.io/badge/JGraphT-1.5.3-4A86E8?style=flat" alt="JGraphT" /></a>

## Why

Python's `networkx` is slow because it is pure Python. Graph algorithms are
irregular and they chase pointers, so they do not vectorize into a C core as
numpy workloads do. On the JVM the same algorithms run on JIT-compiled code with
real threads.

`loom` and `ubergraph` are pure-Clojure graph libraries. Use one of them if it
covers your needs. `cljgrapht` uses a different approach. It does not write the
algorithms in Clojure. It wraps [JGraphT](https://jgrapht.org/) and puts a
Clojure API in front of the JGraphT algorithm catalog (shortest paths,
centrality, flow, matching, coloring, isomorphism, and more). Use cljgrapht when
you want an algorithm that the pure-Clojure libraries do not have, or when you
want JGraphT's performance on large graphs. A vertex can be any Clojure value.
Results come back as vectors, sets, and maps.

This is a performance wrapper, not a persistent data structure. Graphs are
JGraphT's native mutable objects. Constructors and mutators return the graph so
that you can thread calls, but they mutate the graph in place.

Requires **JDK 11+** (JGraphT 1.5.x).

## Installation

tools.deps (`deps.edn`):

```clojure
net.clojars.savya/cljgrapht {:mvn/version "1.2.0"}
```

Leiningen (`project.clj`):

```clojure
[net.clojars.savya/cljgrapht "1.2.0"]
```

Run tests with `clojure -M:test`. Build a jar with `clojure -T:build jar` and
deploy to Clojars with `clojure -T:build deploy`.

### Multigraph edge identity

Graphs created with `:allow-multiple-edges? true` preserve parallel-edge identity
in algorithm results. Path results (`shortest-path`, `astar`, Bellman-Ford,
Yen, Suurballe, all-path, Eulerian, and Chinese-postman variants) include an
`:edges` vector of the selected JGraphT edge objects. Matching and spanning
results include `:edge-objects`, and flow results include `:edge-flow`, keyed by
the actual edge objects. The existing endpoint-based `:edges` and `:flow` values
remain available; for ordinary graphs result shapes are unchanged. Weighted
`add-edge` assigns the weight to the exact newly added edge, so parallel edges
can carry different weights safely. Core edge vectors from `edges`,
`incident-edges`, `incoming-edges`, and `outgoing-edges` retain their existing
shape and carry the edge object as `(:edge (meta edge-vector))`.

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

- Shortest paths: `shortest-path`, `shortest-path-length`, `astar`,
  `bellman-ford`, `bellman-ford-distances`,
  `all-pairs-shortest-path-length`, `johnson-all-pairs`,
  `k-shortest-paths`, `yen-k-shortest-paths`, `eppstein-k-shortest-paths`,
  `bfs-shortest-path`, `dijkstra-many-to-many-paths`, `all-simple-paths`
- Connectivity: `connected-components`, `strongly-connected-components`,
  `connected?`, `strongly-connected?`
- Ordering & cycles: `topological-sort`, `dag?`, `cycle?`,
  `vertices-on-cycles`, `simple-cycles`
- Spanning: `minimum-spanning-tree`
- Matching: `maximum-matching` (Edmonds), `maximum-weight-matching`
  (Kolmogorov blossom V), `bipartite-matching` (Hopcroft-Karp),
  `bipartite?`, `bipartite-sets`
- Flow: `max-flow`, `min-cut` (push-relabel; edge weights are capacities)
- Coloring: `coloring` (DSatur default; `:greedy`, `:largest-degree-first`,
  `:smallest-degree-last` via `:algorithm`), `greedy-coloring`
- Link prediction: ten standard predictors via `link-prediction-score` and
  `predict-links`
- Lowest common ancestor: `lca` and `lca-set` with naive and rooted variants
- Steiner trees: approximate weighted `steiner-tree`
- Line graphs: `line-graph`, including optional edge-weight conversion
- Dense subgraphs: `maximum-density-subgraph` with caller-provided sentinels
- Centrality: `betweenness-centrality`, `edge-betweenness-centrality`,
  `closeness-centrality`, `pagerank`
- Clustering and layouts: `clustering` with `:greedy-modularity`, `modularity`,
  and `layout-2d` with circular, Fruchterman-Reingold, and random algorithms
- Graph shape and scoring: `maximal-cliques`, `clustering-coefficient`,
  `global-clustering-coefficient`, `coreness`, `density`,
  `isolated-vertices`, `isomorphic?`

### What's in `cljgrapht.gen`

- Graph generators: `complete-graph`, `ring-graph`, `star-graph`,
  `grid-graph`
- Seedable random generators: `gnp-random-graph`, `barabasi-albert-graph`,
  `watts-strogatz-graph`

### What's in `cljgrapht.io`

- DOT: `dot`, `write-dot!`, `read-dot`
- GraphML: `graphml`, `write-graphml!`
- TSPLIB: `read-tsplib`, `read-tsplib-tour`

## Loom interop

`cljgrapht.loom` extends [loom](https://github.com/aysylu/loom)'s `Graph`,
`Digraph`, `WeightedGraph`, and `EditableGraph` protocols to raw
`org.jgrapht.Graph`, so loom's generic algorithms (`loom.alg`) run directly on
cljgrapht graphs:

```clojure
(require '[cljgrapht.core :as g]
         '[cljgrapht.loom]  ;; load the protocol extensions
         '[loom.alg :as alg])

(def gr (g/weighted-digraph [[:a :b 1.0] [:a :c 10.0] [:b :c 1.0]]))
(alg/dijkstra-path gr :a :c) ;; => (:a :b :c)
```

Loom is not a dependency of cljgrapht. Add a loom artifact
(`net.clojars.savya/loom` or `aysylu/loom`) to your own deps before you require
`cljgrapht.loom`. Loom's `EditableGraph` operations mutate the underlying
JGraphT graph in place and return the same instance. Loom's persistent graph
records behave differently.

## Performance

These benchmarks use random weighted digraphs and
[criterium](https://github.com/hugoduncan/criterium) `quick-bench`. They use
Clojure 1.12.5 and JDK 17. They exclude graph construction from the algorithm
rows. Source: [`bench/bench.clj`](bench/bench.clj).

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

This is the tradeoff of a native Java engine. loom and ubergraph use persistent,
immutable graphs. cljgrapht uses mutable graphs instead. Use cljgrapht when
graph size or algorithm depth is the constraint.

## License

Copyright © 2026 Savyasachi

Distributed under the [Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/),
the same license JGraphT is available under.
