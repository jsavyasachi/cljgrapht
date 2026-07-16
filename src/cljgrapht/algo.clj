(ns cljgrapht.algo
  "Graph algorithms over `cljgrapht.core` graphs. Every function takes a graph
  and returns plain Clojure data (paths as vectors, components as sets, scores as
  maps), so results compose with the rest of your Clojure code.

  Direction matters: `connected-components` is for undirected graphs;
  `strongly-connected-components`, `topological-sort`, and `cycle?` are for
  directed graphs."
  (:require [cljgrapht.core :as core])
  (:import (java.util Collection HashSet)
           (java.util.function Supplier)
           (org.jgrapht Graph GraphPath)
           (org.jgrapht.graph DefaultEdge DefaultWeightedEdge)
           (org.jgrapht.graph.builder GraphTypeBuilder)
           (org.jgrapht.alg.shortestpath AStarShortestPath
                                         AllDirectedPaths
                                         BellmanFordShortestPath
                                         DijkstraShortestPath
                                         FloydWarshallShortestPaths
                                         JohnsonShortestPaths
                                         YenKShortestPath)
           (org.jgrapht.alg.interfaces AStarAdmissibleHeuristic
                                       PartitioningAlgorithm$Partitioning
                                       ShortestPathAlgorithm$SingleSourcePaths)
           (org.jgrapht.alg.connectivity ConnectivityInspector
                                         KosarajuStrongConnectivityInspector)
           (org.jgrapht.alg.cycle CycleDetector
                                  DirectedSimpleCycles
                                  JohnsonSimpleCycles)
           (org.jgrapht.alg.clique BronKerboschCliqueFinder)
           (org.jgrapht.alg.spanning PrimMinimumSpanningTree)
           (org.jgrapht.alg.interfaces MatchingAlgorithm$Matching
                                       MaximumFlowAlgorithm$MaximumFlow
                                       SpanningTreeAlgorithm$SpanningTree
                                       VertexColoringAlgorithm
                                       VertexColoringAlgorithm$Coloring)
           (org.jgrapht.alg.matching DenseEdmondsMaximumCardinalityMatching
                                     HopcroftKarpMaximumCardinalityBipartiteMatching)
           (org.jgrapht.alg.matching.blossom.v5 KolmogorovWeightedMatching
                                                ObjectiveSense)
           (org.jgrapht.alg.flow PushRelabelMFImpl)
           (org.jgrapht.alg.color GreedyColoring
                                  LargestDegreeFirstColoring
                                  SaturationDegreeColoring
                                  SmallestDegreeLastColoring)
           (org.jgrapht.alg.scoring BetweennessCentrality
                                    ClosenessCentrality
                                    ClusteringCoefficient
                                    Coreness
                                    EigenvectorCentrality
                                    HarmonicCentrality
                                    KatzCentrality
                                    PageRank)
           (org.jgrapht.alg.partition BipartitePartitioning)
           (org.jgrapht.alg.isomorphism VF2GraphIsomorphismInspector)
           (org.jgrapht.traverse BreadthFirstIterator
                                 DepthFirstIterator
                                 TopologicalOrderIterator)))

(defn- directed? [^Graph g]
  (.. g getType isDirected))

(defn- undirected? [^Graph g]
  (.. g getType isUndirected))

(defn- not-directed [^Graph g operation]
  (ex-info "JGraphT graph is not directed"
           {:cljgrapht/error :not-directed
            :cljgrapht/operation operation
            :cljgrapht/graph-type (.getType g)}))

(defn- not-undirected [^Graph g operation]
  (ex-info "JGraphT graph is not undirected"
           {:cljgrapht/error :not-undirected
            :cljgrapht/operation operation
            :cljgrapht/graph-type (.getType g)}))

(defn- unknown-algorithm [algorithm]
  (ex-info "Unknown graph coloring algorithm"
           {:cljgrapht/error :unknown-algorithm
            :cljgrapht/algorithm algorithm}))

(defn- unknown-vertex [operation vertex]
  (ex-info "Unknown vertex"
           {:cljgrapht/error :unknown-vertex
            :cljgrapht/operation operation
            :cljgrapht/vertex vertex}))

(defn- mixed-direction [operation]
  (ex-info "JGraphT graphs have mixed directedness"
           {:cljgrapht/error :mixed-direction
            :cljgrapht/operation operation}))

(defn- ensure-directed [^Graph g operation]
  (when-not (directed? g)
    (throw (not-directed g operation))))

(defn- ensure-undirected [^Graph g operation]
  (when-not (undirected? g)
    (throw (not-undirected g operation))))

