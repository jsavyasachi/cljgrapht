# Change Log

All notable changes to this project are documented here. This change log follows
the conventions of [keepachangelog.com](http://keepachangelog.com/).

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
