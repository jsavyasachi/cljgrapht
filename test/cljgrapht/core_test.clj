(ns cljgrapht.core-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]))

(deftest core-works-without-loom
  (testing "Loom is not a mandatory runtime dependency"
    (is (not (contains? (:deps (edn/read-string (slurp "deps.edn")))
                        'net.clojars.savya/loom))))
  (testing "the core API loads and runs with Loom removed from the classpath"
    (let [separator (System/getProperty "path.separator")
          loom-path? #(re-find #"[/\\]loom(?:[/\\]|-[^/\\]*\\.jar$)" %)
          classpath (->> (str/split (System/getProperty "java.class.path")
                                    (re-pattern (java.util.regex.Pattern/quote separator)))
                         (remove loom-path?)
                         (str/join separator))
          java (str (System/getProperty "java.home")
                    java.io.File/separator "bin" java.io.File/separator "java")
          expression (str "(require '[cljgrapht.core :as g]) "
                          "(assert (= #{:a :b} "
                          "(g/vertices (g/graph [[:a :b]]))))")
          process (-> (ProcessBuilder. [java "-cp" classpath
                                        "clojure.main" "-e" expression])
                      (.redirectErrorStream true)
                      (.start))
          output (with-open [reader (io/reader (.getInputStream process))]
                   (slurp reader))]
      (is (zero? (.waitFor process)) output))))

(deftest empty-constructors
  (testing "empty graphs have no vertices or edges"
    (is (= #{} (g/vertices (g/graph))))
    (is (= #{} (g/vertices (g/digraph))))
    (is (empty? (g/edges (g/weighted-digraph))))))

(deftest build-from-edge-list
  (testing "undirected graph from pairs"
    (let [gr (g/graph [[:a :b] [:b :c]])]
      (is (= #{:a :b :c} (g/vertices gr)))
      (is (= #{#{:a :b} #{:b :c}} (set (map set (g/edges gr)))))))
  (testing "isolated vertices via add-vertex"
    (let [gr (g/add-vertex (g/graph) :solo)]
      (is (= #{:solo} (g/vertices gr))))))

(deftest directedness
  (testing "digraph distinguishes direction; neighbors split into succ/pred"
    (let [gr (g/digraph [[:a :b] [:a :c] [:c :a]])]
      (is (= #{:b :c} (set (g/successors gr :a))))
      (is (= #{:c} (set (g/predecessors gr :a))))
      (is (= #{:a} (set (g/successors gr :c)))))))

(deftest undirected-neighbors
  (testing "undirected neighbors are symmetric"
    (let [gr (g/graph [[:a :b] [:a :c]])]
      (is (= #{:b :c} (set (g/neighbors gr :a))))
      (is (= #{:a} (set (g/neighbors gr :b)))))))

(deftest weighted-edges
  (testing "weighted-digraph stores and reports edge weights"
    (let [gr (g/weighted-digraph [[:a :b 3.0] [:b :c 5.0]])]
      (is (= 3.0 (g/weight gr :a :b)))
      (is (= 5.0 (g/weight gr :b :c)))
      (is (= #{[:a :b 3.0] [:b :c 5.0]} (set (g/edges gr)))))))
