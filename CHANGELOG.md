# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

## Unreleased

## 0.4.0 - 2026-07-08

### Added
- `cljgrapht.gen` graph generators for complete, ring, star, grid,
  Erdos-Renyi, Barabasi-Albert, and Watts-Strogatz graphs.
- `cljgrapht.io` DOT and GraphML export helpers, DOT import, and file writers.
- cljdoc migration guide for moving from Loom to cljgrapht.

## 0.3.0 - 2026-07-08

### Added
- Shortest-path algorithms in `cljgrapht.algo`: `astar`, `bellman-ford`,
  `bellman-ford-distances`, `johnson-all-pairs`, and `k-shortest-paths`.
- Directed path and cycle enumeration: `all-simple-paths` and `simple-cycles`.
- Graph predicates and measures: `dag?`, `connected?`,
  `strongly-connected?`, `density`, `isolated-vertices`, and `isomorphic?`.
- Undirected clique and scoring algorithms: `maximal-cliques`,
  `clustering-coefficient`, `global-clustering-coefficient`, and `coreness`.
- Bipartite helpers: `bipartite?` and `bipartite-sets`.
- Traversal helpers: `bfs` and `dfs`.

## [0.2.0] - 2026-07-07

### Added
- Matching algorithms in `cljgrapht.algo`: `maximum-matching` (Edmonds),
  `maximum-weight-matching` (Kolmogorov blossom V), `bipartite-matching`
  (Hopcroft-Karp).
- Flow algorithms: `max-flow` and `min-cut` (push-relabel; edge weights are
  capacities).
- Vertex coloring: `coloring` (DSatur default, `:greedy`,
  `:largest-degree-first`, `:smallest-degree-last` selectable via
  `:algorithm`) and `greedy-coloring`.
- `cljgrapht.loom` - optional loom protocol interop: extends loom's `Graph`,
  `Digraph`, `WeightedGraph`, and `EditableGraph` to raw `org.jgrapht.Graph`
  so `loom.alg` runs directly on cljgrapht graphs. Loom stays out of
  cljgrapht's dependencies; bring your own loom artifact.

## [0.1.4] - 2026-06-14

### Changed
- Standardize README structure and badges (docs only).

## [0.1.3] - 2026-06-13

### Added
- README "Performance" section benchmarking cljgrapht against loom and ubergraph,
  plus a reproducible `bench/` harness (criterium).

## [0.1.2] - 2026-06-13

### Fixed
- Correct the copyright attribution in the README to "Savyasachi". Docs only.

## [0.1.1] - 2026-06-13

### Changed
- Reword the README "Why" section to describe cljgrapht by its capabilities and
  credit loom/ubergraph, rather than disparaging them. Docs only; no code change.

## [0.1.0] - 2026-06-13

Initial release: an idiomatic Clojure graph library backed by JGraphT 1.5.3.

### Added
- `cljgrapht.core`: graph constructors (`graph`, `digraph`, `weighted-graph`,
  `weighted-digraph`) over Clojure-value vertices, mutators (`add-vertex`,
  `add-edge`), and accessors (`vertices`, `edges`, `neighbors`, `successors`,
  `predecessors`, `weight`).
- `cljgrapht.algo`: `shortest-path`, `shortest-path-length`,
  `all-pairs-shortest-path-length`, `connected-components`,
  `strongly-connected-components`, `topological-sort`, `cycle?`,
  `vertices-on-cycles`, `minimum-spanning-tree`, `betweenness-centrality`,
  `closeness-centrality`, `pagerank`.
- Reflection-clean; CI across JDK 11/17/21 and Clojure 1.10/1.11/1.12.