(defn- edge-pair [^Graph g e]
  [(.getEdgeSource g e) (.getEdgeTarget g e)])

(defn- ensure-vertex [^Graph g operation vertex]
  (when-not (.containsVertex g vertex)
    (throw (unknown-vertex operation vertex))))

(defn- path-result [^GraphPath p]
  (when p
    {:path (vec (.getVertexList p))
     :weight (.getWeight p)}))

(defn- distances-result [^Graph g ^ShortestPathAlgorithm$SingleSourcePaths paths]
  (into {}
        (for [v (.vertexSet g)
              :let [w (.getWeight paths v)]
              :when (not (Double/isInfinite w))]
          [v w])))

(defn- matching-result [^Graph g ^MatchingAlgorithm$Matching matching]
  {:edges (set (map (fn [e] (edge-pair g e)) (.getEdges matching)))
   :size (count (.getEdges matching))})

(defn- weighted-matching-result [^Graph g ^MatchingAlgorithm$Matching matching]
  {:edges (set (map (fn [e] (edge-pair g e)) (.getEdges matching)))
   :weight (.getWeight matching)})

(defn- coloring-result [^VertexColoringAlgorithm algorithm]
  (let [^VertexColoringAlgorithm$Coloring coloring (.getColoring algorithm)]
    {:colors (into {} (.getColors coloring))
     :chromatic (.getNumberColors coloring)}))

(defn- graph-with-suppliers ^Graph [^Graph g]
  (let [weighted? (.. g getType isWeighted)
        ^Supplier vertex-supplier (reify Supplier
                                    (get [_] (Object.)))
        ^GraphTypeBuilder b (if (directed? g)
                              (GraphTypeBuilder/directed)
                              (GraphTypeBuilder/undirected))
        ^Graph copy (-> b
                        (.allowingMultipleEdges (.. g getType isAllowingMultipleEdges))
                        (.allowingSelfLoops (.. g getType isAllowingSelfLoops))
                        (.weighted weighted?)
                        (.vertexSupplier vertex-supplier)
                        (.edgeClass (if weighted? DefaultWeightedEdge DefaultEdge))
                        (.buildGraph))]
    (doseq [v (.vertexSet g)]
      (.addVertex copy v))
    (doseq [e (.edgeSet g)]
      (let [u (.getEdgeSource g e)
            v (.getEdgeTarget g e)
            copied-edge (.addEdge copy u v)]
        (when weighted?
          (.setEdgeWeight copy copied-edge (.getEdgeWeight g e)))))
    copy))

(defn shortest-path
  "Cheapest path from `src` to `dst` as `{:path [v ...] :weight w}`, or nil if
  unreachable. Uses Dijkstra; unweighted graphs use unit edge weights, so
  `:weight` is the hop count."
  [^Graph g src dst]
  (path-result (.getPath (DijkstraShortestPath. g) src dst)))

(defn astar
  "Cheapest path from `src` to `dst` as `{:path [v ...] :weight w}`, or nil if
  unreachable, using A* with `heuristic`, a function of `[vertex target]`."
  [^Graph g src dst heuristic]
  (let [h (reify AStarAdmissibleHeuristic
            (getCostEstimate [_ v target]
              (double (heuristic v target))))]
    (path-result (.getPath (AStarShortestPath. g h) src dst))))

(defn bellman-ford
  "Cheapest path from `src` to `dst` as `{:path [v ...] :weight w}`, or nil if
  unreachable. Supports negative edge weights but not negative cycles."
  [^Graph g src dst]
  (path-result (.getPath (BellmanFordShortestPath. g) src dst)))

(defn bellman-ford-distances
  "Map of every reachable vertex from `src` to its Bellman-Ford distance.
  Includes `src` with distance 0.0."
  [^Graph g src]
  (distances-result g (.getPaths (BellmanFordShortestPath. g) src)))

(defn bfs
  "Vector of vertices in breadth-first order from `start`."
  [^Graph g start]
  (ensure-vertex g :bfs start)
  (vec (iterator-seq (BreadthFirstIterator. g start))))

(defn dfs
  "Vector of vertices in depth-first pre-order from `start`. Neighbor
  visitation follows JGraphT's stack order: most-recently-added first."
  [^Graph g start]
  (ensure-vertex g :dfs start)
  (vec (iterator-seq (DepthFirstIterator. g start))))

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

