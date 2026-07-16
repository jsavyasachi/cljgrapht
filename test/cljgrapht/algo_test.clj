(ns cljgrapht.algo-test
  (:require [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]
            [cljgrapht.algo :as a]))

(deftest traversal-seqs
  (let [gr (g/graph [[:a :b] [:a :c] [:b :d] [:c :e]])]
    (testing "breadth-first traversal from a start vertex"
      (is (= [:a :b :c :d :e] (a/bfs gr :a))))
    (testing "depth-first traversal from a start vertex"
      ;; JGraphT's DFS iterator is stack-driven (LIFO), so later neighbors win.
      (is (= [:a :c :e :b :d] (a/dfs gr :a)))))
  (testing "directed traversal follows edge direction"
    (let [gr (g/digraph [[:a :b] [:a :c] [:b :d] [:c :e]])]
      (is (= [:a :b :c :d :e] (a/bfs gr :a)))
      (is (= [:a :c :e :b :d] (a/dfs gr :a)))))
  (testing "unknown start vertex is reported"
    (try
      (a/bfs (g/graph [[:a :b]]) :missing)
      (is false "expected ex-info")
      (catch clojure.lang.ExceptionInfo e
        (is (= :unknown-vertex (:cljgrapht/error (ex-data e))))))))

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

(deftest astar-shortest-path
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 5.0] [:b :c 1.0] [:c :d 1.0]])
        zero-heuristic (fn [_ _] 0.0)]
    (testing "uses the supplied heuristic and returns the cheapest path"
      (is (= {:path [:a :b :c :d] :weight 3.0}
             (a/astar gr :a :d zero-heuristic))))
    (testing "unreachable returns nil"
      (is (nil? (a/astar gr :d :a zero-heuristic))))))

(deftest bellman-ford-shortest-paths
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 4.0] [:b :c -2.0] [:c :d 2.0]])]
    (testing "supports negative edge weights without negative cycles"
      (is (= {:path [:a :b :c :d] :weight 1.0}
             (a/bellman-ford gr :a :d)))
      (is (= {:a 0.0 :b 1.0 :c -1.0 :d 1.0}
             (a/bellman-ford-distances gr :a))))
    (testing "unreachable returns nil"
      (is (nil? (a/bellman-ford gr :d :a))))))

(deftest johnson-all-pairs-shortest-paths
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 4.0] [:b :c -2.0] [:c :d 2.0]])]
    (is (= {:a {:b 1.0 :c -1.0 :d 1.0}
            :b {:c -2.0 :d 0.0}
            :c {:d 2.0}
            :d {}}
           (a/johnson-all-pairs gr)))))

(deftest yen-k-shortest-paths
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:b :d 1.0] [:a :c 1.0]
                                [:c :d 2.0] [:b :c 1.0]])]
    (is (= [{:path [:a :b :d] :weight 2.0}
            {:path [:a :c :d] :weight 3.0}
            {:path [:a :b :c :d] :weight 4.0}]
           (a/k-shortest-paths gr :a :d 3)))))

(deftest all-simple-paths-directed
  (let [gr (g/digraph [[:a :b] [:a :c] [:b :d] [:c :d] [:b :c]])]
    (is (= #{[:a :b :d] [:a :b :c :d] [:a :c :d]}
           (set (map :path (a/all-simple-paths gr :a :d)))))
    (testing "rejects undirected graphs"
      (try
        (a/all-simple-paths (g/graph [[:a :b]]) :a :b)
        (is false "expected ex-info")
        (catch clojure.lang.ExceptionInfo e
          (is (= :not-directed (:cljgrapht/error (ex-data e)))))))))

