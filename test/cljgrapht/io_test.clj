(ns cljgrapht.io-test
  (:require [clojure.java.io :as jio]
            [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]
            [cljgrapht.io :as gio]))

(defn- undirected-edge-set [gr]
  (set (map set (g/edges gr))))

(deftest dot-export-and-import
  (let [gr (g/graph [[:a :b] [:b :c]])
        s (gio/dot gr)
        imported (gio/read-dot s)]
    (testing "exports DOT with labels for Clojure vertex values"
      (is (string? s))
      (is (re-find #"graph G" s))
      (is (re-find #"label=\":a\"" s)))
    (testing "imports DOT into string vertices"
      (is (= 3 (count (g/vertices imported))))
      (is (= 2 (count (g/edges imported))))
      (is (= #{#{"_a" "_b"} #{"_b" "_c"}}
             (undirected-edge-set imported))))))

(deftest dot-write-and-import-file
  (let [gr (g/digraph [[:a :b]])
        f (java.io.File/createTempFile "cljgrapht" ".dot")]
    (try
      (gio/write-dot! gr (.getPath f))
      (let [imported (gio/read-dot (.getPath f))]
        (is (= #{"_a" "_b"} (g/vertices imported)))
        (is (= #{["_a" "_b"]} (set (g/edges imported)))))
      (finally
        (.delete f)))))

(deftest graphml-export-and-write
  (let [gr (g/graph [[:a :b]])
        s (gio/graphml gr)
        f (java.io.File/createTempFile "cljgrapht" ".graphml")]
    (try
      (testing "exports GraphML"
        (is (string? s))
        (is (re-find #"<graphml" s))
        (is (re-find #"<node" s)))
      (testing "writes GraphML"
        (gio/write-graphml! gr (.getPath f))
        (is (= s (slurp (jio/file f)))))
      (finally
        (.delete f)))))

(deftest graphml-import-round-trip
  (let [gr (g/weighted-digraph [[:a :b 2.5] [:b :c 4.0]])
        imported (gio/read-graphml (gio/graphml gr))]
    (is (= #{"_a" "_b" "_c"} (g/vertices imported)))
    (is (= #{["_a" "_b" 2.5] ["_b" "_c" 4.0]}
           (set (g/edges imported))))))

(deftest gml-export-write-and-import
  (let [gr (g/weighted-digraph [[:a :b 2.5] [:b :c 4.0]])
        s (gio/gml gr)
        imported (gio/read-gml s)
        f (java.io.File/createTempFile "cljgrapht" ".gml")]
    (try
      (is (re-find #"(?m)^graph$" s))
      (is (= 3 (count (g/vertices imported))))
      (is (= #{2.5 4.0} (set (map #(nth % 2) (g/edges imported)))))
      (gio/write-gml! gr (.getPath f))
      (is (= s (slurp f)))
      (finally
        (.delete f)))))
