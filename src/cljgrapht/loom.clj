(ns cljgrapht.loom
  "Loom protocol interop for raw JGraphT graphs.

  This namespace needs a loom artifact (net.clojars.savya/loom or aysylu/loom)
  on the classpath. cljgrapht does not include it as a dependency.

  EditableGraph operations mutate the underlying JGraphT graph in place and
  return the same instance. The return values from the add and remove
  operations are the same graph, not new copies. Loom's persistent graph
  records behave differently."
  (:require [cljgrapht.core :as core]
            [loom.graph :as loom])
  (:import (org.jgrapht Graph GraphType Graphs)))

(defn- directed? [^Graph g]
  (.. g getType isDirected))

(defn- weighted? [^Graph g]
  (.. g getType isWeighted))

(defn- not-directed [^Graph g operation]
  (ex-info "JGraphT graph is not directed"
           {:cljgrapht/error :not-directed
            :cljgrapht/operation operation
            :cljgrapht/graph-type (.getType g)}))

(defn- not-weighted [^Graph g operation]
  (ex-info "JGraphT graph is not weighted"
           {:cljgrapht/error :not-weighted
            :cljgrapht/operation operation
            :cljgrapht/graph-type (.getType g)}))

(defn- ensure-directed [^Graph g operation]
  (when-not (directed? g)
    (throw (not-directed g operation))))

(defn- edge-pair [^Graph g e]
  [(.getEdgeSource g e) (.getEdgeTarget g e)])

(defn- outgoing-edge-pair [^Graph g node e]
  (if (directed? g)
    [node (.getEdgeTarget g e)]
    [node (Graphs/getOppositeVertex g e node)]))

(defn- incoming-edge-pair [^Graph g node e]
  [(.getEdgeSource g e) node])

(defn- edge-weight [^Graph g u v]
  (when-let [e (.getEdge g u v)]
    (.getEdgeWeight g e)))

(defn- add-edge* ^Graph [^Graph g edge]
  (let [[u v w] edge]
    (.addVertex g u)
    (.addVertex g v)
    (let [added (.addEdge g u v)]
      (when (some? w)
        (when-not (weighted? g)
          (throw (not-weighted g :add-edges*)))
        ;; Set the weight on the edge object so that parallel edges keep their
        ;; own weight. `addEdge` returns nil when the edge is already there and
        ;; the graph rejects parallel edges; the vertex pair is unambiguous
        ;; then.
        (if (some? added)
          (.setEdgeWeight g added (double w))
          (.setEdgeWeight g u v (double w)))))
    g))

(defn- transpose-graph ^Graph [^Graph g]
  (ensure-directed g :transpose)
  (let [weighted? (weighted? g)
        ^GraphType gtype (.getType g)
        tg (core/make-graph
            {:directed? true
             :weighted? weighted?
             :allow-multiple-edges? (.isAllowingMultipleEdges gtype)
             :allow-self-loops? (.isAllowingSelfLoops gtype)})]
    (doseq [v (.vertexSet g)]
      (.addVertex tg v))
    (doseq [e (.edgeSet g)]
      (let [u (.getEdgeSource g e)
            v (.getEdgeTarget g e)
            added (.addEdge tg v u)]
        (when (and weighted? (some? added))
          (.setEdgeWeight tg added (.getEdgeWeight g e)))))
    tg))

(extend-type Graph
  loom/Graph
  (nodes [g]
    (.vertexSet g))

  (edges [g]
    (map (fn [e] (edge-pair g e)) (.edgeSet g)))

  (has-node? [g node]
    (.containsVertex g node))

  (has-edge? [g n1 n2]
    (.containsEdge g n1 n2))

  (successors* [g node]
    (Graphs/successorListOf g node))

  (out-degree [g node]
    (.outDegreeOf g node))

  (out-edges [g node]
    (map (fn [e] (outgoing-edge-pair g node e)) (.outgoingEdgesOf g node)))

  loom/Digraph
  (predecessors* [g node]
    (ensure-directed g :predecessors*)
    (Graphs/predecessorListOf g node))

  (in-degree [g node]
    (ensure-directed g :in-degree)
    (.inDegreeOf g node))

  (in-edges [g node]
    (ensure-directed g :in-edges)
    (map (fn [e] (incoming-edge-pair g node e)) (.incomingEdgesOf g node)))

  (transpose [g]
    (transpose-graph g))

  loom/WeightedGraph
  (weight*
    ([g e]
     (edge-weight g (loom/src e) (loom/dest e)))
    ([g n1 n2]
     (edge-weight g n1 n2)))

  loom/EditableGraph
  (add-nodes* [g nodes]
    (doseq [node nodes]
      (.addVertex g node))
    g)

  (add-edges* [g edges]
    (doseq [edge edges]
      (add-edge* g edge))
    g)

  (remove-nodes* [g nodes]
    (.removeAllVertices g nodes)
    g)

  (remove-edges* [g edges]
    (doseq [[u v] edges]
      (.removeEdge g u v))
    g)

  (remove-all [g]
    (.removeAllVertices g (vec (.vertexSet g)))
    g))
