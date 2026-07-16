(ns cljgrapht.core-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g])
  (:import (org.jgrapht.graph DefaultEdge)))

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

(deftest graph-interface-mutation-and-inspection
  (let [gr (g/digraph [[:a :b] [:c :b] [:b :b]])
        ab (g/get-edge gr :a :b)]
    (testing "membership, endpoints, order, and size"
      (is (true? (g/contains-vertex? gr :a)))
      (is (false? (g/contains-vertex? gr :missing)))
      (is (true? (g/contains-edge? gr :a :b)))
      (is (false? (g/contains-edge? gr :b :a)))
      (is (= [:a :b] (g/endpoints gr ab)))
      (is (= :a (g/edge-source gr ab)))
      (is (= :b (g/edge-target gr ab)))
      (is (= 3 (g/order gr)))
      (is (= 3 (g/size gr))))
    (testing "directed degree and incident-edge semantics"
      (is (= 4 (g/degree gr :b)))
      (is (= 3 (g/in-degree gr :b)))
      (is (= 1 (g/out-degree gr :b)))
      (is (= #{[:a :b] [:c :b] [:b :b]}
             (set (g/incident-edges gr :b))))
      (is (= #{[:a :b] [:c :b] [:b :b]}
             (set (g/incoming-edges gr :b))))
      (is (= #{[:b :b]}
             (set (g/outgoing-edges gr :b)))))
    (testing "removal by endpoints and edge object"
      (is (identical? gr (g/remove-edge gr :a :b)))
      (is (false? (g/contains-edge? gr :a :b)))
      (is (identical? gr (g/remove-edge gr (g/get-edge gr :c :b))))
      (is (false? (g/contains-edge? gr :c :b)))
      (is (identical? gr (g/remove-vertex gr :b)))
      (is (false? (g/contains-vertex? gr :b))))))

(deftest undirected-degrees-and-weight-mutation
  (testing "undirected in/out degree equals degree and loops count twice"
    (let [gr (g/graph [[:a :b] [:b :c] [:b :b]])]
      (is (= 4 (g/degree gr :b)))
      (is (= 4 (g/in-degree gr :b)))
      (is (= 4 (g/out-degree gr :b)))))
  (testing "weighted graphs support setter round trips"
    (let [gr (g/weighted-graph [[:a :b 2.0]])]
      (is (identical? gr (g/set-weight gr :a :b 8.5)))
      (is (= 8.5 (g/weight gr :a :b)))))
  (testing "unweighted graphs reject weight mutation with structured data"
    (let [gr (g/graph [[:a :b]])]
      (try
        (g/set-weight gr :a :b 2.0)
        (is false "expected ex-info")
        (catch clojure.lang.ExceptionInfo e
          (is (= :not-weighted (:cljgrapht/error (ex-data e))))
          (is (= :set-weight (:cljgrapht/operation (ex-data e)))))))))

(deftest configurable-graph-construction
  (testing "defaults reproduce graph behavior and report the full graph type"
    (let [gr (g/make-graph {:edges [[:a :b]]})]
      (is (= #{#{:a :b}} (set (map set (g/edges gr)))))
      (is (= {:directed? false
              :undirected? true
              :weighted? false
              :allows-multiple-edges? false
              :allows-self-loops? true
              :allows-cycles? true
              :modifiable? true
              :simple? false
              :pseudograph? false
              :multigraph? false}
             (g/graph-type gr)))))
  (testing "parallel edges remain distinct and removable by edge object"
    (let [gr (g/make-graph {:allow-multiple-edges? true
                            :edges [[:a :b] [:a :b]]})
          all (g/all-edges gr :a :b)
          edge (first all)]
      (is (= 2 (count all)))
      (is (= 2 (count (g/edges gr))))
      (g/remove-edge gr edge)
      (is (= 1 (count (g/all-edges gr :a :b))))))
  (testing "self-loop policy is configurable"
    (let [gr (g/make-graph {:allow-self-loops? false})]
      (is (thrown? IllegalArgumentException (g/add-edge gr :a :a)))
      (is (true? (:simple? (g/graph-type gr))))))
  (testing "direction, weighting, edge class, and Clojure suppliers are accepted"
    (let [next-id (atom 0)
          gr (g/make-graph {:directed? true
                            :weighted? true
                            :vertex-supplier #(keyword (str "v" (swap! next-id inc)))
                            :edge-supplier #(DefaultEdge.)})]
      (is (= :v1 (.addVertex gr)))
      (is (= :v2 (.addVertex gr)))
      (is (true? (g/directed? gr)))
      (is (true? (g/weighted? gr))))
    (let [gr (g/make-graph {:edge-class DefaultEdge})]
      (g/add-edge gr :a :b)
      (is (instance? DefaultEdge (g/get-edge gr :a :b))))))

(deftest graph-views
  (testing "unmodifiable view rejects mutation"
    (let [view (g/unmodifiable-view (g/graph [[:a :b]]))]
      (is (false? (:modifiable? (g/graph-type view))))
      (is (thrown? UnsupportedOperationException (g/add-vertex view :c)))))
  (testing "unweighted view hides weights"
    (let [view (g/unweighted-view (g/weighted-graph [[:a :b 9.0]]))]
      (is (false? (g/weighted? view)))
      (is (= #{[:a :b]} (set (g/edges view))))))
  (testing "undirected and reversed views change edge direction"
    (let [base (g/digraph [[:a :b]])
          undirected (g/undirected-view base)
          reversed (g/edge-reversed-view base)]
      (is (true? (g/contains-edge? undirected :b :a)))
      (is (false? (g/directed? undirected)))
      (is (= [:b :a]
             (g/endpoints reversed (g/get-edge reversed :b :a))))))
  (testing "weighted view uses and updates its weight map"
    (let [base (g/graph [[:a :b]])
          edge (g/get-edge base :a :b)
          view (g/weighted-view base {edge 7.25})]
      (is (true? (g/weighted? view)))
      (is (= 7.25 (g/weight view :a :b)))
      (g/set-weight view :a :b 4.5)
      (is (= 4.5 (g/weight view :a :b)))))
  (testing "subgraphs can be induced or constrained to an edge subset"
    (let [base (g/graph [[:a :b] [:b :c] [:a :c]])
          induced (g/subgraph base #{:a :b})
          edge (g/get-edge base :a :c)
          constrained (g/subgraph base #{:a :b :c} #{edge})]
      (is (= #{:a :b} (g/vertices induced)))
      (is (= #{#{:a :b}} (set (map set (g/edges induced)))))
      (is (= #{#{:a :c}} (set (map set (g/edges constrained))))))))
