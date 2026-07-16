(ns cljgrapht.gen
  "Graph generators returning new `cljgrapht.core` graphs."
  (:require [cljgrapht.core :as core])
  (:import (java.util HashMap)
           (java.util.concurrent.atomic AtomicInteger)
           (java.util.function Supplier)
           (org.jgrapht Graph)
           (org.jgrapht.generate BarabasiAlbertGraphGenerator
                                  ComplementGraphGenerator
                                  CompleteBipartiteGraphGenerator
                                  CompleteGraphGenerator
                                  EmptyGraphGenerator
                                  GeneralizedPetersenGraphGenerator
                                  GnpRandomGraphGenerator
                                  GraphGenerator
                                  GridGraphGenerator
                                  HyperCubeGraphGenerator
                                  LinearGraphGenerator
                                  RingGraphGenerator
                                  StarGraphGenerator
                                  WattsStrogatzGraphGenerator
                                  WheelGraphGenerator
                                  WindmillGraphsGenerator
                                  WindmillGraphsGenerator$Mode)
           (org.jgrapht.graph AbstractBaseGraph)))

(defn- int-supplier ^Supplier []
  (let [counter (AtomicInteger. 0)]
    (reify Supplier
      (get [_] (.getAndIncrement counter)))))

(defn- graph-with-supplier
  (^Graph []
   (graph-with-supplier {}))
  (^Graph [{:keys [directed? weighted?]}]
   (let [^AbstractBaseGraph g (cond
                                (and directed? weighted?) (core/weighted-digraph)
                                directed? (core/digraph)
                                weighted? (core/weighted-graph)
                                :else (core/graph))]
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
  (^Graph [n p {:keys [seed]}]
   (if (some? seed)
     (generate (GnpRandomGraphGenerator. (int n) (double p) (long seed)))
     (gnp-random-graph n p))))

(defn barabasi-albert-graph
  "A new undirected Barabasi-Albert graph with initial `m0`, `m`, and total `n`."
  (^Graph [m0 m n]
   (generate (BarabasiAlbertGraphGenerator. (int m0) (int m) (int n))))
  (^Graph [m0 m n {:keys [seed]}]
   (if (some? seed)
     (generate (BarabasiAlbertGraphGenerator. (int m0) (int m) (int n) (long seed)))
     (barabasi-albert-graph m0 m n))))

(defn watts-strogatz-graph
  "A new undirected Watts-Strogatz graph with n vertices, degree k, and rewiring p."
  (^Graph [n k p]
   (generate (WattsStrogatzGraphGenerator. (int n) (int k) (double p))))
  (^Graph [n k p {:keys [seed]}]
   (if (some? seed)
     (generate (WattsStrogatzGraphGenerator. (int n) (int k) (double p) (long seed)))
     (watts-strogatz-graph n k p))))
