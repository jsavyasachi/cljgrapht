(ns cljgrapht.gen
  "Graph generators returning new `cljgrapht.core` graphs."
  (:require [cljgrapht.core :as core])
  (:import (java.util ArrayList HashMap Random)
           (java.util.concurrent.atomic AtomicInteger)
           (java.util.function Supplier)
           (org.jgrapht Graph)
           (org.jgrapht.generate BarabasiAlbertForestGenerator
                                  BarabasiAlbertGraphGenerator
                                  ComplementGraphGenerator
                                  CompleteBipartiteGraphGenerator
                                  CompleteGraphGenerator
                                  DirectedScaleFreeGraphGenerator
                                  EmptyGraphGenerator
                                  GeneralizedPetersenGraphGenerator
                                  GnmRandomBipartiteGraphGenerator
                                  GnmRandomGraphGenerator
                                  GnpRandomBipartiteGraphGenerator
                                  GnpRandomGraphGenerator
                                  GraphGenerator
                                  GridGraphGenerator
                                  HyperCubeGraphGenerator
                                  KleinbergSmallWorldGraphGenerator
                                  LinearGraphGenerator
                                  LinearizedChordDiagramGraphGenerator
                                  PlantedPartitionGraphGenerator
                                  PruferTreeGenerator
                                  RandomRegularGraphGenerator
                                  RingGraphGenerator
                                  ScaleFreeGraphGenerator
                                  SimpleWeightedBipartiteGraphMatrixGenerator
                                  SimpleWeightedGraphMatrixGenerator
                                  StarGraphGenerator
                                  WattsStrogatzGraphGenerator
                                  WheelGraphGenerator
                                  WindmillGraphsGenerator
                                  WindmillGraphsGenerator$Mode)
           (org.jgrapht.graph AbstractBaseGraph DefaultEdge)
           (org.jgrapht.graph.builder GraphTypeBuilder)))

(defn- int-supplier ^Supplier []
  (let [counter (AtomicInteger. 0)]
    (reify Supplier
      (get [_] (.getAndIncrement counter)))))

(defn- graph-with-supplier
  (^Graph []
   (graph-with-supplier {}))
  (^Graph [{:keys [directed? weighted? multiple-edges? simple?]}]
   (let [^AbstractBaseGraph g
         (if (or multiple-edges? simple?)
           (-> ^GraphTypeBuilder (if directed?
                                   (GraphTypeBuilder/directed)
                                   (GraphTypeBuilder/undirected))
               (.allowingMultipleEdges (boolean multiple-edges?))
               (.allowingSelfLoops (not simple?))
               (.weighted false)
               (.edgeClass DefaultEdge)
               (.buildGraph))
           (cond
             (and directed? weighted?) (core/weighted-digraph)
             directed? (core/digraph)
             weighted? (core/weighted-graph)
             :else (core/graph)))]
     (.setVertexSupplier g (int-supplier))
     g)))

(defn- generate-into ^Graph [^GraphGenerator generator ^Graph g]
  (.generateGraph generator g (HashMap.))
  g)

(defn- generate
  (^Graph [^GraphGenerator generator]
   (generate generator {}))
  (^Graph [^GraphGenerator generator opts]
   (generate-into generator (graph-with-supplier opts))))

(defn- double-matrix [rows]
  (into-array (class (double-array 0)) (map double-array rows)))

(defn complete-graph
  "A new undirected complete graph with integer vertices 0..n-1."
  ^Graph [n]
  (generate (CompleteGraphGenerator. (int n))))

(defn ring-graph
  "A new undirected ring graph with integer vertices 0..n-1."
  ^Graph [n]
  (generate (RingGraphGenerator. (int n))))

(defn star-graph
  "A new undirected star graph with integer vertices 0..n-1."
  ^Graph [n]
  (generate (StarGraphGenerator. (int n))))

(defn grid-graph
  "A new undirected rows-by-cols grid graph with integer vertices 0..n-1."
  ^Graph [rows cols]
  (generate (GridGraphGenerator. (int rows) (int cols))))

(defn complete-bipartite-graph
  "A new undirected complete bipartite graph with partition sizes n1 and n2."
  ^Graph [n1 n2]
  (generate (CompleteBipartiteGraphGenerator. (int n1) (int n2))))

(defn linear-graph
  "A new undirected path graph with integer vertices 0..n-1."
  ^Graph [n]
  (generate (LinearGraphGenerator. (int n))))

(defn wheel-graph
  "A new undirected wheel graph with n total vertices."
  (^Graph [n]
   (generate (WheelGraphGenerator. (int n))))
  (^Graph [n {:keys [directed? inward-spokes?]
              :or {inward-spokes? true}}]
   (generate (WheelGraphGenerator. (int n) (boolean inward-spokes?))
             {:directed? directed?})))

(defn hypercube-graph
  "A new undirected hypercube graph of dimension n."
  ^Graph [n]
  (generate (HyperCubeGraphGenerator. (int n))))

(defn empty-graph
  "A new undirected graph with n vertices and no edges."
  ^Graph [n]
  (generate (EmptyGraphGenerator. (int n))))

