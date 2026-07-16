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

(deftest dot-weight-and-attribute-round-trip
  (let [gr (g/weighted-digraph [[:a :b 2.5]])
        s (gio/dot gr {:attributes {:a {:color "red"}}})
        imported (gio/read-dot s)]
    (is (re-find #"color=\"red\"" s))
    (is (= #{["_a" "_b" 2.5]} (set (g/edges imported))))))

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

(deftest graphml-vertex-attributes
  (let [s (gio/graphml (g/graph [[:a :b]])
                       {:attributes {:a {:color "red" :rank 2}}})]
    (is (re-find #"attr.name=\"color\"" s))
    (is (re-find #">red</data>" s))
    (is (re-find #"attr.name=\"rank\"" s))))

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

(deftest json-export-write-and-import
  (let [gr (g/weighted-graph [[:a :b 2.5] [:b :c 4.0]])
        s (gio/json-graph gr {:attributes {:a {:color "red"}}})
        imported (gio/read-json s)
        f (java.io.File/createTempFile "cljgrapht" ".json")]
    (try
      (is (re-find #"\"nodes\"" s))
      (is (re-find #"\"color\":\"red\"" s))
      (is (= 3 (count (g/vertices imported))))
      (is (= #{2.5 4.0} (set (map #(nth % 2) (g/edges imported)))))
      (gio/write-json! gr (.getPath f))
      (is (re-find #"\"edges\"" (slurp f)))
      (finally
        (.delete f)))))

(deftest csv-formats-round-trip
  (let [gr (g/weighted-graph [[:a :b 2.5] [:b :c 4.0]])]
    (doseq [format [:edge-list :adjacency-list :matrix]]
      (let [s (gio/csv gr {:format format})
            imported (gio/read-csv s {:format format})]
        (is (= 3 (count (g/vertices imported))) (name format))
        (is (= #{2.5 4.0} (set (map #(nth % 2) (g/edges imported))))
            (name format))))))

(deftest csv-write
  (let [gr (g/graph [[:a :b]])
        f (java.io.File/createTempFile "cljgrapht" ".csv")]
    (try
      (gio/write-csv! gr (.getPath f) {:format :edge-list :delimiter \;})
      (is (re-find #";" (slurp f)))
      (finally
        (.delete f)))))

(deftest csv-custom-delimiter-weighted-round-trip
  (let [gr (g/weighted-graph [[:a :b 2.5]])
        opts {:format :adjacency-list :delimiter \;}
        imported (gio/read-csv (gio/csv gr opts) opts)]
    (is (= #{2.5} (set (map #(nth % 2) (g/edges imported)))))))

(deftest dimacs-formats-and-round-trip
  (let [weighted (g/weighted-digraph [[:a :b 2.5] [:b :c 4.0]])
        s (gio/dimacs weighted {:format :shortest-path})
        imported (gio/read-dimacs s)]
    (is (re-find #"(?m)^p sp 3 2$" s))
    (is (= #{[1 2 2.5] [2 3 4.0]} (set (g/edges imported)))))
  (let [gr (g/graph [[:a :b]])]
    (is (re-find #"(?m)^p edge" (gio/dimacs gr {:format :max-clique})))
    (is (re-find #"(?m)^p col" (gio/dimacs gr {:format :coloring})))))

(deftest dimacs-write
  (let [f (java.io.File/createTempFile "cljgrapht" ".col")]
    (try
      (gio/write-dimacs! (g/graph [[:a :b]]) (.getPath f)
                         {:format :coloring})
      (is (re-find #"(?m)^p col" (slurp f)))
      (finally
        (.delete f)))))

(deftest graph6-and-sparse6-round-trip
  (let [gr (g/graph [[:a :b] [:b :c]])]
    (doseq [format [:graph6 :sparse6]]
      (let [s (gio/graph6 gr {:format format})
            imported (gio/read-graph6 s)]
        (is (= 3 (count (g/vertices imported))) (name format))
        (is (= 2 (count (g/edges imported))) (name format))))))

(deftest graph6-write
  (let [f (java.io.File/createTempFile "cljgrapht" ".g6")]
    (try
      (gio/write-graph6! (g/graph [[:a :b]]) (.getPath f)
                         {:format :graph6})
      (is (seq (slurp f)))
      (finally
        (.delete f)))))

(deftest matrix-export-formats
  (let [gr (g/graph [[:a :b] [:b :c]])]
    (doseq [format [:adjacency-matrix :laplacian :normalized-laplacian]]
      (let [s (gio/matrix gr {:format format})]
        (is (string? s) (name format))
        (is (re-find #"_a" s) (name format))))))

(deftest matrix-write
  (let [f (java.io.File/createTempFile "cljgrapht" ".mtx")]
    (try
      (gio/write-matrix! (g/graph [[:a :b]]) (.getPath f))
      (is (re-find #"_a" (slurp f)))
      (finally
        (.delete f)))))

(deftest gexf-export-write-and-import
  (let [gr (g/weighted-digraph [[:a :b 2.5] [:b :c 4.0]])
        s (gio/gexf gr {:attributes {:a {:color "red"}}})
        imported (gio/read-gexf s)
        f (java.io.File/createTempFile "cljgrapht" ".gexf")]
    (try
      (is (re-find #"<gexf" s))
      (is (re-find #"color" s))
      (is (= #{["_a" "_b" 2.5] ["_b" "_c" 4.0]}
             (set (g/edges imported))))
      (gio/write-gexf! gr (.getPath f))
      (is (re-find #"<edges>" (slurp f)))
      (finally
        (.delete f)))))

(deftest visio-export-and-write
  (let [gr (g/digraph [[:a :b]])
        s (gio/visio gr)
        f (java.io.File/createTempFile "cljgrapht" ".csv")]
    (try
      (is (re-find #"(?m)^Shape," s))
      (is (re-find #"(?m)^Link," s))
      (gio/write-visio! gr (.getPath f))
      (is (= s (slurp f)))
      (finally
        (.delete f)))))

(deftest lemon-export-and-write
  (let [gr (g/weighted-digraph [[:a :b 2.5]])
        s (gio/lemon gr)
        f (java.io.File/createTempFile "cljgrapht" ".lgf")]
    (try
      (is (re-find #"@nodes" s))
      (is (re-find #"@arcs" s))
      (is (re-find #"2\.5" s))
      (gio/write-lemon! gr (.getPath f))
      (is (= s (slurp f)))
      (finally
        (.delete f)))))
