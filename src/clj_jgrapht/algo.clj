(ns clj-jgrapht.algo
  "Graph algorithms over `clj-jgrapht.core` graphs. Every function takes a graph
  and returns plain Clojure data (paths as vectors, components as sets, scores as
  maps), so results compose with the rest of your Clojure code.

  Direction matters: `connected-components` is for undirected graphs;
  `strongly-connected-components`, `topological-sort`, and `cycle?` are for
  directed graphs."
  (:require [clj-jgrapht.core :as core])
  (:import (org.jgrapht Graph GraphPath)
           (org.jgrapht.alg.shortestpath DijkstraShortestPath
                                         FloydWarshallShortestPaths)
           (org.jgrapht.alg.connectivity ConnectivityInspector
                                         KosarajuStrongConnectivityInspector)
           (org.jgrapht.alg.cycle CycleDetector)
           (org.jgrapht.alg.spanning PrimMinimumSpanningTree)
           (org.jgrapht.alg.interfaces SpanningTreeAlgorithm$SpanningTree)
           (org.jgrapht.alg.scoring BetweennessCentrality
                                    ClosenessCentrality
                                    PageRank)
           (org.jgrapht.traverse TopologicalOrderIterator)))

(defn shortest-path
  "Cheapest path from `src` to `dst` as `{:path [v ...] :weight w}`, or nil if
  unreachable. Uses Dijkstra; unweighted graphs use unit edge weights, so
  `:weight` is the hop count."
  [^Graph g src dst]
  (let [^GraphPath p (.getPath (DijkstraShortestPath. g) src dst)]
    (when p
      {:path (vec (.getVertexList p))
       :weight (.getWeight p)})))

(defn shortest-path-length
  "Weight of the cheapest `src`->`dst` path, or nil if unreachable."
  [^Graph g src dst]
  (some-> (shortest-path g src dst) :weight))

(defn all-pairs-shortest-path-length
  "Nested map {u {v weight}} of cheapest path weights between every reachable
  ordered pair of distinct vertices (Floyd-Warshall)."
  [^Graph g]
  (let [fw (FloydWarshallShortestPaths. g)
        vs (core/vertices g)]
    (into {}
          (for [u vs]
            [u (into {}
                     (for [v vs
                           :when (not= u v)
                           :let [w (.getPathWeight fw u v)]
                           :when (not (Double/isInfinite w))]
                       [v w]))]))))

(defn connected-components
  "Seq of vertex sets, one per connected component (undirected; for a directed
  graph these are the weakly-connected components)."
  [^Graph g]
  (map set (.connectedSets (ConnectivityInspector. g))))

(defn strongly-connected-components
  "Seq of vertex sets, one per strongly-connected component (directed)."
  [^Graph g]
  (map set (.stronglyConnectedSets (KosarajuStrongConnectivityInspector. g))))

(defn cycle?
  "True if the directed graph `g` contains a cycle."
  [^Graph g]
  (.detectCycles (CycleDetector. g)))

(defn vertices-on-cycles
  "Set of vertices that participate in at least one cycle of directed graph `g`."
  [^Graph g]
  (set (.findCycles (CycleDetector. g))))

(defn topological-sort
  "Vector of vertices of directed acyclic graph `g` in topological order, or nil
  if `g` contains a cycle."
  [^Graph g]
  (when-not (cycle? g)
    (vec (iterator-seq (TopologicalOrderIterator. g)))))

(defn minimum-spanning-tree
  "Minimum spanning tree of weighted graph `g` as
  `{:edges #{[u v] ...} :weight w}` (Prim)."
  [^Graph g]
  (let [^SpanningTreeAlgorithm$SpanningTree st (.getSpanningTree
                                                (PrimMinimumSpanningTree. g))]
    {:edges (set (map (fn [e] [(.getEdgeSource g e) (.getEdgeTarget g e)])
                      (.getEdges st)))
     :weight (.getWeight st)}))

(defn betweenness-centrality
  "Map of vertex -> betweenness centrality score."
  [^Graph g]
  (into {} (.getScores (BetweennessCentrality. g))))

(defn closeness-centrality
  "Map of vertex -> closeness centrality score."
  [^Graph g]
  (into {} (.getScores (ClosenessCentrality. g))))

(defn pagerank
  "Map of vertex -> PageRank score."
  [^Graph g]
  (into {} (.getScores (PageRank. g))))
