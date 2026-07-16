(ns cljgrapht.core
  "Idiomatic construction and inspection of graphs backed by JGraphT.

  Vertices are arbitrary Clojure values (keywords, strings, numbers, maps).
  Graphs are JGraphT's native mutable objects: constructors and mutators return
  the same graph for threading, but they mutate in place (this is a performance
  wrapper, not a persistent data structure). Algorithms live in `cljgrapht.algo`
  and return plain Clojure data."
  (:import (java.util HashMap)
           (java.util.function Supplier)
           (org.jgrapht Graph Graphs GraphType)
           (org.jgrapht.graph AsSubgraph
                               AsUndirectedGraph
                               AsUnmodifiableGraph
                               AsUnweightedGraph
                               AsWeightedGraph
                               DefaultEdge
                               DefaultWeightedEdge
                               EdgeReversedGraph)
           (org.jgrapht.graph.builder GraphTypeBuilder)))

(defn- as-supplier ^Supplier [supplier]
  (if (instance? Supplier supplier)
    supplier
    (reify Supplier
      (get [_] (supplier)))))

(defn- build ^Graph
  [{:keys [directed? weighted? allow-multiple-edges? allow-self-loops?
           vertex-supplier edge-supplier edge-class edges]
    :or {directed? false
         weighted? false
         allow-multiple-edges? false
         allow-self-loops? true}}]
  (let [^GraphTypeBuilder b (if directed?
                              (GraphTypeBuilder/directed)
                              (GraphTypeBuilder/undirected))]
    (.allowingMultipleEdges b (boolean allow-multiple-edges?))
    (.allowingSelfLoops b (boolean allow-self-loops?))
    (.weighted b (boolean weighted?))
    (when vertex-supplier
      (.vertexSupplier b (as-supplier vertex-supplier)))
    (if edge-supplier
      (.edgeSupplier b (as-supplier edge-supplier))
      (.edgeClass b (or edge-class
                        (if weighted? DefaultWeightedEdge DefaultEdge))))
    (let [g (.buildGraph b)]
      (doseq [e edges]
        (let [[u v w] e]
          (.addVertex g u)
          (.addVertex g v)
          (let [edge (.addEdge g u v)]
            (when (and weighted? (some? w))
              (.setEdgeWeight g edge (double w))))))
      g)))

(defn make-graph
  "Build a graph from options controlling its type, suppliers, and initial edges."
  ^Graph [opts]
  (build opts))

(defn graph
  "An undirected graph. Optional `edges` is a seq of [u v] pairs."
  (^Graph [] (build {}))
  (^Graph [edges] (build {:edges edges})))

(defn digraph
  "A directed graph. Optional `edges` is a seq of [u v] pairs."
  (^Graph [] (build {:directed? true}))
  (^Graph [edges] (build {:directed? true :edges edges})))

(defn weighted-graph
  "An undirected weighted graph. Optional `edges` is a seq of [u v w] triples."
  (^Graph [] (build {:weighted? true}))
  (^Graph [edges] (build {:weighted? true :edges edges})))

(defn weighted-digraph
  "A directed weighted graph. Optional `edges` is a seq of [u v w] triples."
  (^Graph [] (build {:directed? true :weighted? true}))
  (^Graph [edges] (build {:directed? true :weighted? true :edges edges})))

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

(defn weighted?
  "Whether `g` supports edge weights."
  [^Graph g]
  (.. g getType isWeighted))

(defn directed?
  "Whether `g` is directed."
  [^Graph g]
  (.. g getType isDirected))

(defn- render-edge [^Graph g edge]
  (let [u (.getEdgeSource g edge)
        v (.getEdgeTarget g edge)]
    (if (weighted? g)
      [u v (.getEdgeWeight g edge)]
      [u v])))

(defn vertices
  "The set of vertices in `g`."
  [^Graph g]
  (set (.vertexSet g)))

