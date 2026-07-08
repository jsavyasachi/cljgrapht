(ns cljgrapht.loom-test
  (:require [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]
            [cljgrapht.loom]
            [loom.alg :as alg]
            [loom.graph :as lg]))

(defn- edge-set [edges]
  (set (map vec edges)))

(defn- undirected-edge-set [edges]
  (set (map set edges)))

(deftest graph-protocol-on-undirected-graph
  (let [gr (g/graph [[:a :b] [:a :c] [:c :d]])]
    (is (= #{:a :b :c :d} (set (lg/nodes gr))))
    (is (= #{#{:a :b} #{:a :c} #{:c :d}}
           (undirected-edge-set (lg/edges gr))))
    (is (true? (lg/has-node? gr :a)))
    (is (false? (lg/has-node? gr :missing)))
    (is (true? (lg/has-edge? gr :a :b)))
    (is (true? (lg/has-edge? gr :b :a)))
    (is (= #{:b :c} (set (lg/successors* gr :a))))
    (is (= 2 (lg/out-degree gr :a)))
    (is (= #{[:a :b] [:a :c]} (edge-set (lg/out-edges gr :a))))))

(deftest digraph-protocol-on-directed-graph
  (let [gr (g/digraph [[:a :b] [:a :c] [:c :a] [:d :a]])]
    (is (= #{:a :b :c :d} (set (lg/nodes gr))))
    (is (= #{[:a :b] [:a :c] [:c :a] [:d :a]}
           (edge-set (lg/edges gr))))
    (is (true? (lg/has-edge? gr :a :b)))
    (is (false? (lg/has-edge? gr :b :a)))
    (is (= #{:b :c} (set (lg/successors* gr :a))))
    (is (= 2 (lg/out-degree gr :a)))
    (is (= #{[:a :b] [:a :c]} (edge-set (lg/out-edges gr :a))))
    (is (= #{:c :d} (set (lg/predecessors* gr :a))))
    (is (= 2 (lg/in-degree gr :a)))
    (is (= #{[:c :a] [:d :a]} (edge-set (lg/in-edges gr :a))))))

(deftest digraph-protocol-rejects-undirected-graphs
  (let [gr (g/graph [[:a :b]])]
    (doseq [f [#(lg/predecessors* % :a)
               #(lg/in-degree % :a)
               #(lg/in-edges % :a)
               lg/transpose]]
      (is (= {:cljgrapht/error :not-directed}
             (select-keys (ex-data (is (thrown? clojure.lang.ExceptionInfo (f gr))))
                          [:cljgrapht/error]))))))

(deftest transpose-reverses-directed-edges-and-preserves-weights
  (let [gr (g/weighted-digraph [[:a :b 2.5] [:b :c 4.0]])
        tr (lg/transpose gr)]
    (is (not (identical? gr tr)))
    (is (= #{[:b :a] [:c :b]} (edge-set (lg/edges tr))))
    (is (= 2.5 (lg/weight* tr :b :a)))
    (is (= 4.0 (lg/weight* tr :c :b)))
    (is (nil? (lg/weight* tr :a :b)))))

(deftest weighted-graph-protocol-returns-jgrapht-weights
  (let [weighted (g/weighted-digraph [[:a :b 7.25]])
        unweighted (g/digraph [[:a :b]])]
    (is (= 7.25 (lg/weight* weighted [:a :b])))
    (is (= 7.25 (lg/weight* weighted :a :b)))
    (is (= 1.0 (lg/weight* unweighted [:a :b])))
    (is (= 1.0 (lg/weight* unweighted :a :b)))
    (is (nil? (lg/weight* weighted :b :a)))
    (is (nil? (lg/weight* unweighted :b :a)))))

(deftest editable-graph-protocol-mutates-and-returns-same-instance
  (let [gr (g/weighted-digraph)]
    (is (identical? gr (lg/add-nodes* gr [:a :b :c])))
    (is (= #{:a :b :c} (set (lg/nodes gr))))
    (is (identical? gr (lg/add-edges* gr [[:a :b 3.0] [:b :c]])))
    (is (= #{[:a :b] [:b :c]} (edge-set (lg/edges gr))))
    (is (= 3.0 (lg/weight* gr :a :b)))
    (is (= 1.0 (lg/weight* gr :b :c)))
    (is (identical? gr (lg/remove-edges* gr [[:a :b]])))
    (is (false? (lg/has-edge? gr :a :b)))
    (is (identical? gr (lg/remove-nodes* gr [:c])))
    (is (false? (lg/has-node? gr :c)))
    (is (identical? gr (lg/remove-all gr)))
    (is (empty? (lg/nodes gr)))))

(deftest weighted-editing-rejects-edge-weight-on-unweighted-graphs
  (let [gr (g/digraph)]
    (is (= {:cljgrapht/error :not-weighted}
           (select-keys
            (ex-data (is (thrown? clojure.lang.ExceptionInfo
                                  (lg/add-edges* gr [[:a :b 3.0]]))))
            [:cljgrapht/error])))))

(deftest loom-algorithms-run-on-cljgrapht-graphs
  (testing "breadth-first traversal and shortest path use Graph successors"
    (let [gr (g/digraph [[:a :b] [:a :c] [:b :d] [:c :d]])]
      (is (= [:a :b :c :d] (vec (alg/bf-traverse gr :a))))
      (is (contains? #{'(:a :b :d) '(:a :c :d)}
                     (alg/shortest-path gr :a :d)))))
  (testing "weighted shortest path uses JGraphT edge weights"
    (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 10.0] [:b :c 1.0]])]
      (is (= '(:a :b :c) (alg/dijkstra-path gr :a :c)))))
  (testing "connected components work through loom protocols"
    (let [gr (g/digraph [[:a :b] [:c :d]])]
      (is (= #{#{:a :b} #{:c :d}}
             (set (map set (alg/connected-components gr))))))))
