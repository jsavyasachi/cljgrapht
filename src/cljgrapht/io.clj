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
           (org.jgrapht.nio.graphml GraphMLExporter)))

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
    (export-string exporter g)))

(defn write-graphml!
  "Write `(graphml g)` to `path`, returning nil."
  [^Graph g path]
  (spit path (graphml g)))
