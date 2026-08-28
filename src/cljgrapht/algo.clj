(ns cljgrapht.algo
  "Graph algorithms over `cljgrapht.core` graphs. Every function takes a graph
  and returns plain Clojure data: paths as vectors, components as sets, and
  scores as maps. The results compose with the rest of your Clojure code.

  Direction is important. `connected-components` is for undirected graphs.
  `strongly-connected-components`, `topological-sort`, and `cycle?` are for
  directed graphs."
  (:require [cljgrapht.core :as core])
  (:import (java.util ArrayList Collection HashSet)
           (java.util.concurrent Executors ThreadPoolExecutor)
           (java.util.function BiFunction Function Supplier)
           (org.jgrapht Graph GraphPath GraphTests)
           (org.jgrapht.graph DefaultEdge DefaultWeightedEdge SimpleDirectedGraph)
           (org.jgrapht.graph.builder GraphTypeBuilder)
           (org.jgrapht.alg StoerWagnerMinimumCut TransitiveClosure TransitiveReduction)
           (org.jgrapht.alg.shortestpath AStarShortestPath
                                         AllDirectedPaths
                                         BellmanFordShortestPath
                                         BidirectionalDijkstraShortestPath
                                         ContractionHierarchyBidirectionalDijkstra
                                         DeltaSteppingShortestPath
                                         DijkstraShortestPath
                                         DijkstraManyToManyShortestPaths
                                         BFSShortestPath
                                         EppsteinKShortestPath
                                         FloydWarshallShortestPaths
                                         GraphMeasurer
                                         JohnsonShortestPaths
                                         SuurballeKDisjointShortestPaths
                                         YenKShortestPath)
           (org.jgrapht.alg.interfaces AStarAdmissibleHeuristic
                                       LinkPredictionAlgorithm
                                       LowestCommonAncestorAlgorithm
                                       MaximumDensitySubgraphAlgorithm
                                       SteinerTreeAlgorithm$SteinerTree
                                       PartitioningAlgorithm$Partitioning
                                       ShortestPathAlgorithm$SingleSourcePaths)
           (org.jgrapht.alg.connectivity ConnectivityInspector
                                         BiconnectivityInspector
                                         GabowStrongConnectivityInspector
                                         KosarajuStrongConnectivityInspector)
           (org.jgrapht.alg.cycle CycleDetector
                                  ChinesePostman
                                  ChordalityInspector
                                  DirectedSimpleCycles
                                  HierholzerEulerianCycle
                                  JohnsonSimpleCycles
                                  PatonCycleBase
                                  SzwarcfiterLauerSimpleCycles
                                  TarjanSimpleCycles)
           (org.jgrapht.alg.clique BronKerboschCliqueFinder
                                   ChordalGraphMaxCliqueFinder
                                   DegeneracyBronKerboschCliqueFinder
                                   PivotBronKerboschCliqueFinder)
           (org.jgrapht.alg.spanning PrimMinimumSpanningTree)
           (org.jgrapht.alg.steiner KouMarkowskyBermanAlgorithm)
           (org.jgrapht.alg.interfaces MatchingAlgorithm$Matching
                                       MaximumFlowAlgorithm$MaximumFlow
                                       MinimumCostFlowAlgorithm$MinimumCostFlow
                                       CapacitatedSpanningTreeAlgorithm$CapacitatedSpanningTree
                                       SpanningTreeAlgorithm$SpanningTree
                                       VertexCoverAlgorithm$VertexCover
                                       VertexColoringAlgorithm
                                       VertexColoringAlgorithm$Coloring)
           (org.jgrapht.alg.matching DenseEdmondsMaximumCardinalityMatching
                                     GreedyMaximumCardinalityMatching
                                     GreedyWeightedMatching
                                     HopcroftKarpMaximumCardinalityBipartiteMatching
                                     KuhnMunkresMinimalWeightBipartitePerfectMatching
                                     PathGrowingWeightedMatching
                                     SparseEdmondsMaximumCardinalityMatching)
           (org.jgrapht.alg.matching.blossom.v5 KolmogorovWeightedMatching
                                                ObjectiveSense)
           (org.jgrapht.alg.vertexcover BarYehudaEvenTwoApproxVCImpl
                                        ClarksonTwoApproxVCImpl
                                        EdgeBasedTwoApproxVCImpl
                                        GreedyVCImpl
                                        RecursiveExactVCImpl)
           (org.jgrapht.alg.tour ChristofidesThreeHalvesApproxMetricTSP
                                 GreedyHeuristicTSP
                                 HeldKarpTSP
                                 NearestInsertionHeuristicTSP
                                 NearestNeighborHeuristicTSP
                                 PalmerHamiltonianCycle
                                 RandomTourTSP
                                 TwoOptHeuristicTSP)
           (org.jgrapht.alg.clustering GirvanNewmanClustering
                                       KSpanningTreeClustering
                                       LabelPropagationClustering)
           (org.jgrapht.alg.flow DinicMFImpl
                                  EdmondsKarpMFImpl
                                  GusfieldGomoryHuCutTree
                                  PushRelabelMFImpl)
           (org.jgrapht.alg.flow.mincost CapacityScalingMinimumCostFlow
                                          MinimumCostFlowProblem$MinimumCostFlowProblemImpl)
           (org.jgrapht.alg.spanning BoruvkaMinimumSpanningTree
                                     EsauWilliamsCapacitatedMinimumSpanningTree
                                     GreedyMultiplicativeSpanner
                                     KruskalMinimumSpanningTree)
           (org.jgrapht.alg.color ChordalGraphColoring
                                  GreedyColoring
                                  ColorRefinementAlgorithm
                                  LargestDegreeFirstColoring
                                  RandomGreedyColoring
                                  SaturationDegreeColoring
                                  SmallestDegreeLastColoring)
           (org.jgrapht.alg.scoring BetweennessCentrality
                                    EdgeBetweennessCentrality
                                    ClosenessCentrality
                                    ClusteringCoefficient
                                    Coreness
                                    EigenvectorCentrality
                                    HarmonicCentrality
                                    KatzCentrality
                                    PageRank)
           (org.jgrapht.alg.partition BipartitePartitioning)
           (org.jgrapht.alg.independentset ChordalGraphIndependentSetFinder)
           (org.jgrapht.alg.linkprediction CommonNeighborsLinkPrediction
                                          AdamicAdarIndexLinkPrediction
                                          HubDepressedIndexLinkPrediction
                                          HubPromotedIndexLinkPrediction
                                          JaccardCoefficientLinkPrediction
                                          PreferentialAttachmentLinkPrediction
                                          LeichtHolmeNewmanIndexLinkPrediction
                                          ResourceAllocationIndexLinkPrediction
                                          SaltonIndexLinkPrediction
                                          SorensenIndexLinkPrediction)
           (org.jgrapht.alg.lca BinaryLiftingLCAFinder
                               EulerTourRMQLCAFinder
                               HeavyPathLCAFinder
                               NaiveLCAFinder
                               TarjanLCAFinder)
           (org.jgrapht.alg.transform LineGraphConverter)
           (org.jgrapht.alg.isomorphism AHURootedTreeIsomorphismInspector
                                          AHUUnrootedTreeIsomorphismInspector
                                          ColorRefinementIsomorphismInspector
                                          VF2GraphIsomorphismInspector
                                          VF2SubgraphIsomorphismInspector)
           (org.jgrapht.alg.similarity ZhangShashaTreeEditDistance)
           (org.jgrapht.alg.planar BoyerMyrvoldPlanarityInspector)
           (org.jgrapht.alg.decomposition DulmageMendelsohnDecomposition)
           (org.jgrapht.alg.clustering GreedyModularityAlgorithm
                                       UndirectedModularityMeasurer)
           (org.jgrapht.alg.drawing LayoutAlgorithm2D
                                    CircularLayoutAlgorithm2D
                                    FRLayoutAlgorithm2D
                                    RandomLayoutAlgorithm2D)
           (org.jgrapht.alg.drawing.model Box2D MapLayoutModel2D Point2D)
           (org.jgrapht.alg.densesubgraph GoldbergMaximumDensitySubgraphAlgorithm)
           (org.jgrapht.alg.util Pair Triple)
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

(defn- multigraph? [^Graph g]
  (.. g getType isAllowingMultipleEdges))

(defn- ensure-vertex [^Graph g operation vertex]
  (when-not (.containsVertex g vertex)
    (throw (unknown-vertex operation vertex))))

(defn- ensure-positive-int [value operation option]
  (when-not (and (integer? value) (pos? value))
    (throw (ex-info (str (name option) " must be a positive integer")
                    {:cljgrapht/error :invalid-option
                     :cljgrapht/operation operation
                     :cljgrapht/option option
                     :cljgrapht/value value}))))

(defn- link-prediction-algorithm ^LinkPredictionAlgorithm
  [^Graph g algorithm]
  (case algorithm
    :common-neighbors (CommonNeighborsLinkPrediction. g)
    :jaccard (JaccardCoefficientLinkPrediction. g)
    :salton (SaltonIndexLinkPrediction. g)
    :sorensen (SorensenIndexLinkPrediction. g)
    :resource-allocation (ResourceAllocationIndexLinkPrediction. g)
    :hub-promoted (HubPromotedIndexLinkPrediction. g)
    :hub-depressed (HubDepressedIndexLinkPrediction. g)
    :preferential-attachment (PreferentialAttachmentLinkPrediction. g)
    :adamic-adar (AdamicAdarIndexLinkPrediction. g)
    :leicht-holme-newman (LeichtHolmeNewmanIndexLinkPrediction. g)
    (throw (ex-info "Unknown link prediction algorithm"
                    {:cljgrapht/error :unknown-algorithm
                     :cljgrapht/algorithm algorithm}))))

(defn- lca-algorithm ^LowestCommonAncestorAlgorithm
  [^Graph g algorithm {:keys [root roots]}]
  (case algorithm
    :naive (NaiveLCAFinder. g)
    :binary-lifting (let [roots (or roots (when (some? root) #{root}))]
      (when-not roots
        (throw (ex-info "Rooted LCA algorithms require :root or :roots"
                        {:cljgrapht/error :missing-root
                         :cljgrapht/algorithm algorithm})))
      (if root (BinaryLiftingLCAFinder. g root)
          (BinaryLiftingLCAFinder. g roots)))
    :euler-tour-rmq (let [roots (or roots (when (some? root) #{root}))]
                      (when-not roots
                        (throw (ex-info "Rooted LCA algorithms require :root or :roots"
                                        {:cljgrapht/error :missing-root
                                         :cljgrapht/algorithm algorithm})))
                      (if root (EulerTourRMQLCAFinder. g root)
                          (EulerTourRMQLCAFinder. g roots)))
    :heavy-path (let [roots (or roots (when (some? root) #{root}))]
                  (when-not roots
                    (throw (ex-info "Rooted LCA algorithms require :root or :roots"
                                    {:cljgrapht/error :missing-root
                                     :cljgrapht/algorithm algorithm})))
                  (if root (HeavyPathLCAFinder. g root)
                      (HeavyPathLCAFinder. g roots)))
    :tarjan (let [roots (or roots (when (some? root) #{root}))]
              (when-not roots
                (throw (ex-info "Rooted LCA algorithms require :root or :roots"
                                {:cljgrapht/error :missing-root
                                 :cljgrapht/algorithm algorithm})))
              (if root (TarjanLCAFinder. g root)
                  (TarjanLCAFinder. g roots)))
    (throw (ex-info "Unknown LCA algorithm"
                    {:cljgrapht/error :unknown-algorithm
                     :cljgrapht/algorithm algorithm}))))

(defn link-prediction-score
  "Predict a link score between `u` and `v`."
  [^Graph g u v {:keys [algorithm] :or {algorithm :common-neighbors}}]
  (.predict (link-prediction-algorithm g algorithm) u v))

(defn lca
  "Lowest common ancestor, or nil when none exists."
  ([^Graph g a b] (lca g a b {}))
  ([^Graph g a b opts]
   (.getLCA (lca-algorithm g (:algorithm opts :naive) opts) a b)))

(defn steiner-tree
  "Approximate weighted Steiner tree spanning `terminals`."
  [^Graph g terminals]
  (let [^SteinerTreeAlgorithm$SteinerTree tree
        (.getSteinerTree (KouMarkowskyBermanAlgorithm. g) terminals)]
    (cond-> {:edges (set (map #(edge-pair g %) (.getEdges tree)))
             :weight (.getWeight tree)}
      (multigraph? g) (assoc :edge-objects (set (.getEdges tree))))))

(defn line-graph
  "Return the line graph of `g`. With `weight-fn`, produce weighted line edges."
  ([^Graph g]
   (line-graph g nil))
  ([^Graph g weight-fn]
   (let [weighted? (some? weight-fn)
         ^Graph target (core/make-graph {:directed? (core/directed? g)
                                         :weighted? weighted?
                                         :edge-class DefaultEdge})
         ^LineGraphConverter converter (LineGraphConverter. g)]
     (if weight-fn
       (let [^BiFunction edge-weight (reify BiFunction
                                       (apply [_ e1 e2]
                                         (double (weight-fn e1 e2))))]
         (.convertToLineGraph converter target edge-weight))
       (.convertToLineGraph converter target))
     target)))

(defn maximum-density-subgraph
  "Find a densest subgraph. `s` and `t` must be different sentinel vertices.
  `g` must not contain them."
  ([^Graph g s t]
   (maximum-density-subgraph g s t {:epsilon 1e-9}))
  ([^Graph g s t {:keys [epsilon] :or {epsilon 1e-9}}]
   (when (or (= s t) (.containsVertex g s) (.containsVertex g t))
     (throw (ex-info "Density sentinels must be distinct and absent from the graph"
                     {:cljgrapht/error :invalid-sentinels
                      :cljgrapht/source s
                      :cljgrapht/sink t})))
   (let [^MaximumDensitySubgraphAlgorithm algorithm
         (GoldbergMaximumDensitySubgraphAlgorithm. g s t (double epsilon))
         ^Graph result (.calculateDensest algorithm)]
     (cond-> {:vertices (set (.vertexSet result))
              :edges (set (map #(edge-pair result %) (.edgeSet result)))
              :density (.getDensity algorithm)}
       (multigraph? result) (assoc :edge-objects (set (.edgeSet result)))))))

(defn lca-set
  "Set of lowest common ancestors, possibly empty."
  ([^Graph g a b] (lca-set g a b {}))
  ([^Graph g a b opts]
   (let [^LowestCommonAncestorAlgorithm algorithm
         (lca-algorithm g (:algorithm opts :naive) opts)]
     (try
       (set (.getLCASet algorithm a b))
       (catch UnsupportedOperationException _
         (if-let [ancestor (.getLCA algorithm a b)]
           #{ancestor}
           #{}))))))

(defn naive-lca [^Graph g a b]
  (lca g a b {:algorithm :naive}))

(defn predict-links
  "Predict scores for `[u v]` pairs as a map."
  (^clojure.lang.IPersistentMap [^Graph g pairs]
   (predict-links g pairs {}))
  (^clojure.lang.IPersistentMap [^Graph g pairs {:keys [algorithm]
                                                :or {algorithm :common-neighbors}}]
   (let [^LinkPredictionAlgorithm predictor (link-prediction-algorithm g algorithm)
         ^ArrayList requests (ArrayList.)]
     (doseq [[u v] pairs]
       (.add requests (Pair/of u v)))
     (into {}
           (map (fn [^Triple result]
                  [[(.getFirst result) (.getSecond result)] (.getThird result)])
                (.predict predictor requests))))))

(defn common-neighbors-score [^Graph g u v]
  (link-prediction-score g u v {:algorithm :common-neighbors}))
(defn jaccard-coefficient [^Graph g u v]
  (link-prediction-score g u v {:algorithm :jaccard}))
(defn salton-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :salton}))
(defn sorensen-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :sorensen}))
(defn resource-allocation-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :resource-allocation}))
(defn hub-promoted-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :hub-promoted}))
(defn hub-depressed-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :hub-depressed}))
(defn preferential-attachment-score [^Graph g u v]
  (link-prediction-score g u v {:algorithm :preferential-attachment}))
(defn adamic-adar-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :adamic-adar}))
(defn leicht-holme-newman-index [^Graph g u v]
  (link-prediction-score g u v {:algorithm :leicht-holme-newman}))
(defn- path-result [^GraphPath p]
  (when p
    (cond-> {:path (vec (.getVertexList p))
             :weight (.getWeight p)}
      (multigraph? (.getGraph p))
      (assoc :edges (vec (.getEdgeList p))))))

(defn- distances-result [^Graph g ^ShortestPathAlgorithm$SingleSourcePaths paths]
  (into {}
        (for [v (.vertexSet g)
              :let [w (.getWeight paths v)]
              :when (not (Double/isInfinite w))]
          [v w])))

(defn- matching-result [^Graph g ^MatchingAlgorithm$Matching matching]
  (cond-> {:edges (set (map (fn [e] (edge-pair g e)) (.getEdges matching)))
           :size (count (.getEdges matching))}
    (multigraph? g) (assoc :edge-objects (set (.getEdges matching)))))

(defn- weighted-matching-result [^Graph g ^MatchingAlgorithm$Matching matching]
  (cond-> {:edges (set (map (fn [e] (edge-pair g e)) (.getEdges matching)))
           :weight (.getWeight matching)}
    (multigraph? g) (assoc :edge-objects (set (.getEdges matching)))))

(defn- spanning-tree-result [^Graph g ^SpanningTreeAlgorithm$SpanningTree tree]
  (cond-> {:edges (set (map #(edge-pair g %) (.getEdges tree)))
           :weight (.getWeight tree)}
    (multigraph? g) (assoc :edge-objects (set (.getEdges tree)))))

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
  (ensure-vertex g :shortest-path src)
  (ensure-vertex g :shortest-path dst)
  (path-result (.getPath (DijkstraShortestPath. g) src dst)))

(defn bfs-shortest-path
  "Shortest unweighted path from `src` to `dst`, or nil when unreachable."
  [^Graph g src dst]
  (ensure-vertex g :bfs-shortest-path src)
  (ensure-vertex g :bfs-shortest-path dst)
  (path-result (.getPath (BFSShortestPath. g) src dst)))

(defn dijkstra-many-to-many-paths
  "Nested map of reachable shortest-path results for source and target sets."
  [^Graph g sources targets]
  (doseq [v (concat sources targets)]
    (ensure-vertex g :dijkstra-many-to-many-paths v))
  (let [paths (.getManyToManyPaths (DijkstraManyToManyShortestPaths. g)
                                   (set sources) (set targets))]
    (into {}
          (for [src sources]
            [src (into {}
                       (keep (fn [dst]
                               (when-let [p (.getPath paths src dst)]
                                 [dst (path-result p)])))
                       targets)]))))

(defn astar
  "Cheapest path from `src` to `dst` as `{:path [v ...] :weight w}`, or nil if
  unreachable. Uses A* with `heuristic`, a function of `[vertex target]`."
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
  "Vector of vertices in depth-first pre-order from `start`. It visits
  neighbors in JGraphT's stack order: most-recently-added first."
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

(defn bidirectional-shortest-path
  "Cheapest path from `src` to `dst` with bidirectional Dijkstra."
  [^Graph g src dst]
  (path-result (.getPath (BidirectionalDijkstraShortestPath. g) src dst)))

(defn delta-stepping-shortest-path
  "Cheapest path from `src` to `dst` with parallel delta-stepping."
  [^Graph g src dst]
  (let [^ThreadPoolExecutor executor (Executors/newFixedThreadPool 1)]
    (try
      (path-result (.getPath (DeltaSteppingShortestPath. g executor) src dst))
      (finally
        (.shutdownNow executor)))))

(defn contraction-hierarchy-shortest-path
  "Cheapest path from `src` to `dst` with a contraction hierarchy."
  [^Graph g src dst]
  (let [^ThreadPoolExecutor executor (Executors/newFixedThreadPool 1)]
    (try
      (path-result
       (.getPath (ContractionHierarchyBidirectionalDijkstra. g executor) src dst))
      (finally
        (.shutdownNow executor)))))

(defn yen-k-shortest-paths
  "The `k` shortest loopless paths from `src` to `dst` with Yen's algorithm."
  [^Graph g src dst k]
  (ensure-vertex g :yen-k-shortest-paths src)
  (ensure-vertex g :yen-k-shortest-paths dst)
  (ensure-positive-int k :yen-k-shortest-paths :k)
  (mapv path-result (.getPaths (YenKShortestPath. g) src dst (int k))))

(defn eppstein-k-shortest-paths
  "The `k` shortest paths from `src` to `dst` with Eppstein's algorithm."
  [^Graph g src dst k]
  (ensure-vertex g :eppstein-k-shortest-paths src)
  (ensure-vertex g :eppstein-k-shortest-paths dst)
  (when-not (pos? (int k))
    (throw (ex-info "k must be positive"
                    {:cljgrapht/error :invalid-option :cljgrapht/option :k :value k})))
  (mapv path-result (.getPaths (EppsteinKShortestPath. g) src dst (int k))))

(defn disjoint-shortest-paths
  "Up to `k` edge-disjoint shortest paths from `src` to `dst` with Suurballe."
  [^Graph g src dst k]
  (ensure-directed g :disjoint-shortest-paths)
  (mapv path-result
        (.getPaths (SuurballeKDisjointShortestPaths. g) src dst (int k))))

(defn all-directed-paths
  "All directed paths from `src` to `dst`. Options are `:simple?` (default true)
  and `:max-length`, the maximum number of edges."
  ([^Graph g src dst]
   (all-directed-paths g src dst {}))
  ([^Graph g src dst {:keys [simple? max-length]
                      :or {simple? true}}]
   (ensure-directed g :all-directed-paths)
   (mapv path-result
         (.getAllPaths (AllDirectedPaths. g) src dst (boolean simple?)
                       (when (some? max-length) (Integer/valueOf (int max-length)))))))

(defn transitive-reduction
  "Edges of the transitive reduction of directed acyclic graph `g`."
  [^Graph g]
  (ensure-directed g :transitive-reduction)
  (let [copy (graph-with-suppliers g)]
    (.reduce TransitiveReduction/INSTANCE copy)
    (set (map #(edge-pair copy %) (.edgeSet copy)))))

(defn transitive-closure
  "Edges of the transitive closure of directed graph `g`."
  [^Graph g]
  (ensure-directed g :transitive-closure)
  (let [^SimpleDirectedGraph copy (SimpleDirectedGraph. DefaultEdge)]
    (doseq [v (.vertexSet g)]
      (.addVertex copy v))
    (doseq [e (.edgeSet g)]
      (.addEdge copy (.getEdgeSource g e) (.getEdgeTarget g e)))
    (.closeSimpleDirectedGraph TransitiveClosure/INSTANCE copy)
    (set (map #(edge-pair copy %) (.edgeSet copy)))))

(defn connected-components
  "Seq of vertex sets, one per connected component (undirected; for a directed
  graph these are the weakly-connected components)."
  [^Graph g]
  (map set (.connectedSets (ConnectivityInspector. g))))

(defn strongly-connected-components
  "Seq of vertex sets, one per strongly-connected component (directed)."
  [^Graph g]
  (map set (.stronglyConnectedSets (KosarajuStrongConnectivityInspector. g))))

(defn gabow-strongly-connected-components
  "Seq of strongly-connected vertex sets with Gabow's algorithm."
  [^Graph g]
  (ensure-directed g :gabow-strongly-connected-components)
  (map set (.stronglyConnectedSets (GabowStrongConnectivityInspector. g))))

(defn kosaraju-strongly-connected-components
  "Seq of strongly-connected vertex sets with Kosaraju's algorithm."
  [^Graph g]
  (ensure-directed g :kosaraju-strongly-connected-components)
  (map set (.stronglyConnectedSets (KosarajuStrongConnectivityInspector. g))))

(defn articulation-points
  "Set of articulation vertices in an undirected graph."
  [^Graph g]
  (ensure-undirected g :articulation-points)
  (set (.getCutpoints (BiconnectivityInspector. g))))

(defn bridges
  "Set of bridge edges as `[source target]` pairs in an undirected graph."
  [^Graph g]
  (ensure-undirected g :bridges)
  (set (map #(edge-pair g %) (.getBridges (BiconnectivityInspector. g)))))

(defn biconnected-components
  "Seq of vertex sets, one per maximal biconnected block."
  [^Graph g]
  (ensure-undirected g :biconnected-components)
  (map #(set (.vertexSet ^Graph %)) (.getBlocks (BiconnectivityInspector. g))))

(defn blocks
  "Seq of vertex sets, one per block of an undirected graph."
  [^Graph g]
  (biconnected-components g))

(defn block-cut-tree
  "Block-cut tree as block sets, articulation vertices, and `[block cutpoint]`
  edges."
  [^Graph g]
  (ensure-undirected g :block-cut-tree)
  (let [block-sets (set (biconnected-components g))
        cutpoints (articulation-points g)]
    {:blocks block-sets
     :articulation-points cutpoints
     :edges (set (for [block block-sets
                       cutpoint cutpoints
                       :when (contains? block cutpoint)]
                   [block cutpoint]))}))

(defn condensation
  "Condensation DAG of a directed graph as SCC vertex sets and edges between
  those sets."
  [^Graph g]
  (ensure-directed g :condensation)
  (let [^Graph condensed (.getCondensation
                          (KosarajuStrongConnectivityInspector. g))
        component (fn [^Graph subgraph] (set (.vertexSet subgraph)))]
    {:components (set (map component (.vertexSet condensed)))
     :edges (set (for [e (.edgeSet condensed)]
                   [(component (.getEdgeSource condensed e))
                    (component (.getEdgeTarget condensed e))]))}))

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

(defn- directed-cycles [^Graph g operation ^DirectedSimpleCycles algorithm]
  (ensure-directed g operation)
  (mapv vec (.findSimpleCycles algorithm)))

(defn johnson-simple-cycles
  "Simple directed cycles with Johnson's algorithm."
  [^Graph g]
  (directed-cycles g :johnson-simple-cycles (JohnsonSimpleCycles. g)))

(defn tarjan-simple-cycles
  "Simple directed cycles with Tarjan's algorithm."
  [^Graph g]
  (directed-cycles g :tarjan-simple-cycles (TarjanSimpleCycles. g)))

(defn szwarcfiter-lauer-simple-cycles
  "Simple directed cycles with the Szwarcfiter-Lauer algorithm."
  [^Graph g]
  (directed-cycles g :szwarcfiter-lauer-simple-cycles
                   (SzwarcfiterLauerSimpleCycles. g)))

(defn cycle-basis
  "Paton cycle basis as `{:cycles [[v ...] ...] :length n :weight w}`."
  [^Graph g]
  (ensure-undirected g :cycle-basis)
  (let [basis (.getCycleBasis (PatonCycleBase. g))]
    {:cycles (mapv #(vec (.getVertexList ^GraphPath %))
                   (.getCyclesAsGraphPaths basis))
     :length (.getLength basis)
     :weight (.getWeight basis)}))

(defn eulerian?
  "True when `g` has an Eulerian cycle."
  [^Graph g]
  (GraphTests/isEulerian g))

(defn eulerian-cycle
  "Eulerian cycle as `{:path [v ...] :weight w}`, or nil when none exists."
  [^Graph g]
  (when (eulerian? g)
    (path-result (.getEulerianCycle (HierholzerEulerianCycle.) g))))

(defn chinese-postman
  "Minimum closed walk covering every edge as `{:path [v ...] :weight w}`."
  [^Graph g]
  (path-result (.getCPPSolution (ChinesePostman.) g)))

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
    (spanning-tree-result g st)))

(defn prim-minimum-spanning-tree
  "Minimum spanning tree with Prim's algorithm."
  [^Graph g]
  (spanning-tree-result g (.getSpanningTree (PrimMinimumSpanningTree. g))))

(defn kruskal-minimum-spanning-tree
  "Minimum spanning tree with Kruskal's algorithm."
  [^Graph g]
  (spanning-tree-result g (.getSpanningTree (KruskalMinimumSpanningTree. g))))

(defn boruvka-minimum-spanning-tree
  "Minimum spanning tree with Boruvka's algorithm."
  [^Graph g]
  (spanning-tree-result g (.getSpanningTree (BoruvkaMinimumSpanningTree. g))))

(defn spanner
  "Greedy multiplicative `(2k-1)`-spanner as `{:edges ... :weight w}`."
  [^Graph g k]
  (let [result (.getSpanner (GreedyMultiplicativeSpanner. g (int k)))]
    (cond-> {:edges (set (map #(edge-pair g %) result))
             :weight (.getWeight result)}
      (multigraph? g) (assoc :edge-objects (set result)))))

(defn capacitated-spanning-tree
  "Esau-Williams capacitated spanning tree rooted at `root`. `demands` maps
  vertices to nonnegative demand values."
  [^Graph g root capacity demands]
  (ensure-undirected g :capacitated-spanning-tree)
  (let [^CapacitatedSpanningTreeAlgorithm$CapacitatedSpanningTree tree
        (.getCapacitatedSpanningTree
         (EsauWilliamsCapacitatedMinimumSpanningTree.
          g root (double capacity) demands (int 1000)))]
    (assoc (spanning-tree-result g tree)
           :labels (into {} (.getLabels tree)))))

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

(defn dense-edmonds-maximum-matching
  "Maximum-cardinality matching with the dense Edmonds implementation."
  [^Graph g]
  (ensure-undirected g :dense-edmonds-maximum-matching)
  (matching-result g (.getMatching (DenseEdmondsMaximumCardinalityMatching. g))))

(defn sparse-edmonds-maximum-matching
  "Maximum-cardinality matching with the sparse Edmonds implementation."
  [^Graph g]
  (ensure-undirected g :sparse-edmonds-maximum-matching)
  (matching-result g (.getMatching (SparseEdmondsMaximumCardinalityMatching. g))))

(defn hopcroft-karp-matching
  "Maximum bipartite matching with Hopcroft-Karp."
  [^Graph g part1 part2]
  (ensure-undirected g :hopcroft-karp-matching)
  (matching-result
   g (.getMatching (HopcroftKarpMaximumCardinalityBipartiteMatching.
                    g (HashSet. ^Collection part1) (HashSet. ^Collection part2)))))

(defn dulmage-mendelsohn
  "Dulmage-Mendelsohn decomposition of a bipartite graph. Set `:fine?` for the
  fine decomposition of the perfectly matched part."
  ([^Graph g part1 part2]
   (dulmage-mendelsohn g part1 part2 {}))
  ([^Graph g part1 part2 {:keys [fine?] :or {fine? false}}]
   (ensure-undirected g :dulmage-mendelsohn)
   (let [decomposition (.getDecomposition
                        (DulmageMendelsohnDecomposition.
                         g (HashSet. ^Collection part1) (HashSet. ^Collection part2))
                        (boolean fine?))]
     {:partition1-dominated (set (.getPartition1DominatedSet decomposition))
      :partition2-dominated (set (.getPartition2DominatedSet decomposition))
      :perfect-matched (mapv set (.getPerfectMatchedSets decomposition))})))

(defn assignment
  "Minimum-weight perfect bipartite matching between `part1` and `part2`."
  [^Graph g part1 part2]
  (ensure-undirected g :assignment)
  (weighted-matching-result
   g (.getMatching (KuhnMunkresMinimalWeightBipartitePerfectMatching.
                    g (HashSet. ^Collection part1) (HashSet. ^Collection part2)))))

(defn minimal-weight-perfect-matching
  "Alias for `assignment`."
  [^Graph g part1 part2]
  (assignment g part1 part2))

(defn path-growing-weighted-matching
  "Approximate maximum-weight matching with the path growing algorithm."
  [^Graph g]
  (ensure-undirected g :path-growing-weighted-matching)
  (weighted-matching-result g (.getMatching (PathGrowingWeightedMatching. g))))

(defn greedy-maximum-matching
  "Greedy maximal cardinality matching."
  [^Graph g]
  (ensure-undirected g :greedy-maximum-matching)
  (matching-result g (.getMatching (GreedyMaximumCardinalityMatching. g true))))

(defn greedy-weighted-matching
  "Greedy approximate maximum-weight matching."
  [^Graph g]
  (ensure-undirected g :greedy-weighted-matching)
  (weighted-matching-result g (.getMatching (GreedyWeightedMatching. g true))))

(defn- vertex-cover-result [^VertexCoverAlgorithm$VertexCover cover]
  {:vertices (set cover)
   :weight (.getWeight cover)})

(defn min-vertex-cover
  "Exact minimum vertex cover, optionally with a vertex-to-weight map."
  ([^Graph g]
   (ensure-undirected g :min-vertex-cover)
   (vertex-cover-result (.getVertexCover (RecursiveExactVCImpl. g))))
  ([^Graph g weights]
   (ensure-undirected g :min-vertex-cover)
   (vertex-cover-result (.getVertexCover (RecursiveExactVCImpl. g weights)))))

(defn greedy-vertex-cover
  "Greedy vertex cover, optionally with a vertex-to-weight map."
  ([^Graph g]
   (ensure-undirected g :greedy-vertex-cover)
   (vertex-cover-result (.getVertexCover (GreedyVCImpl. g))))
  ([^Graph g weights]
   (ensure-undirected g :greedy-vertex-cover)
   (vertex-cover-result (.getVertexCover (GreedyVCImpl. g weights)))))

(defn clarkson-two-approx-vertex-cover
  "Clarkson 2-approximation vertex cover, optionally weighted."
  ([^Graph g]
   (ensure-undirected g :clarkson-two-approx-vertex-cover)
   (vertex-cover-result (.getVertexCover (ClarksonTwoApproxVCImpl. g))))
  ([^Graph g weights]
   (ensure-undirected g :clarkson-two-approx-vertex-cover)
   (vertex-cover-result (.getVertexCover (ClarksonTwoApproxVCImpl. g weights)))))

(defn bar-yehuda-even-two-approx-vertex-cover
  "Bar-Yehuda-Even 2-approximation vertex cover, optionally weighted."
  ([^Graph g]
   (ensure-undirected g :bar-yehuda-even-two-approx-vertex-cover)
   (vertex-cover-result (.getVertexCover (BarYehudaEvenTwoApproxVCImpl. g))))
  ([^Graph g weights]
   (ensure-undirected g :bar-yehuda-even-two-approx-vertex-cover)
   (vertex-cover-result
    (.getVertexCover (BarYehudaEvenTwoApproxVCImpl. g weights)))))

(defn edge-based-two-approx-vertex-cover
  "Edge-based 2-approximation vertex cover."
  [^Graph g]
  (ensure-undirected g :edge-based-two-approx-vertex-cover)
  (vertex-cover-result (.getVertexCover (EdgeBasedTwoApproxVCImpl. g))))

(defn- tour-result [^GraphPath tour]
  (when tour
    {:tour (vec (.getVertexList tour))
     :weight (.getWeight tour)}))

(defn- simple-copy ^Graph [^Graph g]
  (let [weighted? (.. g getType isWeighted)
        ^Graph copy (-> (GraphTypeBuilder/undirected)
                        (.allowingMultipleEdges false)
                        (.allowingSelfLoops false)
                        (.weighted weighted?)
                        (.edgeClass (if weighted? DefaultWeightedEdge DefaultEdge))
                        (.buildGraph))]
    (doseq [v (.vertexSet g)]
      (.addVertex copy v))
    (doseq [e (.edgeSet g)]
      (let [copied (.addEdge copy (.getEdgeSource g e) (.getEdgeTarget g e))]
        (when weighted?
          (.setEdgeWeight copy copied (.getEdgeWeight g e)))))
    copy))

(defn tsp-tour
  "Hamiltonian tour as `{:tour [v ... v] :weight w}`. Methods are
  `:nearest-neighbor` (default), `:held-karp`, `:christofides`, `:greedy`,
  `:nearest-insertion`, `:random`, `:two-opt`, and `:palmer`."
  ([^Graph g]
   (tsp-tour g {}))
  ([^Graph g {:keys [method] :or {method :nearest-neighbor}}]
   (ensure-undirected g :tsp-tour)
   (let [algorithm (case method
                     :held-karp (HeldKarpTSP.)
                     :nearest-neighbor (NearestNeighborHeuristicTSP.)
                     :christofides (ChristofidesThreeHalvesApproxMetricTSP.)
                     :greedy (GreedyHeuristicTSP.)
                     :nearest-insertion (NearestInsertionHeuristicTSP.)
                     :random (RandomTourTSP.)
                     :two-opt (TwoOptHeuristicTSP.)
                     :palmer (PalmerHamiltonianCycle.)
                     (throw (ex-info "Unknown TSP method"
                                     {:cljgrapht/error :unknown-algorithm
                                      :cljgrapht/algorithm method})))
         tour-graph (if (= method :palmer) (simple-copy g) g)]
     (tour-result (.getTour algorithm tour-graph)))))

(defn maximal-cliques
  "Seq of maximal cliques of undirected graph `g`, each as a vertex set
  (Bron-Kerbosch)."
  [^Graph g]
  (ensure-undirected g :maximal-cliques)
  (map set (iterator-seq (.iterator (BronKerboschCliqueFinder. g)))))

(defn bron-kerbosch-maximal-cliques
  "Seq of maximal cliques with the basic Bron-Kerbosch algorithm."
  [^Graph g]
  (maximal-cliques g))

(defn pivot-maximal-cliques
  "Seq of maximal cliques with pivoting Bron-Kerbosch."
  [^Graph g]
  (ensure-undirected g :pivot-maximal-cliques)
  (map set (iterator-seq (.iterator (PivotBronKerboschCliqueFinder. g)))))

(defn degeneracy-maximal-cliques
  "Seq of maximal cliques with degeneracy-ordered Bron-Kerbosch."
  [^Graph g]
  (ensure-undirected g :degeneracy-maximal-cliques)
  (map set (iterator-seq (.iterator (DegeneracyBronKerboschCliqueFinder. g)))))

(defn chordal?
  "True if the undirected graph is chordal."
  [^Graph g]
  (ensure-undirected g :chordal?)
  (.isChordal (ChordalityInspector. g)))

(defn perfect-elimination-order
  "Perfect elimination order for a chordal graph, or nil when non-chordal."
  [^Graph g]
  (ensure-undirected g :perfect-elimination-order)
  (let [inspector (ChordalityInspector. g)]
    (when (.isChordal inspector)
      (vec (.getPerfectEliminationOrder inspector)))))

(defn chordal-maximum-clique
  "Maximum clique as a vertex set, or nil when the graph is non-chordal."
  [^Graph g]
  (ensure-undirected g :chordal-maximum-clique)
  (some-> (.getClique (ChordalGraphMaxCliqueFinder. g)) set))

(defn chordal-coloring
  "Optimal coloring of a chordal graph."
  [^Graph g]
  (ensure-undirected g :chordal-coloring)
  (coloring-result (ChordalGraphColoring. g)))

(defn chordal-maximum-independent-set
  "Maximum independent vertex set of a chordal graph."
  [^Graph g]
  (ensure-undirected g :chordal-maximum-independent-set)
  (some-> (.getIndependentSet (ChordalGraphIndependentSetFinder. g)) set))

(defn chordal-minimum-vertex-cover
  "Minimum vertex cover of a chordal graph, derived as the complement of a
  maximum independent set."
  [^Graph g]
  (ensure-undirected g :chordal-minimum-vertex-cover)
  (when-let [independent (chordal-maximum-independent-set g)]
    (set (remove independent (.vertexSet g)))))

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
  "Graph density from distinct non-loop endpoint pairs."
  [^Graph g]
  (let [n (.size (.vertexSet g))
        directed? (directed? g)
        m (count
           (into #{}
                 (keep (fn [e]
                         (let [u (.getEdgeSource g e)
                               v (.getEdgeTarget g e)]
                           (when (not= u v)
                             (if directed? [u v] #{u v})))))
                 (.edgeSet g)))]
    (if (< n 2)
      0.0
      (double (if directed?
                (/ m (* n (dec n)))
                (/ (* 2 m) (* n (dec n))))))))

(defn isolated-vertices
  "Set of vertices with degree zero."
  [^Graph g]
  (set (filter #(zero? (.degreeOf g %)) (.vertexSet g))))

(defn planar?
  "True if the undirected graph is planar."
  [^Graph g]
  (ensure-undirected g :planar?)
  (.isPlanar (BoyerMyrvoldPlanarityInspector. g)))

(defn planar-embedding
  "Planar rotation system as `{vertex [neighbor ...]}`; nil if nonplanar."
  [^Graph g]
  (ensure-undirected g :planar-embedding)
  (let [inspector (BoyerMyrvoldPlanarityInspector. g)]
    (when (.isPlanar inspector)
      (let [embedding (.getEmbedding inspector)]
        (into {}
              (for [v (.vertexSet g)]
                [v (mapv (fn [e]
                           (let [u (.getEdgeSource g e)
                                 w (.getEdgeTarget g e)]
                             (if (= v u) w u)))
                         (.getEdgesAround embedding v))]))))))

(defn kuratowski-subdivision
  "Kuratowski subdivision witness as vertex and edge sets; nil if planar."
  [^Graph g]
  (ensure-undirected g :kuratowski-subdivision)
  (let [inspector (BoyerMyrvoldPlanarityInspector. g)]
    (when-not (.isPlanar inspector)
      (let [^Graph witness (.getKuratowskiSubdivision inspector)]
        {:vertices (set (.vertexSet witness))
         :edges (set (map #(edge-pair witness %) (.edgeSet witness)))}))))

(defn isomorphic?
  "True if `g1` and `g2` are graph-isomorphic according to VF2. Rejects mixed
  directed/undirected graph pairs."
  [^Graph g1 ^Graph g2]
  (when (not= (directed? g1) (directed? g2))
    (throw (mixed-direction :isomorphic?)))
  (.isomorphismExists (VF2GraphIsomorphismInspector. g1 g2)))

(defn subgraph-isomorphic?
  "True if `subgraph` is isomorphic to a subgraph of `g`."
  [^Graph g ^Graph subgraph]
  (when (not= (directed? g) (directed? subgraph))
    (throw (mixed-direction :subgraph-isomorphic?)))
  (.isomorphismExists (VF2SubgraphIsomorphismInspector. g subgraph)))

(defn tree-isomorphic?
  "AHU tree-isomorphism predicate. The four-argument form fixes both roots."
  ([^Graph tree1 ^Graph tree2]
   (ensure-undirected tree1 :tree-isomorphic?)
   (ensure-undirected tree2 :tree-isomorphic?)
   (.isomorphismExists (AHUUnrootedTreeIsomorphismInspector. tree1 tree2)))
  ([^Graph tree1 root1 ^Graph tree2 root2]
   (ensure-undirected tree1 :tree-isomorphic?)
   (ensure-undirected tree2 :tree-isomorphic?)
   (.isomorphismExists
    (AHURootedTreeIsomorphismInspector. tree1 root1 tree2 root2))))

(defn color-refinement-isomorphic?
  "True when color refinement proves `g1` and `g2` isomorphic."
  [^Graph g1 ^Graph g2]
  (when (not= (directed? g1) (directed? g2))
    (throw (mixed-direction :color-refinement-isomorphic?)))
  (.isomorphismExists (ColorRefinementIsomorphismInspector. g1 g2)))

(defn tree-edit-distance
  "Zhang-Shasha edit distance between two rooted ordered trees."
  [^Graph tree1 root1 ^Graph tree2 root2]
  (.getDistance (ZhangShashaTreeEditDistance. tree1 root1 tree2 root2)))

(defn max-flow
  "Maximum `source`->`sink` flow in directed graph `g` as
  `{:value flow-value :flow {[u v] flow-on-edge, ...}}` (Push-Relabel). Edge
  weights are capacities. `:flow` does not include zero-flow edges."
  [^Graph g source sink]
  (ensure-directed g :max-flow)
  (let [^MaximumFlowAlgorithm$MaximumFlow flow (.getMaximumFlow
                                                (PushRelabelMFImpl. g) source sink)]
    (cond-> {:value (double (.getValue flow))
             :flow (into {}
                         (for [[e f] (.getFlowMap flow)
                               :let [f (double f)]
                               :when (not (zero? f))]
                           [(edge-pair g e) f]))}
      (multigraph? g)
      (assoc :edge-flow (into {}
                              (for [[e f] (.getFlowMap flow)
                                    :let [f (double f)]
                                    :when (not (zero? f))]
                                [e (double f)]))))))

(defn min-cut
  "Minimum `source`->`sink` cut in directed graph `g` as
  `{:weight w :source-partition #{...} :sink-partition #{...}}` (Push-Relabel)."
  [^Graph g source sink]
  (ensure-directed g :min-cut)
  (let [impl (PushRelabelMFImpl. g)]
    {:weight (.calculateMinCut impl source sink)
     :source-partition (set (.getSourcePartition impl))
     :sink-partition (set (.getSinkPartition impl))}))

(defn- maximum-flow-result [^Graph g algorithm source sink]
  (ensure-directed g :max-flow)
  (let [^MaximumFlowAlgorithm$MaximumFlow flow (.getMaximumFlow algorithm source sink)]
    (cond-> {:value (double (.getValue flow))
             :flow (into {}
                         (for [[e f] (.getFlowMap flow)
                               :let [f (double f)]
                               :when (not (zero? f))]
                           [(edge-pair g e) f]))}
      (multigraph? g)
      (assoc :edge-flow (into {}
                              (for [[e f] (.getFlowMap flow)
                                    :let [f (double f)]
                                    :when (not (zero? f))]
                                [e (double f)]))))))

(defn edmonds-karp-max-flow
  "Maximum flow with Edmonds-Karp."
  [^Graph g source sink]
  (maximum-flow-result g (EdmondsKarpMFImpl. g) source sink))

(defn push-relabel-max-flow
  "Maximum flow with push-relabel."
  [^Graph g source sink]
  (maximum-flow-result g (PushRelabelMFImpl. g) source sink))

(defn dinic-max-flow
  "Maximum flow with Dinic's algorithm."
  [^Graph g source sink]
  (maximum-flow-result g (DinicMFImpl. g) source sink))

(defn min-cost-flow
  "Minimum-cost flow for integer `:supplies` and `:capacities` maps.
  Edge weights are costs. Optional `:lower-bounds` defaults to zero."
  [^Graph g {:keys [supplies capacities lower-bounds]
             :or {supplies {} capacities {} lower-bounds {}}}]
  (ensure-directed g :min-cost-flow)
  (let [pair #(edge-pair g %)
        value-for (fn [values e default]
                    (or (get values e) (get values (pair e)) default))
        node-supply (reify Function
                      (apply [_ v] (int (get supplies v 0))))
        lower-bound (reify Function
                      (apply [_ e] (int (value-for lower-bounds e 0))))
        upper-bound (reify Function
                      (apply [_ e]
                        (int (value-for capacities e
                                        CapacityScalingMinimumCostFlow/CAP_INF))))
        cost (reify Function
               (apply [_ e] (double (.getEdgeWeight g e))))
        problem (MinimumCostFlowProblem$MinimumCostFlowProblemImpl.
                 g node-supply upper-bound lower-bound cost)
        ^MinimumCostFlowAlgorithm$MinimumCostFlow flow
        (.getMinimumCostFlow (CapacityScalingMinimumCostFlow.) problem)]
    (cond-> {:cost (.getCost flow)
             :flow (into {}
                         (for [[e f] (.getFlowMap flow)
                               :let [f (double f)]
                               :when (not (zero? f))]
                           [(pair e) f]))}
      (multigraph? g)
      (assoc :edge-flow (into {}
                              (for [[e f] (.getFlowMap flow)
                                    :let [f (double f)]
                                    :when (not (zero? f))]
                                [e (double f)]))))))

(defn gomory-hu-tree
  "Gomory-Hu cut tree as weighted `[u v weight]` edges."
  [^Graph g]
  (ensure-undirected g :gomory-hu-tree)
  (let [^Graph tree (.getGomoryHuTree (GusfieldGomoryHuCutTree. g))]
    {:edges (set (for [e (.edgeSet tree)]
                   [(.getEdgeSource tree e)
                    (.getEdgeTarget tree e)
                    (.getEdgeWeight tree e)]))}))

(defn minimum-cut
  "Global minimum cut of an undirected graph."
  [^Graph g]
  (ensure-undirected g :minimum-cut)
  (let [cut (StoerWagnerMinimumCut. g)]
    {:weight (.minCutWeight cut)
     :partition (set (.minCut cut))}))

(defn minimum-st-cut
  "Minimum source-to-sink cut with source and sink partitions."
  [^Graph g source sink]
  (ensure-directed g :minimum-st-cut)
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
      :random-greedy (RandomGreedyColoring. g)
      :color-refinement (ColorRefinementAlgorithm. g)
      (throw (unknown-algorithm algorithm))))))

(defn greedy-coloring
  "Greedy vertex coloring of `g` as
  `{:colors {vertex color-int, ...} :chromatic n}`."
  [^Graph g]
  (coloring g {:algorithm :greedy}))

(defn largest-degree-first-coloring
  "Greedy coloring in descending degree order."
  [^Graph g]
  (coloring g {:algorithm :largest-degree-first}))

(defn smallest-degree-last-coloring
  "Greedy coloring in smallest-degree-last order."
  [^Graph g]
  (coloring g {:algorithm :smallest-degree-last}))

(defn dsatur-coloring
  "Saturation-degree (DSATUR) vertex coloring."
  [^Graph g]
  (coloring g {:algorithm :saturation}))

(defn random-greedy-coloring
  "Greedy coloring in randomized vertex order."
  [^Graph g]
  (coloring g {:algorithm :random-greedy}))

(defn color-refinement
  "Stable equitable vertex coloring produced by color refinement."
  [^Graph g]
  (coloring g {:algorithm :color-refinement}))

(defn clustering-coefficient
  "Map of vertex -> local clustering coefficient."
  [^Graph g]
  (into {} (.getScores (ClusteringCoefficient. g))))

(defn global-clustering-coefficient
  "Global clustering coefficient of `g`."
  [^Graph g]
  (.getGlobalClusteringCoefficient (ClusteringCoefficient. g)))

(defn clustering
  "Partition vertices into clusters. Methods are `:label-propagation` (default),
  `:girvan-newman`, `:k-spanning-tree`, and `:greedy-modularity`."
  ([^Graph g]
   (clustering g {}))
  ([^Graph g {:keys [method k] :or {method :label-propagation}}]
   (ensure-undirected g :clustering)
   (when (and (#{:girvan-newman :k-spanning-tree} method) (nil? k))
     (throw (ex-info "Clustering method requires :k"
                     {:cljgrapht/error :missing-option
                      :cljgrapht/option :k
                      :cljgrapht/algorithm method})))
   (let [algorithm (case method
                     :label-propagation (LabelPropagationClustering. g)
                     :greedy-modularity (GreedyModularityAlgorithm. g)
                     :girvan-newman (GirvanNewmanClustering. g (int k))
                     :k-spanning-tree (KSpanningTreeClustering. g (int k))
                     (throw (ex-info "Unknown clustering method"
                                     {:cljgrapht/error :unknown-algorithm
                                      :cljgrapht/algorithm method})))]
     (mapv set (.getClusters (.getClustering algorithm))))))

(defn modularity
  "Measure the modularity of an undirected graph partition."
  [^Graph g clusters]
  (ensure-undirected g :modularity)
  (.modularity (UndirectedModularityMeasurer. g)
               (java.util.ArrayList. (map set clusters))))

(defn layout-2d
  "Lay out vertices and return a map of vertex -> `[x y]` coordinates.
  Algorithms are `:circular` (default), `:fr`, and `:random`."
  ([^Graph g] (layout-2d g {}))
  ([^Graph g {:keys [algorithm width height seed]
              :or {algorithm :circular width 1.0 height 1.0}}]
   (let [^Box2D area (Box2D/of (double width) (double height))
         ^MapLayoutModel2D model (MapLayoutModel2D. area)
         algorithm (case algorithm
                     :circular (CircularLayoutAlgorithm2D.)
                     :fr (FRLayoutAlgorithm2D.)
                     :random (if (some? seed)
                               (RandomLayoutAlgorithm2D. (long seed))
                               (RandomLayoutAlgorithm2D.))
                     (throw (ex-info "Unknown layout algorithm"
                                     {:cljgrapht/error :unknown-algorithm
                                      :cljgrapht/algorithm algorithm})))]
     (.layout ^LayoutAlgorithm2D algorithm g model)
     (into {}
           (map (fn [entry]
                  (let [v (.getKey entry) ^Point2D p (.getValue entry)]
                    [v [(.getX p) (.getY p)]])))
           (.collect model)))))

(defn coreness
  "Map of vertex -> core number."
  [^Graph g]
  (into {} (.getScores (Coreness. g))))

(defn betweenness-centrality
  "Map of vertex -> betweenness centrality score."
  [^Graph g]
  (into {} (.getScores (BetweennessCentrality. g))))

(defn edge-betweenness-centrality
  "Map edge representations to edge betweenness centrality scores. Multigraphs
  use their edge objects as keys to preserve parallel-edge identity."
  [^Graph g]
  (into {}
        (map (fn [e]
               [(if (multigraph? g) e (edge-pair g e))
                (get (.getScores (EdgeBetweennessCentrality. g)) e)]))
        (.edgeSet g)))

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

(defn diameter
  "Maximum shortest-path distance between any two vertices."
  [^Graph g]
  (.getDiameter (GraphMeasurer. g)))

(defn radius
  "Minimum vertex eccentricity."
  [^Graph g]
  (.getRadius (GraphMeasurer. g)))

(defn graph-center
  "Set of vertices whose eccentricity equals the graph radius."
  [^Graph g]
  (set (.getGraphCenter (GraphMeasurer. g))))

(defn graph-periphery
  "Set of vertices whose eccentricity equals the graph diameter."
  [^Graph g]
  (set (.getGraphPeriphery (GraphMeasurer. g))))

(defn pseudo-periphery
  "Set of vertices whose neighbors have no greater eccentricity."
  [^Graph g]
  (set (.getGraphPseudoPeriphery (GraphMeasurer. g))))

(defn vertex-eccentricities
  "Map of vertex -> maximum shortest-path distance to another vertex."
  [^Graph g]
  (into {} (.getVertexEccentricityMap (GraphMeasurer. g))))

(defn- girth-from [^Graph g root]
  (loop [queue (conj clojure.lang.PersistentQueue/EMPTY root)
         distances {root 0}
         parent-edges {}
         best Long/MAX_VALUE]
    (if (empty? queue)
      best
      (let [v (peek queue)
            queue (pop queue)
            distance (distances v)
            [queue distances parent-edges best]
            (reduce
             (fn [[q ds ps shortest] e]
               (let [source (.getEdgeSource g e)
                     target (.getEdgeTarget g e)
                     neighbor (if (= v source) target source)]
                 (cond
                   (not (contains? ds neighbor))
                   [(conj q neighbor)
                    (assoc ds neighbor (inc distance))
                    (assoc ps neighbor e)
                    shortest]

                   (not= e (get ps v))
                   [q ds ps (min shortest (inc (+ distance (ds neighbor))))]

                   :else [q ds ps shortest])))
             [queue distances parent-edges best]
             (.edgesOf g v))]
        (recur queue distances parent-edges best)))))

(defn girth
  "Length of the shortest cycle in an undirected graph, or nil when acyclic."
  [^Graph g]
  (ensure-undirected g :girth)
  (let [length (reduce min Long/MAX_VALUE
                       (map #(girth-from g %) (.vertexSet g)))]
    (when-not (= Long/MAX_VALUE length)
      length)))
