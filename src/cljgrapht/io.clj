(ns cljgrapht.io
  "DOT and GraphML import/export for `cljgrapht.core` graphs."
  (:require [cljgrapht.core :as core]
            [clojure.java.io :as jio]
            [clojure.string :as str])
  (:import (java.io File StringReader StringWriter)
           (java.util Collections)
           (java.util.function Function)
           (org.jgrapht Graph)
           (org.jgrapht.nio DefaultAttribute GraphExporter)
           (org.jgrapht.nio.dot DOTExporter DOTImporter)
           (org.jgrapht.nio.gml GmlExporter GmlExporter$Parameter GmlImporter)
           (org.jgrapht.nio.graphml GraphMLExporter GraphMLImporter)
           (org.jgrapht.nio.json JSONExporter JSONImporter)))

(defn- valid-id? [^String s]
  (boolean (re-matches #"[A-Za-z_][A-Za-z0-9_]*|-?(\.[0-9]+|[0-9]+(\.[0-9]*)?)" s)))

(defn- sanitized-id [v]
  (let [s (pr-str v)
        s (if (seq s) s "v")
        s (str/replace s #"[^A-Za-z0-9_]" "_")
        s (if (re-find #"^[A-Za-z_]" s) s (str "v_" s))]
    (if (valid-id? s) s "v")))

(defn- id-provider ^Function []
  (let [ids (atom {})
        used (atom #{})]
    (reify Function
      (apply [_ v]
        (or (get @ids v)
            (let [base (sanitized-id v)
                  id (loop [candidate base
                            n 2]
                       (if (contains? @used candidate)
                         (recur (str base "_" n) (inc n))
                         candidate))]
              (swap! ids assoc v id)
              (swap! used conj id)
              id))))))

(defn- label-provider ^Function []
  (reify Function
    (apply [_ v]
      (Collections/singletonMap "label" (DefaultAttribute/createAttribute (pr-str v))))))

(defn- attribute [v]
  (cond
    (instance? Boolean v) (DefaultAttribute/createAttribute ^Boolean v)
    (instance? Integer v) (DefaultAttribute/createAttribute ^Integer v)
    (instance? Long v) (DefaultAttribute/createAttribute ^Long v)
    (instance? Float v) (DefaultAttribute/createAttribute ^Float v)
    (instance? Double v) (DefaultAttribute/createAttribute ^Double v)
    :else (DefaultAttribute/createAttribute (str v))))

(defn- attribute-provider ^Function [attributes]
  (reify Function
    (apply [_ v]
      (into {}
            (map (fn [[k value]] [(name k) (attribute value)]))
            (get attributes v {})))))

(defn- weight-provider ^Function [^Graph g]
  (reify Function
    (apply [_ e]
      {"weight" (DefaultAttribute/createAttribute
                  (double (.getEdgeWeight g e)))})))

(defn- export-string [^GraphExporter exporter ^Graph g]
  (let [writer (StringWriter.)]
    (.exportGraph exporter g writer)
    (str writer)))

(defn- path-string? [x]
  (and (string? x) (.exists (jio/file x))))

(defn- input-string [x]
  (cond
    (instance? File x) (slurp x)
    (path-string? x) (slurp (jio/file x))
    :else (str x)))

(defn- directed-dot? [^String s]
  (boolean (re-find #"(?is)^\s*(strict\s+)?digraph\b" s)))

(defn- graph-for [directed? weighted?]
  (cond
    (and directed? weighted?) (core/weighted-digraph)
    directed? (core/digraph)
    weighted? (core/weighted-graph)
    :else (core/graph)))

(defn dot
  "DOT string for `g`. Vertex ids are sanitized `pr-str` values; labels keep the
  original `pr-str` for display."
  [^Graph g]
  (let [^DOTExporter exporter (DOTExporter. (id-provider))]
    (.setVertexAttributeProvider exporter (label-provider))
    (export-string exporter g)))

(defn write-dot!
  "Write `(dot g)` to `path`, returning nil."
  [^Graph g path]
  (spit path (dot g)))

(defn read-dot
  "Read a DOT string or existing path. Imported vertices are DOT id strings, not
  EDN parsed Clojure values."
  [path-or-string]
  (let [s (input-string path-or-string)
        ^Graph g (if (directed-dot? s) (core/digraph) (core/graph))
        ^DOTImporter importer (DOTImporter.)]
    (.setVertexFactory importer (reify Function
                                  (apply [_ id] id)))
    (.importGraph importer g (StringReader. s))
    g))

(defn graphml
  "GraphML string for `g` with sanitized `pr-str` vertex ids."
  [^Graph g]
  (let [^GraphMLExporter exporter (GraphMLExporter. (id-provider))]
    (.setExportVertexLabels exporter true)
    (.setExportEdgeWeights exporter true)
    (export-string exporter g)))

(defn write-graphml!
  "Write `(graphml g)` to `path`, returning nil."
  [^Graph g path]
  (spit path (graphml g)))

(defn read-graphml
  "Read a GraphML string or existing path. Imported vertices are GraphML id
  strings, not EDN parsed Clojure values."
  [path-or-string]
  (let [s (input-string path-or-string)
        directed? (boolean (re-find #"(?i)edgedefault=\"directed\"" s))
        weighted? (boolean (re-find #"(?i)attr\.name=\"weight\"" s))
        ^Graph g (graph-for directed? weighted?)
        ^GraphMLImporter importer (GraphMLImporter.)]
    (.setSchemaValidation importer false)
    (.setVertexFactory importer (reify Function
                                  (apply [_ id] id)))
    (.importGraph importer g (StringReader. s))
    g))

(defn gml
  "GML string for `g`. Set `:attributes` to a vertex-to-attribute map."
  (^String [^Graph g] (gml g {}))
  (^String [^Graph g {:keys [attributes]}]
   (let [^GmlExporter exporter (GmlExporter.)]
     (.setParameter exporter GmlExporter$Parameter/EXPORT_EDGE_WEIGHTS true)
     (when attributes
       (.setParameter exporter GmlExporter$Parameter/EXPORT_CUSTOM_VERTEX_ATTRIBUTES true)
       (.setVertexAttributeProvider exporter (attribute-provider attributes)))
     (export-string exporter g))))

(defn write-gml!
  "Write `(gml g opts)` to `path`, returning nil."
  ([^Graph g path] (write-gml! g path {}))
  ([^Graph g path opts] (spit path (gml g opts))))

(defn read-gml
  "Read a GML string or existing path. Imported vertices are numeric GML ids."
  [path-or-string]
  (let [s (input-string path-or-string)
        directed? (boolean (re-find #"(?m)^\s*directed\s+1\s*$" s))
        weighted? (boolean (re-find #"(?m)^\s*weight\s+" s))
        ^Graph g (graph-for directed? weighted?)
        ^GmlImporter importer (GmlImporter.)]
    (.setVertexFactory importer (reify Function
                                  (apply [_ id] id)))
    (.importGraph importer g (StringReader. s))
    g))

(defn json-graph
  "JGraphT JSON string for `g`. Set `:attributes` to a vertex-to-attribute map."
  (^String [^Graph g] (json-graph g {}))
  (^String [^Graph g {:keys [attributes]}]
   (let [^JSONExporter exporter (JSONExporter. (id-provider))]
     (when attributes
       (.setVertexAttributeProvider exporter (attribute-provider attributes)))
     (when (.. g getType isWeighted)
       (.setEdgeAttributeProvider exporter (weight-provider g)))
     (export-string exporter g))))

(defn write-json!
  "Write `(json-graph g opts)` to `path`, returning nil."
  ([^Graph g path] (write-json! g path {}))
  ([^Graph g path opts] (spit path (json-graph g opts))))

(defn read-json
  "Read a JGraphT JSON string or existing path. Use `:directed? true` when the
  source edge list represents a directed graph."
  ([path-or-string] (read-json path-or-string {}))
  ([path-or-string {:keys [directed?]}]
   (let [s (input-string path-or-string)
         weighted? (boolean (re-find #"\"weight\"\s*:" s))
         ^Graph g (graph-for directed? weighted?)
         ^JSONImporter importer (JSONImporter.)]
     (.setVertexFactory importer (reify Function
                                   (apply [_ id] id)))
     (.importGraph importer g (StringReader. s))
     g)))
