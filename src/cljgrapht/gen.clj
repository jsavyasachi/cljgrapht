(ns cljgrapht.gen
  "Graph generators returning new `cljgrapht.core` graphs."
  (:require [cljgrapht.core :as core])
  (:import (java.util HashMap)
           (java.util.concurrent.atomic AtomicInteger)
           (java.util.function Supplier)
           (org.jgrapht Graph)
           (org.jgrapht.generate BarabasiAlbertGraphGenerator
                                  CompleteGraphGenerator
                                  GnpRandomGraphGenerator
                                  GraphGenerator
                                  GridGraphGenerator
                                  RingGraphGenerator
                                  StarGraphGenerator
                                  WattsStrogatzGraphGenerator)
           (org.jgrapht.graph AbstractBaseGraph)))

(defn- int-supplier ^Supplier []
  (let [counter (AtomicInteger. 0)]
    (reify Supplier
      (get [_] (.getAndIncrement counter)))))

(defn- graph-with-supplier ^Graph []
  (let [^AbstractBaseGraph g (core/graph)]
    (.setVertexSupplier g (int-supplier))
    g))

(defn- generate ^Graph [^GraphGenerator generator]
  (let [g (graph-with-supplier)]
    (.generateGraph generator g (HashMap.))
    g))

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
