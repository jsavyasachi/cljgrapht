# Migrating from Loom

This guide is for moving code from `aysylu/loom` or the
`net.clojars.savya/loom` fork to `cljgrapht`.

## When To Migrate

Migrate when you need JGraphT algorithms, JVM performance, or direct Java graph
interop.

Do not migrate when you need ClojureScript or immutable graph values. `loom`
graphs are immutable values. `cljgrapht` wraps mutable JGraphT graphs. Mutators
return the graph for threading, but they change the same object in place.

## Function Mapping

| loom.graph / loom.alg | cljgrapht |
|---|---|
| `loom.graph/graph` | `cljgrapht.core/graph` |
| `loom.graph/digraph` | `cljgrapht.core/digraph` |
| `loom.graph/weighted-graph` | `cljgrapht.core/weighted-graph` |
| `loom.graph/weighted-digraph` | `cljgrapht.core/weighted-digraph` |
| `loom.graph/nodes` | `cljgrapht.core/vertices` |
| `loom.graph/edges` | `cljgrapht.core/edges` |
| `loom.graph/successors` | `cljgrapht.core/successors` |
| `loom.graph/predecessors` | `cljgrapht.core/predecessors` |
| `loom.graph/add-nodes` | `cljgrapht.core/add-vertex`, repeated |
| `loom.graph/add-edges` | `cljgrapht.core/add-edge`, repeated |
| `loom.alg/bf-traverse` | `cljgrapht.algo/bfs` |
| `loom.alg/bf-path` | `cljgrapht.algo/shortest-path` |
| `loom.alg/dijkstra-path` | `cljgrapht.algo/shortest-path` |
| `loom.alg/dijkstra-path-dist` | `cljgrapht.algo/shortest-path` or `shortest-path-length` |
| `loom.alg/bellman-ford` | `cljgrapht.algo/bellman-ford` |
| `loom.alg/astar-path` | `cljgrapht.algo/astar` |
| `loom.alg/astar-dist` | `cljgrapht.algo/astar`, then `:weight` |
| `loom.alg/topsort` | `cljgrapht.algo/topological-sort` |
| `loom.alg/dag?` | `cljgrapht.algo/dag?` |
| `loom.alg/connected-components` | `cljgrapht.algo/connected-components` |
| `loom.alg/connected?` | `cljgrapht.algo/connected?` |
| `loom.alg/scc` | `cljgrapht.algo/strongly-connected-components` |
| `loom.alg/strongly-connected?` | `cljgrapht.algo/strongly-connected?` |
| `loom.alg/bipartite?` | `cljgrapht.algo/bipartite?` |
| `loom.alg/bipartite-sets` | `cljgrapht.algo/bipartite-sets` |
| `loom.alg/greedy-coloring` | `cljgrapht.algo/greedy-coloring` |
| `loom.alg/max-flow` | `cljgrapht.algo/max-flow` |
| `loom.alg/prim-mst` | `cljgrapht.algo/minimum-spanning-tree` |
| `loom.alg/maximal-cliques` | `cljgrapht.algo/maximal-cliques` |
| `loom.alg/clustering-coefficient` | `cljgrapht.algo/clustering-coefficient` |
| `loom.alg/loners` | `cljgrapht.algo/isolated-vertices` |
| `loom.alg/density` | `cljgrapht.algo/density` |
| `loom.alg/distinct-edges` | `cljgrapht.core/edges`, then normalize if needed |
| `loom.alg/isomorphism?` | `cljgrapht.algo/isomorphic?` |
| `loom.alg/pre-traverse` | `cljgrapht.algo/dfs` |

## Beyond Loom

These `cljgrapht` functions expose JGraphT capabilities without direct Loom
counterparts.