(defn johnson-all-pairs
  "Nested map {u {v weight}} of cheapest path weights between every reachable
  ordered pair of distinct vertices (Johnson). Supports negative edge weights
  but not negative cycles."
  [^Graph g]
  (let [johnson (JohnsonShortestPaths. (graph-with-suppliers g))
        vs (core/vertices g)]
    (into {}
          (for [u vs]
            [u (into {}
                     (for [v vs
                           :when (not= u v)
                           :let [w (.getPathWeight johnson u v)]
                           :when (not (Double/isInfinite w))]
                       [v w]))]))))

(defn k-shortest-paths
  "`k` shortest simple paths from `src` to `dst`, as vectors of
  `{:path [v ...] :weight w}` maps (Yen)."
  [^Graph g src dst k]
  (mapv path-result (.getPaths (YenKShortestPath. g) src dst (int k))))

(defn all-simple-paths
  "All simple directed paths from `src` to `dst`, as vectors of
  `{:path [v ...] :weight w}` maps."
  [^Graph g src dst]
  (ensure-directed g :all-simple-paths)
  (mapv path-result (.getAllPaths (AllDirectedPaths. g) src dst true nil)))

(defn connected-components
  "Seq of vertex sets, one per connected component (undirected; for a directed
  graph these are the weakly-connected components)."
  [^Graph g]
  (map set (.connectedSets (ConnectivityInspector. g))))

(defn strongly-connected-components
  "Seq of vertex sets, one per strongly-connected component (directed)."
  [^Graph g]
  (map set (.stronglyConnectedSets (KosarajuStrongConnectivityInspector. g))))

(defn connected?
  "True if `g` is connected. Directed graphs are checked as weakly connected."
  [^Graph g]
  (.isConnected (ConnectivityInspector. g)))

(defn strongly-connected?
  "True if directed graph `g` is strongly connected."
  [^Graph g]
  (ensure-directed g :strongly-connected?)
  (.isStronglyConnected (KosarajuStrongConnectivityInspector. g)))

(defn cycle?
  "True if the directed graph `g` contains a cycle."
  [^Graph g]
  (.detectCycles (CycleDetector. g)))

(defn vertices-on-cycles
  "Set of vertices that participate in at least one cycle of directed graph `g`."
  [^Graph g]
  (set (.findCycles (CycleDetector. g))))

(defn dag?
  "True if directed graph `g` is acyclic."
  [^Graph g]
  (ensure-directed g :dag?)
  (not (cycle? g)))

(defn simple-cycles
  "Vector of simple directed cycles, each as a vector of vertices
  (JohnsonSimpleCycles)."
  [^Graph g]
  (ensure-directed g :simple-cycles)
  (mapv vec (.findSimpleCycles ^DirectedSimpleCycles (JohnsonSimpleCycles. g))))

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

(defn maximum-matching
  "Maximum cardinality matching of undirected graph `g` as
  `{:edges #{[u v] ...} :size n}` (Edmonds)."
  [^Graph g]
  (ensure-undirected g :maximum-matching)
  (let [^MatchingAlgorithm$Matching matching (.getMatching
                                              (DenseEdmondsMaximumCardinalityMatching. g))]
    (matching-result g matching)))

(defn maximum-weight-matching
  "Maximum weight matching of undirected graph `g` as
  `{:edges #{[u v] ...} :weight w}` (Kolmogorov blossom)."
  [^Graph g]
  (ensure-undirected g :maximum-weight-matching)
  (let [matching-graph (graph-with-suppliers g)
        ^MatchingAlgorithm$Matching matching (.getMatching
                                              (KolmogorovWeightedMatching.
                                               matching-graph ObjectiveSense/MAXIMIZE))]
    (weighted-matching-result matching-graph matching)))

(defn bipartite-matching
  "Maximum cardinality matching of bipartite graph `g` with vertex partitions
  `part1` and `part2`, as `{:edges #{[u v] ...} :size n}` (Hopcroft-Karp)."
  [^Graph g part1 part2]
  (let [^MatchingAlgorithm$Matching matching (.getMatching
                                              (HopcroftKarpMaximumCardinalityBipartiteMatching.
                                               g
                                               (HashSet. ^Collection part1)
                                               (HashSet. ^Collection part2)))]
    (matching-result g matching)))

(defn maximal-cliques
  "Seq of maximal cliques of undirected graph `g`, each as a vertex set
  (Bron-Kerbosch)."
  [^Graph g]
  (ensure-undirected g :maximal-cliques)
  (map set (iterator-seq (.iterator (BronKerboschCliqueFinder. g)))))

(defn bipartite?
  "True if `g` is bipartite."
  [^Graph g]
  (.isBipartite (BipartitePartitioning. g)))