(deftest shortest-path-variants-and-transitive-operations
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:b :d 1.0] [:a :c 1.0]
                                [:c :d 2.0] [:b :c 1.0]])]
    (is (= {:path [:a :b :d] :weight 2.0}
           (a/bidirectional-shortest-path gr :a :d)))
    (is (= {:path [:a :b :d] :weight 2.0}
           (a/delta-stepping-shortest-path gr :a :d)))
    (is (= {:path [:a :b :d] :weight 2.0}
           (a/contraction-hierarchy-shortest-path gr :a :d)))
    (is (= (a/k-shortest-paths gr :a :d 3)
           (a/yen-k-shortest-paths gr :a :d 3)))
    (is (= #{[:a :b :d] [:a :c :d]}
           (set (map :path (a/disjoint-shortest-paths gr :a :d 2)))))
    (is (= #{[:a :b :d] [:a :c :d]}
           (set (map :path (a/all-directed-paths gr :a :d {:max-length 2}))))))
  (let [dag (g/digraph [[:a :b] [:b :c] [:a :c]])]
    (is (= #{[:a :b] [:b :c]} (a/transitive-reduction dag)))
    (is (= #{[:a :b] [:b :c] [:a :c]}
           (a/transitive-closure (g/digraph [[:a :b] [:b :c]]))))))

(deftest clique-and-scoring-algorithms
  (let [gr (g/graph [[:a :b] [:a :c] [:b :c] [:c :d]])]
    (testing "maximal cliques"
      (is (= #{#{:a :b :c} #{:c :d}}
             (set (a/maximal-cliques gr)))))
    (testing "maximal cliques reject directed graphs"
      (try
        (a/maximal-cliques (g/digraph [[:a :b]]))
        (is false "expected ex-info")
        (catch clojure.lang.ExceptionInfo e
          (is (= :not-undirected (:cljgrapht/error (ex-data e)))))))
    (testing "clustering coefficients"
      (is (= {:a 1.0 :b 1.0 :c (/ 1.0 3.0) :d 0.0}
             (a/clustering-coefficient gr)))
      (is (= 0.6 (a/global-clustering-coefficient gr))))
    (testing "coreness"
      (is (= {:a 2 :b 2 :c 2 :d 1}
             (a/coreness gr))))))

(deftest clique-and-chordal-variants
  (let [gr (g/graph [[:a :b] [:a :c] [:b :c] [:c :d]])
        expected #{#{:a :b :c} #{:c :d}}]
    (is (= expected (set (a/bron-kerbosch-maximal-cliques gr))))
    (is (= expected (set (a/pivot-maximal-cliques gr))))
    (is (= expected (set (a/degeneracy-maximal-cliques gr))))
    (is (true? (a/chordal? gr)))
    (is (= #{:a :b :c :d} (set (a/perfect-elimination-order gr))))
    (is (= #{:a :b :c} (a/chordal-maximum-clique gr)))
    (is (= 3 (:chromatic (a/chordal-coloring gr))))
    (is (= 2 (count (a/chordal-maximum-independent-set gr))))
    (is (= 2 (count (a/chordal-minimum-vertex-cover gr)))))
  (is (false? (a/chordal? (g/graph [[:a :b] [:b :c] [:c :d] [:d :a]])))))

(deftest graph-predicates-and-shape
  (testing "bipartite checks and partitions"
    (let [gr (g/graph [[:a :x] [:a :y] [:b :y]])]
      (is (true? (a/bipartite? gr)))
      (is (= #{#{:a :b} #{:x :y}}
             (set (a/bipartite-sets gr)))))
    (let [triangle (g/graph [[:a :b] [:b :c] [:c :a]])]
      (is (false? (a/bipartite? triangle)))
      (is (nil? (a/bipartite-sets triangle)))))
  (testing "directed acyclic graph predicate"
    (is (true? (a/dag? (g/digraph [[:a :b] [:b :c]]))))
    (is (false? (a/dag? (g/digraph [[:a :b] [:b :a]])))))
  (testing "connectivity predicates"
    (is (true? (a/connected? (g/graph [[:a :b] [:b :c]]))))
    (is (false? (a/connected? (g/graph [[:a :b] [:c :d]]))))
    (is (true? (a/strongly-connected? (g/digraph [[:a :b] [:b :a]]))))
    (is (false? (a/strongly-connected? (g/digraph [[:a :b]])))))
  (testing "density and isolated vertices"
    (let [gr (doto (g/graph [[:a :b] [:b :c]])
               (g/add-vertex :d))]
      (is (= (/ 1.0 3.0) (a/density gr)))
      (is (= #{:d} (a/isolated-vertices gr))))))

(deftest isomorphism
  (testing "VF2 graph isomorphism ignores vertex values by default"
    (is (true? (a/isomorphic? (g/graph [[:a :b] [:b :c]])
                              (g/graph [[1 2] [2 3]]))))
    (is (false? (a/isomorphic? (g/graph [[:a :b] [:b :c]])
                               (g/graph [[1 2] [2 3] [3 1]])))))
  (testing "rejects mixed directedness"
    (try
      (a/isomorphic? (g/graph [[:a :b]]) (g/digraph [[:a :b]]))
      (is false "expected ex-info")
      (catch clojure.lang.ExceptionInfo e
        (is (= :mixed-direction (:cljgrapht/error (ex-data e))))))))

(deftest simple-cycles-directed
  (let [gr (g/digraph [[:a :b] [:b :c] [:c :a] [:b :d] [:d :b]])]
    (is (= #{#{:a :b :c} #{:b :d}}
           (set (map set (a/simple-cycles gr)))))
    (testing "rejects undirected graphs"
      (try
        (a/simple-cycles (g/graph [[:a :b] [:b :c] [:c :a]]))
        (is false "expected ex-info")
        (catch clojure.lang.ExceptionInfo e
          (is (= :not-directed (:cljgrapht/error (ex-data e)))))))))

(deftest cycle-algorithm-variants
  (let [gr (g/digraph [[:a :b] [:b :c] [:c :a] [:b :d] [:d :b]])
        expected #{#{:a :b :c} #{:b :d}}]
    (is (= expected (set (map set (a/johnson-simple-cycles gr)))))
    (is (= expected (set (map set (a/tarjan-simple-cycles gr)))))
    (is (= expected (set (map set (a/szwarcfiter-lauer-simple-cycles gr))))))
  (let [triangle (g/graph [[:a :b] [:b :c] [:c :a]])]
    (is (= 3 (:length (a/cycle-basis triangle))))
    (is (true? (a/eulerian? triangle)))
    (is (= #{:a :b :c} (set (:path (a/eulerian-cycle triangle))))))
  (let [path (g/graph [[:a :b] [:b :c]])
        tour (a/chinese-postman path)]
    (is (= 4.0 (:weight tour)))
    (is (= (first (:path tour)) (last (:path tour))))))

(deftest connected-components-undirected
  (let [gr (g/graph [[:a :b] [:c :d]])]
    (is (= #{#{:a :b} #{:c :d}} (set (a/connected-components gr))))))

(deftest strongly-connected
  (let [gr (g/digraph [[:a :b] [:b :a] [:c :d]])]
    (is (= #{#{:a :b} #{:c} #{:d}} (set (a/strongly-connected-components gr))))))

(deftest connectivity-inspector-variants-and-blocks
  (let [gr (g/graph [[:a :b] [:b :c] [:c :a]
                     [:c :d] [:d :e] [:e :c] [:c :f]])]
    (is (= #{:c} (a/articulation-points gr)))
    (is (= #{#{:c :f}} (set (map set (a/bridges gr)))))
    (is (= #{#{:a :b :c} #{:c :d :e} #{:c :f}}
           (set (a/biconnected-components gr))))
    (is (= (set (a/biconnected-components gr)) (set (a/blocks gr))))
    (let [tree (a/block-cut-tree gr)]
      (is (= #{:c} (:articulation-points tree)))
      (is (= 3 (count (:edges tree))))))
  (let [gr (g/digraph [[:a :b] [:b :a] [:b :c] [:c :d] [:d :c]])
        expected #{#{:a :b} #{:c :d}}]
    (is (= expected (set (a/gabow-strongly-connected-components gr))))
    (is (= expected (set (a/kosaraju-strongly-connected-components gr))))
    (is (= {:components expected :edges #{[#{:a :b} #{:c :d}]}}
           (a/condensation gr)))))

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

(deftest spanning-tree-variants-and-spanner
  (let [gr (g/weighted-graph [[:a :b 1.0] [:b :c 2.0] [:a :c 3.0]])]
    (doseq [tree [(a/prim-minimum-spanning-tree gr)
                  (a/kruskal-minimum-spanning-tree gr)
                  (a/boruvka-minimum-spanning-tree gr)]]
      (is (= 3.0 (:weight tree)))
      (is (= 2 (count (:edges tree)))))
    (let [result (a/spanner gr 2)]
      (is (<= (count (:edges result)) 3))
      (is (number? (:weight result)))))
  (let [gr (g/weighted-graph [[:r :a 1.0] [:r :b 1.0] [:r :c 1.0]
                              [:a :b 2.0] [:a :c 2.0] [:b :c 2.0]])
        tree (a/capacitated-spanning-tree
              gr :r 2.0 {:r 0.0 :a 1.0 :b 1.0 :c 1.0})]
    (is (= 3 (count (:edges tree))))
    (is (map? (:labels tree)))))

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

(deftest matching-algorithm-variants
  (let [path (g/graph [[:a :b] [:b :c] [:c :d]])]
    (is (= 2 (:size (a/dense-edmonds-maximum-matching path))))
    (is (= 2 (:size (a/sparse-edmonds-maximum-matching path))))
    (is (pos? (:size (a/greedy-maximum-matching path)))))
  (let [gr (g/graph [[:a :x] [:a :y] [:b :y]])]
    (is (= 2 (:size (a/hopcroft-karp-matching gr #{:a :b} #{:x :y})))))
  (let [k22 (g/weighted-graph [[:a :x 1.0] [:a :y 4.0]
                                [:b :x 3.0] [:b :y 1.0]])]
    (is (= 2.0 (:weight (a/assignment k22 #{:a :b} #{:x :y}))))
    (is (= 2.0 (:weight
                (a/minimal-weight-perfect-matching k22 #{:a :b} #{:x :y}))))
    (is (number? (:weight (a/path-growing-weighted-matching k22))))
    (is (number? (:weight (a/greedy-weighted-matching k22))))))

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

(deftest flow-and-cut-variants
  (let [gr (g/weighted-digraph [[:s :a 3.0] [:s :b 2.0] [:a :t 2.0]
                                [:b :t 3.0] [:a :b 1.0]])]
    (doseq [flow [(a/edmonds-karp-max-flow gr :s :t)
                  (a/push-relabel-max-flow gr :s :t)
                  (a/dinic-max-flow gr :s :t)]]
      (is (= 5.0 (:value flow))))
    (is (= 5.0 (:weight (a/minimum-st-cut gr :s :t)))))
  (let [network (g/weighted-digraph [[:s :t 2.0]])
        result (a/min-cost-flow network
                                {:supplies {:s 2 :t -2}
                                 :capacities {[:s :t] 3}})]
    (is (= 4.0 (:cost result)))
    (is (= 2.0 (get-in result [:flow [:s :t]]))))
  (let [gr (g/weighted-graph [[:a :b 10.0] [:a :c 1.0] [:b :c 1.0]])
        cut (a/minimum-cut gr)
        tree (a/gomory-hu-tree gr)]
    (is (= 2.0 (:weight cut)))
    (is (= 2 (count (:edges tree))))))

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

(deftest vertex-cover-algorithms
  (let [path (g/graph [[:a :b] [:b :c] [:c :d]])]
    (is (= 2 (count (:vertices (a/min-vertex-cover path)))))
    (doseq [cover [(a/greedy-vertex-cover path)
                   (a/clarkson-two-approx-vertex-cover path)
                   (a/bar-yehuda-even-two-approx-vertex-cover path)
                   (a/edge-based-two-approx-vertex-cover path)]]
      (is (every? (fn [[u v]]
                    (or (contains? (:vertices cover) u)
                        (contains? (:vertices cover) v)))
                  (g/edges path)))))
  (let [edge (g/graph [[:a :b]])
        cover (a/min-vertex-cover edge {:a 10.0 :b 1.0})]
    (is (= #{:b} (:vertices cover)))
    (is (= 1.0 (:weight cover)))))

(deftest coloring-variants
  (let [gr (g/graph [[:a :b] [:b :c] [:c :d] [:d :a]])]
    (doseq [result [(a/largest-degree-first-coloring gr)
                    (a/smallest-degree-last-coloring gr)
                    (a/dsatur-coloring gr)
                    (a/random-greedy-coloring gr)
                    (a/color-refinement gr)]]
      (is (= #{:a :b :c :d} (set (keys (:colors result)))))
      (is (pos? (:chromatic result))))))

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

(deftest additional-centrality-algorithms
  (let [star (g/graph [[:hub :a] [:hub :b] [:hub :c]])]
    (doseq [scores [(a/harmonic-centrality star)
                    (a/eigenvector-centrality star)
                    (a/alpha-centrality star)]]
      (is (= #{:hub :a :b :c} (set (keys scores))))
      (is (= :hub (key (apply max-key val scores)))))))

(deftest graph-measurements
  (let [path (g/graph [[:a :b] [:b :c] [:c :d]])]
    (is (= 3.0 (a/diameter path)))
    (is (= 2.0 (a/radius path)))
    (is (= #{:b :c} (a/graph-center path)))
    (is (= #{:a :d} (a/graph-periphery path)))
    (is (= #{:a :d} (a/pseudo-periphery path)))
    (is (= {:a 3.0 :b 2.0 :c 2.0 :d 3.0}
           (a/vertex-eccentricities path))))
  (is (= 3 (a/girth (g/graph [[:a :b] [:b :c] [:c :a]]))))
  (is (nil? (a/girth (g/graph [[:a :b] [:b :c]])))))