| cljgrapht | Notes |
|---|---|
| `cljgrapht.algo/bellman-ford-distances` | Single-source Bellman-Ford distances. |
| `cljgrapht.algo/all-pairs-shortest-path-length` | Floyd-Warshall all-pairs shortest paths. |
| `cljgrapht.algo/johnson-all-pairs` | Johnson all-pairs shortest paths. |
| `cljgrapht.algo/k-shortest-paths` | Yen k shortest simple paths. |
| `cljgrapht.algo/all-simple-paths` | All simple directed paths between two vertices. |
| `cljgrapht.algo/cycle?` | Directed cycle detection. |
| `cljgrapht.algo/vertices-on-cycles` | Vertices participating in directed cycles. |
| `cljgrapht.algo/simple-cycles` | Directed simple cycle enumeration. |
| `cljgrapht.algo/maximum-matching` | Edmonds maximum cardinality matching. |
| `cljgrapht.algo/maximum-weight-matching` | Kolmogorov weighted matching. |
| `cljgrapht.algo/bipartite-matching` | Hopcroft-Karp bipartite matching. |
| `cljgrapht.algo/min-cut` | Minimum source/sink cut from push-relabel. |
| `cljgrapht.algo/coloring` | Selectable coloring algorithms. |
| `cljgrapht.algo/global-clustering-coefficient` | Whole-graph clustering score. |
| `cljgrapht.algo/coreness` | Core number per vertex. |
| `cljgrapht.algo/betweenness-centrality` | Betweenness centrality scores. |
| `cljgrapht.algo/closeness-centrality` | Closeness centrality scores. |
| `cljgrapht.algo/pagerank` | PageRank scores. |
| `cljgrapht.gen/complete-graph` | JGraphT complete graph generator. |
| `cljgrapht.gen/ring-graph` | JGraphT ring graph generator. |
| `cljgrapht.gen/star-graph` | JGraphT star graph generator. |
| `cljgrapht.gen/grid-graph` | JGraphT grid graph generator. |
| `cljgrapht.gen/gnp-random-graph` | Seedable Erdos-Renyi generator. |
| `cljgrapht.gen/barabasi-albert-graph` | Seedable Barabasi-Albert generator. |
| `cljgrapht.gen/watts-strogatz-graph` | Seedable Watts-Strogatz generator. |
| `cljgrapht.io/dot` | DOT export. |
| `cljgrapht.io/read-dot` | DOT import with string vertex ids. |
| `cljgrapht.io/graphml` | GraphML export. |

## Not Covered

| Loom area | cljgrapht status | Workaround |
|---|---|---|
| `loom.alg/post-traverse` | No direct wrapper. | Use JGraphT interop directly or keep Loom for that traversal. |
| `loom.attr` | No separate attribute namespace. | Put attributes in vertex values or maintain an external map. |
| `loom.io/view` | No visualization wrapper. | Export DOT with `cljgrapht.io/dot` and render with Graphviz. |
| ClojureScript | Not supported. | Keep Loom for CLJS code. |

## Attribute Pattern

Loom attribute code often keeps identity and attributes separate:

```clojure
(require '[loom.graph :as lg]
         '[loom.attr :as attr])

(-> (lg/graph [:a :b])
    (attr/add-attr :a :label "Alice"))
```

In `cljgrapht`, vertices can be arbitrary Clojure values. Put the attributes in
the vertex value when that fits your model:

```clojure
(require '[cljgrapht.core :as g])

(def alice {:id :a :label "Alice"})
(def bob {:id :b :label "Bob"})

(def gr (g/graph [[alice bob]]))
```

If vertex identity and attributes must change independently, keep vertices as
stable ids and store attributes in a separate map.

## Incremental Migration With cljgrapht.loom

`cljgrapht.loom` extends Loom protocols to raw JGraphT graphs. Load the
namespace for side effects, then Loom algorithms can consume a `cljgrapht` graph:

```clojure
(require '[cljgrapht.core :as g]
         '[cljgrapht.loom]
         '[loom.alg :as alg])

(def gr (g/weighted-digraph [[:a :b 1.0] [:a :c 10.0] [:b :c 1.0]]))

(alg/dijkstra-path gr :a :c)
;; => (:a :b :c)
```

This lets you migrate construction first, then replace Loom algorithms one call
site at a time.

## DFS Ordering

Loom `pre-traverse` visits neighbors in adjacency order. `cljgrapht.algo/dfs`
uses JGraphT's stack-based iterator, so later neighbors can be visited first.
If exact traversal order is observable in your tests, update the expected order
while migrating.