(defn generalized-petersen-graph
  "A new undirected generalized Petersen graph GP(n,k)."
  ^Graph [n k]
  (generate (GeneralizedPetersenGraphGenerator. (int n) (int k))))

(defn windmill-graph
  "A new windmill graph with m copies of K_n or C_n sharing one vertex."
  ^Graph [mode m n]
  (let [generator-mode (case mode
                         :windmill WindmillGraphsGenerator$Mode/WINDMILL
                         :dutch-windmill WindmillGraphsGenerator$Mode/DUTCHWINDMILL
                         (throw (IllegalArgumentException.
                                 (str "Unknown windmill mode: " mode))))]
    (generate (WindmillGraphsGenerator. generator-mode (int m) (int n)))))

(defn complement-graph
  "A new complement of graph gr, optionally including missing self-loops."
  (^Graph [^Graph gr]
   (complement-graph gr {}))
  (^Graph [^Graph gr {:keys [self-loops?]}]
   (let [target (graph-with-supplier
                 {:directed? (.. gr getType isDirected)})]
     (generate-into (ComplementGraphGenerator. gr (boolean self-loops?)) target))))

(defn gnp-random-graph
  "A new undirected Erdos-Renyi G(n,p) graph."
  (^Graph [n p]
   (generate (GnpRandomGraphGenerator. (int n) (double p))))
  (^Graph [n p {:keys [seed directed? self-loops?]}]
   (let [generator (if (some? seed)
                     (GnpRandomGraphGenerator. (int n) (double p) (long seed)
                                               (boolean self-loops?))
                     (GnpRandomGraphGenerator. (int n) (double p) (Random.)
                                               (boolean self-loops?)))]
     (generate generator {:directed? directed?}))))

(defn gnm-random-graph
  "A new graph sampled uniformly from graphs with n vertices and m edges."
  (^Graph [n m]
   (generate (GnmRandomGraphGenerator. (int n) (int m))))
  (^Graph [n m {:keys [seed directed? self-loops? multiple-edges?]}]
   (let [rng (if (some? seed) (Random. (long seed)) (Random.))]
     (generate (GnmRandomGraphGenerator.
                (int n) (int m) rng (boolean self-loops?)
                (boolean multiple-edges?))
               {:directed? directed? :multiple-edges? multiple-edges?}))))

(defn gnp-random-bipartite-graph
  "A new random bipartite graph with partition sizes n1 and n2 and edge probability p."
  (^Graph [n1 n2 p]
   (generate (GnpRandomBipartiteGraphGenerator.
              (int n1) (int n2) (double p))))
  (^Graph [n1 n2 p {:keys [seed directed?]}]
   (let [generator (if (some? seed)
                     (GnpRandomBipartiteGraphGenerator.
                      (int n1) (int n2) (double p) (long seed))
                     (GnpRandomBipartiteGraphGenerator.
                      (int n1) (int n2) (double p)))]
     (generate generator {:directed? directed?}))))

(defn gnm-random-bipartite-graph
  "A new random bipartite graph with partition sizes n1 and n2 and m edges."
  (^Graph [n1 n2 m]
   (generate (GnmRandomBipartiteGraphGenerator.
              (int n1) (int n2) (int m))))
  (^Graph [n1 n2 m {:keys [seed directed?]}]
   (let [generator (if (some? seed)
                     (GnmRandomBipartiteGraphGenerator.
                      (int n1) (int n2) (int m) (long seed))
                     (GnmRandomBipartiteGraphGenerator.
                      (int n1) (int n2) (int m)))]
     (generate generator {:directed? directed?}))))

(defn barabasi-albert-graph
  "A new undirected Barabasi-Albert graph with initial `m0`, `m`, and total `n`."
  (^Graph [m0 m n]
   (generate (BarabasiAlbertGraphGenerator. (int m0) (int m) (int n))))
  (^Graph [m0 m n {:keys [seed]}]
   (if (some? seed)
     (generate (BarabasiAlbertGraphGenerator. (int m0) (int m) (int n) (long seed)))
     (barabasi-albert-graph m0 m n))))

(defn barabasi-albert-forest
  "A new Barabasi-Albert forest with t trees and n total vertices."
  (^Graph [t n]
   (generate (BarabasiAlbertForestGenerator. (int t) (int n))))
  (^Graph [t n {:keys [seed]}]
   (if (some? seed)
     (generate (BarabasiAlbertForestGenerator. (int t) (int n) (long seed)))
     (barabasi-albert-forest t n))))

(defn kleinberg-small-world-graph
  "A new Kleinberg n-by-n lattice with local radius p and q long-range contacts."
  (^Graph [n p q r]
   (generate (KleinbergSmallWorldGraphGenerator.
              (int n) (int p) (int q) (int r))))
  (^Graph [n p q r {:keys [seed directed?]}]
   (let [generator (if (some? seed)
                     (KleinbergSmallWorldGraphGenerator.
                      (int n) (int p) (int q) (int r) (long seed))
                     (KleinbergSmallWorldGraphGenerator.
                      (int n) (int p) (int q) (int r)))]
     (generate generator {:directed? directed?}))))