(defn edges
  "A seq of edges in `g`: [u v] pairs, or [u v w] triples when `g` is weighted."
  [^Graph g]
  (map #(render-edge g %) (.edgeSet g)))

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

(defn remove-vertex
  "Remove vertex `v` and its incident edges from `g`, returning `g`."
  ^Graph [^Graph g v]
  (.removeVertex g v)
  g)

(defn remove-edge
  "Remove an edge from `g` by endpoints or by edge object, returning `g`."
  (^Graph [^Graph g edge]
   (.removeEdge g edge)
   g)
  (^Graph [^Graph g u v]
   (.removeEdge g u v)
   g))

(defn contains-vertex?
  "Whether `g` contains vertex `v`."
  [^Graph g v]
  (.containsVertex g v))

(defn contains-edge?
  "Whether `g` contains an edge from `u` to `v`."
  [^Graph g u v]
  (.containsEdge g u v))

(defn degree
  "The degree of vertex `v` in `g`."
  [^Graph g v]
  (.degreeOf g v))

(defn in-degree
  "The incoming degree of vertex `v` in `g`."
  [^Graph g v]
  (.inDegreeOf g v))

(defn out-degree
  "The outgoing degree of vertex `v` in `g`."
  [^Graph g v]
  (.outDegreeOf g v))

(defn incident-edges
  "A seq of rendered edges incident to vertex `v` in `g`."
  [^Graph g v]
  (map #(render-edge g %) (.edgesOf g v)))

(defn incoming-edges
  "A seq of rendered edges incoming to vertex `v` in `g`."
  [^Graph g v]
  (map #(render-edge g %) (.incomingEdgesOf g v)))

(defn outgoing-edges
  "A seq of rendered edges outgoing from vertex `v` in `g`."
  [^Graph g v]
  (map #(render-edge g %) (.outgoingEdgesOf g v)))

(defn edge-source
  "The source vertex of `edge` in `g`."
  [^Graph g edge]
  (.getEdgeSource g edge))

(defn edge-target
  "The target vertex of `edge` in `g`."
  [^Graph g edge]
  (.getEdgeTarget g edge))

(defn endpoints
  "The [source target] endpoints of `edge` in `g`."
  [^Graph g edge]
  [(edge-source g edge) (edge-target g edge)])

(defn get-edge
  "The edge object from `u` to `v` in `g`, or nil."
  [^Graph g u v]
  (.getEdge g u v))

(defn all-edges
  "The set of edge objects from `u` to `v` in `g`."
  [^Graph g u v]
  (set (.getAllEdges g u v)))

(defn- not-weighted [^Graph g operation]
  (ex-info "JGraphT graph is not weighted"
           {:cljgrapht/error :not-weighted
            :cljgrapht/operation operation
            :cljgrapht/graph-type (.getType g)}))

(defn set-weight
  "Set the weight of edge `u -> v` to `w`, returning `g`."
  ^Graph [^Graph g u v w]
  (when-not (weighted? g)
    (throw (not-weighted g :set-weight)))
  (.setEdgeWeight g u v (double w))
  g)

(defn order
  "The number of vertices in `g`."
  [^Graph g]
  (count (.vertexSet g)))

(defn size
  "The number of edges in `g`."
  [^Graph g]
  (count (.edgeSet g)))

(defn graph-type
  "The JGraphT graph type of `g` as a Clojure map."
  [^Graph g]
  (let [^GraphType type (.getType g)]
    {:directed? (.isDirected type)
     :undirected? (.isUndirected type)
     :weighted? (.isWeighted type)
     :allows-multiple-edges? (.isAllowingMultipleEdges type)
     :allows-self-loops? (.isAllowingSelfLoops type)
     :allows-cycles? (.isAllowingCycles type)
     :modifiable? (.isModifiable type)
     :simple? (.isSimple type)
     :pseudograph? (.isPseudograph type)
     :multigraph? (.isMultigraph type)}))

(defn unmodifiable-view
  "An unmodifiable view of `g`."
  ^Graph [^Graph g]
  (AsUnmodifiableGraph. g))

(defn unweighted-view
  "An unweighted view of `g`."
  ^Graph [^Graph g]
  (AsUnweightedGraph. g))

(defn undirected-view
  "An undirected view of directed graph `g`."
  ^Graph [^Graph g]
  (AsUndirectedGraph. g))

(defn edge-reversed-view
  "A view of directed graph `g` with all edge directions reversed."
  ^Graph [^Graph g]
  (EdgeReversedGraph. g))

(defn weighted-view
  "A weighted view of `g` using a map from edge objects to weights."
  ^Graph [^Graph g weights]
  (let [weight-map (HashMap.)]
    (doseq [[edge edge-weight] weights]
      (.put weight-map edge (double edge-weight)))
    (AsWeightedGraph. g weight-map)))

(defn subgraph
  "A view of `g` over a vertex subset and optional edge-object subset."
  (^Graph [^Graph g vertex-subset]
   (AsSubgraph. g (set vertex-subset)))
  (^Graph [^Graph g vertex-subset edge-subset]
   (AsSubgraph. g (set vertex-subset) (set edge-subset))))
