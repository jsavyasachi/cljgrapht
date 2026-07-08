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

(deftest matching
  (testing "maximum cardinality matching on an undirected graph"
    (let [gr (g/graph [[:a :b] [:b :c] [:c :d]])
          matching (a/maximum-matching gr)]
      (is (= 2 (:size matching)))
      (is (= #{#{:a :b} #{:c :d}} (set (map set (:edges matching)))))))
  (testing "maximum weight matching on an undirected graph"
    (let [gr (g/weighted-graph [[:a :b 10.0] [:a :c 1.0] [:b :c 1.0]])
          matching (a/maximum-weight-matching gr)]
      (is (= 10.0 (:weight matching)))
      (is (= #{#{:a :b}} (set (map set (:edges matching)))))))
  (testing "bipartite matching"
    (let [gr (g/graph [[:a :x] [:a :y] [:b :y]])
          matching (a/bipartite-matching gr [:a :b] [:x :y])]
      (is (= 2 (:size matching)))
      (is (= #{#{:a :x} #{:b :y}} (set (map set (:edges matching)))))))
  (testing "maximum matching rejects directed graphs"
    (try
      (a/maximum-matching (g/digraph [[:a :b]]))
      (is false "expected ex-info")
      (catch clojure.lang.ExceptionInfo e
        (is (= :not-undirected (:cljgrapht/error (ex-data e))))))))

(deftest flow-and-cuts
  (let [gr (g/weighted-digraph [[:s :a 3.0] [:s :b 2.0] [:a :t 2.0]
                                [:b :t 3.0] [:a :b 1.0]])]
    (testing "max flow returns value and nonzero edge flows"
      (let [flow (a/max-flow gr :s :t)]
        (is (= 5.0 (:value flow)))
        (is (= #{[:s :a] [:s :b] [:a :t] [:b :t] [:a :b]}
               (set (keys (:flow flow)))))
        (is (every? (fn [[[u v] f]]
                      (<= f (g/weight gr u v)))
                    (:flow flow)))))
    (testing "min cut returns weight and source/sink partitions"
      (let [cut (a/min-cut gr :s :t)]
        (is (= 5.0 (:weight cut)))
        (is (contains? (:source-partition cut) :s))
        (is (contains? (:sink-partition cut) :t)))))
  (testing "max flow rejects undirected graphs"
    (try
      (a/max-flow (g/graph [[:s :t]]) :s :t)
      (is false "expected ex-info")
      (catch clojure.lang.ExceptionInfo e
        (is (= :not-directed (:cljgrapht/error (ex-data e))))))))

(deftest coloring
  (testing "triangle needs three colors"
    (let [gr (g/graph [[:a :b] [:b :c] [:a :c]])
          coloring (a/coloring gr)]
      (is (= 3 (:chromatic coloring)))
      (is (= #{:a :b :c} (set (keys (:colors coloring)))))
      (is (every? (fn [[u v]]
                    (not= (get-in coloring [:colors u])
                          (get-in coloring [:colors v])))
                  (g/edges gr)))))
  (testing "path needs two colors"
    (let [gr (g/graph [[:a :b] [:b :c] [:c :d]])
          coloring (a/coloring gr)]
      (is (= 2 (:chromatic coloring)))
      (is (= 2 (:chromatic (a/greedy-coloring gr))))
      (is (every? (fn [[u v]]
                    (not= (get-in coloring [:colors u])
                          (get-in coloring [:colors v])))
                  (g/edges gr)))))
  (testing "unknown coloring algorithm is reported"
    (try
      (a/coloring (g/graph [[:a :b]]) {:algorithm :missing})
      (is false "expected ex-info")
      (catch clojure.lang.ExceptionInfo e
        (is (= :unknown-algorithm (:cljgrapht/error (ex-data e))))))))

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