(defn bipartite-sets
  "Two vertex partition sets when `g` is bipartite, otherwise nil."
  [^Graph g]
  (let [partitioning (BipartitePartitioning. g)]
    (when (.isBipartite partitioning)
      (let [^PartitioningAlgorithm$Partitioning p (.getPartitioning partitioning)]
        [(set (.getPartition p 0)) (set (.getPartition p 1))]))))

(defn density
  "Graph density as m divided by the number of possible non-loop edges."
  [^Graph g]
  (let [n (.size (.vertexSet g))
        m (.size (.edgeSet g))]
    (if (< n 2)
      0.0
      (double (if (directed? g)
                (/ m (* n (dec n)))
                (/ (* 2 m) (* n (dec n))))))))

(defn isolated-vertices
  "Set of vertices with degree zero."
  [^Graph g]
  (set (filter #(zero? (.degreeOf g %)) (.vertexSet g))))

(defn isomorphic?
  "True if `g1` and `g2` are graph-isomorphic according to VF2. Rejects mixed
  directed/undirected graph pairs."
  [^Graph g1 ^Graph g2]
  (when (not= (directed? g1) (directed? g2))
    (throw (mixed-direction :isomorphic?)))
  (.isomorphismExists (VF2GraphIsomorphismInspector. g1 g2)))

(defn max-flow
  "Maximum `source`->`sink` flow in directed graph `g` as
  `{:value flow-value :flow {[u v] flow-on-edge, ...}}` (Push-Relabel). Edge
  weights are capacities; zero-flow edges are omitted from `:flow`."
  [^Graph g source sink]
  (ensure-directed g :max-flow)
  (let [^MaximumFlowAlgorithm$MaximumFlow flow (.getMaximumFlow
                                                (PushRelabelMFImpl. g) source sink)]
    {:value (double (.getValue flow))
     :flow (into {}
                 (for [[e f] (.getFlowMap flow)
                       :let [f (double f)]
                       :when (not (zero? f))]
                   [(edge-pair g e) f]))}))

(defn min-cut
  "Minimum `source`->`sink` cut in directed graph `g` as
  `{:weight w :source-partition #{...} :sink-partition #{...}}` (Push-Relabel)."
  [^Graph g source sink]
  (ensure-directed g :min-cut)
  (let [impl (PushRelabelMFImpl. g)]
    {:weight (.calculateMinCut impl source sink)
     :source-partition (set (.getSourcePartition impl))
     :sink-partition (set (.getSinkPartition impl))}))

(defn coloring
  "Vertex coloring of `g` as `{:colors {vertex color-int, ...} :chromatic n}`.
  Options may include `:algorithm`, one of `:saturation` (default), `:greedy`,
  `:largest-degree-first`, or `:smallest-degree-last`."
  ([^Graph g]
   (coloring g {}))
  ([^Graph g {:keys [algorithm] :or {algorithm :saturation}}]
   (coloring-result
    (case algorithm
      :saturation (SaturationDegreeColoring. g)
      :greedy (GreedyColoring. g)
      :largest-degree-first (LargestDegreeFirstColoring. g)
      :smallest-degree-last (SmallestDegreeLastColoring. g)
      (throw (unknown-algorithm algorithm))))))

(defn greedy-coloring
  "Greedy vertex coloring of `g` as
  `{:colors {vertex color-int, ...} :chromatic n}`."
  [^Graph g]
  (coloring g {:algorithm :greedy}))

(defn clustering-coefficient
  "Map of vertex -> local clustering coefficient."
  [^Graph g]
  (into {} (.getScores (ClusteringCoefficient. g))))

(defn global-clustering-coefficient
  "Global clustering coefficient of `g`."
  [^Graph g]
  (.getGlobalClusteringCoefficient (ClusteringCoefficient. g)))

(defn coreness
  "Map of vertex -> core number."
  [^Graph g]
  (into {} (.getScores (Coreness. g))))

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

(defn harmonic-centrality
  "Map of vertex -> harmonic centrality score."
  [^Graph g]
  (into {} (.getScores (HarmonicCentrality. g))))

(defn eigenvector-centrality
  "Map of vertex -> eigenvector centrality score."
  [^Graph g]
  (into {} (.getScores (EigenvectorCentrality. g))))

(defn alpha-centrality
  "Map of vertex -> alpha-attenuated (Katz) centrality score. `alpha` defaults
  to JGraphT's damping factor."
  ([^Graph g]
   (into {} (.getScores (KatzCentrality. g))))
  ([^Graph g alpha]
   (into {} (.getScores (KatzCentrality. g (double alpha))))))