(defn scale-free-graph
  "A new scale-free graph with n vertices."
  (^Graph [n]
   (generate (ScaleFreeGraphGenerator. (int n))))
  (^Graph [n {:keys [seed directed?]}]
   (let [generator (if (some? seed)
                     (ScaleFreeGraphGenerator. (int n) (long seed))
                     (ScaleFreeGraphGenerator. (int n)))]
     (generate generator {:directed? directed?}))))

(defn random-regular-graph
  "A new random d-regular graph with n vertices."
  (^Graph [n d]
   (generate (RandomRegularGraphGenerator. (int n) (int d)) {:simple? true}))
  (^Graph [n d {:keys [seed]}]
   (if (some? seed)
     (generate (RandomRegularGraphGenerator. (int n) (int d) (long seed))
               {:simple? true})
     (random-regular-graph n d))))

(defn prufer-tree
  "A new tree from a vertex count or an explicit Prüfer sequence."
  (^Graph [n-or-sequence]
   (if (sequential? n-or-sequence)
     (generate (PruferTreeGenerator. (int-array n-or-sequence)))
     (generate (PruferTreeGenerator. (int n-or-sequence)))))
  (^Graph [n {:keys [seed]}]
   (if (some? seed)
     (generate (PruferTreeGenerator. (int n) (long seed)))
     (prufer-tree n))))

(defn planted-partition-graph
  "A new planted partition graph with l groups of k vertices."
  (^Graph [l k p q]
   (generate (PlantedPartitionGraphGenerator.
              (int l) (int k) (double p) (double q))))
  (^Graph [l k p q {:keys [seed directed? self-loops?]}]
   (let [rng (if (some? seed) (Random. (long seed)) (Random.))]
     (generate (PlantedPartitionGraphGenerator.
                (int l) (int k) (double p) (double q) rng
                (boolean self-loops?))
               {:directed? directed?}))))

(defn directed-scale-free-graph
  "A new directed scale-free graph controlled by edge and vertex targets."
  (^Graph [alpha gamma delta-in delta-out target-edges target-nodes]
   (directed-scale-free-graph alpha gamma delta-in delta-out
                              target-edges target-nodes {}))
  (^Graph [alpha gamma delta-in delta-out target-edges target-nodes
           {:keys [seed multiple-edges? self-loops?]
            :or {multiple-edges? true self-loops? true}}]
   (let [rng (if (some? seed) (Random. (long seed)) (Random.))
         generator (DirectedScaleFreeGraphGenerator.
                    (float alpha) (float gamma) (float delta-in) (float delta-out)
                    (int target-edges) (int target-nodes) rng
                    (boolean multiple-edges?) (boolean self-loops?))]
     (generate generator
               {:directed? true :multiple-edges? multiple-edges?}))))

(defn linearized-chord-diagram-graph
  "A new linearized chord diagram multigraph with n vertices and m edges per vertex."
  (^Graph [n m]
   (generate (LinearizedChordDiagramGraphGenerator. (int n) (int m))
             {:multiple-edges? true}))
  (^Graph [n m {:keys [seed]}]
   (let [generator (if (some? seed)
                     (LinearizedChordDiagramGraphGenerator.
                      (int n) (int m) (long seed))
                     (LinearizedChordDiagramGraphGenerator. (int n) (int m)))]
     (generate generator {:multiple-edges? true}))))

(defn weighted-matrix-graph
  "A new weighted digraph from a square adjacency weight matrix."
  ^Graph [weights]
  (let [vertices (ArrayList. ^java.util.Collection
                             (mapv int (range (count weights))))
        generator (doto (SimpleWeightedGraphMatrixGenerator.)
                    (.vertices vertices)
                    (.weights (double-matrix weights)))]
    (generate generator {:directed? true :weighted? true})))

(defn weighted-bipartite-matrix-graph
  "A new weighted bipartite graph from partition sizes and a weight matrix."
  (^Graph [n1 n2 weights]
   (weighted-bipartite-matrix-graph n1 n2 weights {}))
  (^Graph [n1 n2 weights {:keys [directed?]}]
   (let [first-partition (ArrayList. ^java.util.Collection
                                     (mapv int (range n1)))
         second-partition (ArrayList. ^java.util.Collection
                                      (mapv int (range n1 (+ n1 n2))))
         generator (doto (SimpleWeightedBipartiteGraphMatrixGenerator.)
                     (.first first-partition)
                     (.second second-partition)
                     (.weights (double-matrix weights)))]
     (generate generator {:directed? directed? :weighted? true}))))

(defn watts-strogatz-graph
  "A new undirected Watts-Strogatz graph with n vertices, degree k, and rewiring p."
  (^Graph [n k p]
   (generate (WattsStrogatzGraphGenerator. (int n) (int k) (double p))))
  (^Graph [n k p {:keys [seed]}]
   (if (some? seed)
     (generate (WattsStrogatzGraphGenerator. (int n) (int k) (double p) (long seed)))
     (watts-strogatz-graph n k p))))
