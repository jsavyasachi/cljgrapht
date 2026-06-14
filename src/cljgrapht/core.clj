(ns cljgrapht.core
  "Idiomatic construction and inspection of graphs backed by JGraphT.

  Vertices are arbitrary Clojure values (keywords, strings, numbers, maps).
  Graphs are JGraphT's native mutable objects: constructors and mutators return
  the same graph for threading, but they mutate in place (this is a performance
  wrapper, not a persistent data structure). Algorithms live in `cljgrapht.algo`
  and return plain Clojure data."
  (:import (org.jgrapht Graph Graphs)
           (org.jgrapht.graph DefaultEdge DefaultWeightedEdge)
           (org.jgrapht.graph.builder GraphTypeBuilder)))

(defn- build ^Graph [directed? weighted? edges]
  (let [^GraphTypeBuilder b (if directed?
                              (GraphTypeBuilder/directed)
                              (GraphTypeBuilder/undirected))
        g (-> b
              (.allowingMultipleEdges false)
              (.allowingSelfLoops true)
              (.weighted (boolean weighted?))
              (.edgeClass (if weighted? DefaultWeightedEdge DefaultEdge))
              (.buildGraph))]
    (doseq [e edges]
      (let [[u v w] e]
        (.addVertex g u)
        (.addVertex g v)
        (.addEdge g u v)
        (when (and weighted? (some? w))
          (.setEdgeWeight g u v (double w)))))
    g))

(defn graph
  "An undirected graph. Optional `edges` is a seq of [u v] pairs."
  (^Graph [] (build false false nil))
  (^Graph [edges] (build false false edges)))

(defn digraph
  "A directed graph. Optional `edges` is a seq of [u v] pairs."
  (^Graph [] (build true false nil))
  (^Graph [edges] (build true false edges)))

(defn weighted-graph
  "An undirected weighted graph. Optional `edges` is a seq of [u v w] triples."
  (^Graph [] (build false true nil))
  (^Graph [edges] (build false true edges)))

(defn weighted-digraph
  "A directed weighted graph. Optional `edges` is a seq of [u v w] triples."
  (^Graph [] (build true true nil))
  (^Graph [edges] (build true true edges)))

(defn add-vertex
  "Add vertex `v` to `g`, returning `g`."
  ^Graph [^Graph g v]
  (.addVertex g v)
  g)

(defn add-edge
  "Add an edge `u -> v` (optionally with weight `w`) to `g`, adding the vertices
  if absent. Returns `g`."
  (^Graph [^Graph g u v]
   (.addVertex g u)
   (.addVertex g v)
   (.addEdge g u v)
   g)
  (^Graph [^Graph g u v w]
   (add-edge g u v)
   (.setEdgeWeight g u v (double w))
   g))

(defn- weighted? [^Graph g]
  (.. g getType isWeighted))

(defn vertices
  "The set of vertices in `g`."
  [^Graph g]
  (set (.vertexSet g)))

(defn edges
  "A seq of edges in `g`: [u v] pairs, or [u v w] triples when `g` is weighted."
  [^Graph g]
  (let [w? (weighted? g)]
    (map (fn [e]
           (let [u (.getEdgeSource g e)
                 v (.getEdgeTarget g e)]
             (if w?
               [u v (.getEdgeWeight g e)]
               [u v])))
         (.edgeSet g))))

(defn neighbors
  "Vertices adjacent to `v` in `g` (direction-agnostic)."
  [^Graph g v]
  (Graphs/neighborListOf g v))

(defn successors
  "Vertices reachable from `v` by an outgoing edge in `g`."
  [^Graph g v]
  (Graphs/successorListOf g v))

(defn predecessors
  "Vertices with an edge into `v` in `g`."
  [^Graph g v]
  (Graphs/predecessorListOf g v))

(defn weight
  "The weight of the edge `u -> v` in `g`."
  [^Graph g u v]
  (.getEdgeWeight g (.getEdge g u v)))
