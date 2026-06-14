(ns cljgrapht.algo-test
  (:require [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]
            [cljgrapht.algo :as a]))

(deftest shortest-path-weighted
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 4.0] [:b :c 1.0] [:c :d 1.0]])]
    (testing "picks the cheaper multi-hop route over the direct expensive edge"
      (is (= {:path [:a :b :c :d] :weight 3.0} (a/shortest-path gr :a :d)))
      (is (= 3.0 (a/shortest-path-length gr :a :d))))
    (testing "unreachable returns nil"
      (is (nil? (a/shortest-path gr :d :a))))))

(deftest shortest-path-unweighted
  (let [gr (g/digraph [[:a :b] [:b :c] [:a :c]])]
    (testing "hop count via default unit weights"
      (is (= 1.0 (a/shortest-path-length gr :a :c)))
      (is (= [:a :c] (:path (a/shortest-path gr :a :c)))))))

(deftest connected-components-undirected
  (let [gr (g/graph [[:a :b] [:c :d]])]
    (is (= #{#{:a :b} #{:c :d}} (set (a/connected-components gr))))))

(deftest strongly-connected
  (let [gr (g/digraph [[:a :b] [:b :a] [:c :d]])]
    (is (= #{#{:a :b} #{:c} #{:d}} (set (a/strongly-connected-components gr))))))

(deftest topological-and-cycles
  (testing "DAG sorts with sources before sinks; no cycle"
    (let [dag (g/digraph [[:a :b] [:a :c] [:b :d] [:c :d]])
          order (a/topological-sort dag)]
      (is (false? (a/cycle? dag)))
      (is (= :a (first order)))
      (is (= :d (last order)))
      (is (= #{:a :b :c :d} (set order)))))
  (testing "directed cycle is detected; topo-sort returns nil"
    (let [cyc (g/digraph [[:a :b] [:b :a]])]
      (is (true? (a/cycle? cyc)))
      (is (nil? (a/topological-sort cyc))))))

(deftest minimum-spanning-tree
  (let [gr (g/weighted-graph [[:a :b 1.0] [:b :c 2.0] [:a :c 3.0]])
        mst (a/minimum-spanning-tree gr)]
    (is (= 3.0 (:weight mst)))
    (is (= #{#{:a :b} #{:b :c}} (set (map set (:edges mst)))))))

(deftest centrality
  (let [star (g/graph [[:hub :a] [:hub :b] [:hub :c]])]
    (testing "every vertex scored"
      (is (= #{:hub :a :b :c} (set (keys (a/betweenness-centrality star)))))
      (is (= #{:hub :a :b :c} (set (keys (a/pagerank star))))))
    (testing "hub is the most central node"
      (let [bc (a/betweenness-centrality star)
            pr (a/pagerank star)]
        (is (= :hub (key (apply max-key val bc))))
        (is (= :hub (key (apply max-key val pr))))))))
