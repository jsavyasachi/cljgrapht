(ns cljgrapht.algo-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.set :as set]
            [cljgrapht.core :as g]
            [cljgrapht.algo :as a]))

(deftest traversal-seqs
  (let [gr (g/graph [[:a :b] [:a :c] [:b :d] [:c :e]])]
    (testing "breadth-first traversal from a start vertex"
      (is (= [:a :b :c :d :e] (a/bfs gr :a))))
    (testing "depth-first traversal from a start vertex"
      ;; JGraphT's DFS iterator is stack-driven (LIFO), so it visits later
      ;; neighbors first.
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

(deftest eppstein-k-shortest-paths
  (let [gr (g/make-graph {:directed? true :weighted? true :allow-self-loops? false
                          :edges [[:a :b 1] [:b :d 1] [:a :c 1] [:c :d 2]]})
        f (ns-resolve 'cljgrapht.algo 'eppstein-k-shortest-paths)]
    (is (some? f))
    (when f
      (is (= 2 (count (f gr :a :d 2)))))))

(deftest distinct-shortest-path-apis
  (let [gr (g/digraph [[:a :b] [:b :c] [:a :c]])]
    (is (some? (ns-resolve 'cljgrapht.algo 'bfs-shortest-path)))
    (is (some? (ns-resolve 'cljgrapht.algo 'dijkstra-many-to-many-paths)))
    (when-let [f (ns-resolve 'cljgrapht.algo 'bfs-shortest-path)]
      (is (= {:path [:a :c] :weight 1.0} (f gr :a :c))))
    (when-let [f (ns-resolve 'cljgrapht.algo 'dijkstra-many-to-many-paths)]
      (is (= {:path [:a :c] :weight 1.0}
             (get-in (f gr #{:a} #{:c}) [:a :c]))))))

(deftest modularity-clustering
  (let [gr (g/graph [[:a :b] [:b :c] [:c :d] [:d :e]])
        f (ns-resolve 'cljgrapht.algo 'modularity)]
    (is (some? f))
    (is (seq (a/clustering gr {:method :greedy-modularity})))
    (when f
      (is (number? (f gr [#{:a :b :c} #{:d :e}]))))))

(deftest graph-layout
  (let [gr (g/graph [[:a :b] [:b :c]])
        f (ns-resolve 'cljgrapht.algo 'layout-2d)]
    (is (some? f))
    (when f
      (let [coords (f gr {:algorithm :circular :width 100 :height 80})]
        (is (= #{:a :b :c} (set (keys coords))))
        (is (every? #(and (= 2 (count %)) (every? number? %))
                    (vals coords)))))))

(deftest structured-algorithm-validation
  (let [gr (g/graph [[:a :b]])]
    (try
      (a/shortest-path gr :missing :b)
      (is false "expected unknown vertex")
      (catch clojure.lang.ExceptionInfo e
        (is (= :unknown-vertex (:cljgrapht/error (ex-data e))))))
    (try
      (a/yen-k-shortest-paths gr :a :b 0)
      (is false "expected invalid k")
      (catch clojure.lang.ExceptionInfo e
        (is (= :invalid-option (:cljgrapht/error (ex-data e))))))))

(deftest path-and-flow-validation
  (let [gr (g/digraph [[:a :b]])]
    (doseq [[path-fn args] [[a/astar [:missing :b (constantly 0)]]
                            [a/bellman-ford [:missing :b]]
                            [a/bidirectional-shortest-path [:missing :b]]
                            [a/delta-stepping-shortest-path [:missing :b]]
                            [a/contraction-hierarchy-shortest-path [:missing :b]]
                            [a/yen-k-shortest-paths [:missing :b 1]]
                            [a/eppstein-k-shortest-paths [:missing :b 1]]]]
      (try
        (apply path-fn gr args)
        (is false "expected unknown vertex")
        (catch clojure.lang.ExceptionInfo e
          (is (= :unknown-vertex (:cljgrapht/error (ex-data e)))))))
    (try
      (a/all-directed-paths gr :a :b {:max-length -1})
      (is false "expected invalid max length")
      (catch clojure.lang.ExceptionInfo e
        (is (= :invalid-option (:cljgrapht/error (ex-data e))))))
    (try
      (a/minimum-st-cut gr :a :a)
      (is false "expected distinct source and sink")
      (catch clojure.lang.ExceptionInfo e
        (is (= :invalid-source-sink (:cljgrapht/error (ex-data e))))))))

(deftest link-prediction
  (let [gr (g/graph [[:a :b] [:a :c] [:b :c] [:b :d]])]
    (is (= 1.0 (a/common-neighbors-score gr :a :d)))
    (is (= (a/jaccard-coefficient gr :a :d)
           (a/link-prediction-score gr :a :d {:algorithm :jaccard})))
    (is (= 1.0 (get (a/predict-links gr [[:a :d]]) [:a :d])))))

(deftest additive-link-prediction-algorithms
  (let [gr (g/graph [[:a :b] [:a :c] [:b :c] [:b :d]])]
    (is (some? (ns-resolve 'cljgrapht.algo 'adamic-adar-index)))
    (is (some? (ns-resolve 'cljgrapht.algo 'leicht-holme-newman-index)))
    (when-let [f (ns-resolve 'cljgrapht.algo 'adamic-adar-index)]
      (is (= (f gr :a :d)
             (get (a/predict-links gr [[:a :d]] {:algorithm :adamic-adar}) [:a :d]))))))

(deftest edge-betweenness-centrality
  (let [gr (g/graph [[:a :b] [:b :c]])
        f (ns-resolve 'cljgrapht.algo 'edge-betweenness-centrality)]
    (is (some? f))
    (when f
      (let [scores (f gr)]
        (is (= 2 (count scores)))
        (is (= #{[:a :b] [:b :c]} (set (keys scores))))))))

(defn- close?
  "True when `a` and `b` are within 1e-9 of each other."
  [a b]
  (< (Math/abs (double (- a b))) 1e-9))

(deftest link-prediction-indices
  ;; Neighbors: a{b c}, b{a c d}, c{a b}, d{b}. The pair (:a :d) has only b as
  ;; a common neighbor. Each index has a value that you can calculate.
  (let [gr (g/graph [[:a :b] [:a :c] [:b :c] [:b :d]])]
    (testing "each convenience fn matches its closed-form value"
      (is (close? (/ 1.0 (Math/sqrt 2.0)) (a/salton-index gr :a :d)))          ; |CN|/sqrt(k_a*k_d)
      (is (close? (/ 2.0 3.0) (a/sorensen-index gr :a :d)))                     ; 2|CN|/(k_a+k_d)
      (is (close? (/ 1.0 3.0) (a/resource-allocation-index gr :a :d)))         ; sum 1/k_z, z in CN
      (is (close? 1.0 (a/hub-promoted-index gr :a :d)))                        ; |CN|/min(k_a,k_d)
      (is (close? 0.5 (a/hub-depressed-index gr :a :d)))                       ; |CN|/max(k_a,k_d)
      (is (close? 2.0 (a/preferential-attachment-score gr :a :d))))           ; k_a*k_d
    (testing "each convenience fn delegates to the matching :algorithm dispatch"
      (doseq [[f alg] [[a/salton-index :salton]
                       [a/sorensen-index :sorensen]
                       [a/resource-allocation-index :resource-allocation]
                       [a/hub-promoted-index :hub-promoted]
                       [a/hub-depressed-index :hub-depressed]
                       [a/preferential-attachment-score :preferential-attachment]]]
        (is (= (f gr :a :d)
               (a/link-prediction-score gr :a :d {:algorithm alg})))))))

(deftest lowest-common-ancestor
  (let [gr (g/digraph [[:root :left] [:root :right] [:left :leaf]])]
    (is (= :left (a/lca gr :leaf :left)))
    (is (= :root (a/lca gr :leaf :right {:algorithm :binary-lifting :root :root})))
    (is (= #{:root} (a/lca-set gr :leaf :right {:algorithm :euler-tour-rmq :root :root})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #":root"
                          (a/lca gr :leaf :right {:algorithm :tarjan})))))

(deftest steiner-tree
  (let [gr (g/weighted-graph [[:a :b 1.0] [:b :c 2.0] [:c :d 1.0] [:a :d 10.0]])
        result (a/steiner-tree gr #{:a :d})]
    (is (= #{#{:a :b} #{:b :c} #{:c :d}}
           (set (map set (:edges result)))))
    (is (= 4.0 (:weight result)))))

(deftest line-graph
  (let [gr (g/graph [[:a :b] [:b :c]])
        lg (a/line-graph gr)
        edges (vec (.edgeSet gr))]
    (is (= 2 (count (.vertexSet lg))))
    (is (= 1 (count (.edgeSet lg))))
    (is (or (.containsEdge lg (first edges) (second edges))
            (.containsEdge lg (second edges) (first edges))))))

(deftest maximum-density-subgraph
  (let [gr (g/weighted-graph [[:a :b 2.0] [:b :c 2.0] [:a :c 2.0] [:c :d 0.1]])
        result (a/maximum-density-subgraph gr :source-sentinel :sink-sentinel)]
    (is (pos? (:density result)))
    (is (set/subset? (:vertices result) (g/vertices gr)))))

(deftest shortest-path-weighted
  (let [gr (g/weighted-digraph [[:a :b 1.0] [:a :c 4.0] [:b :c 1.0] [:c :d 1.0]])]
    (testing "picks the cheaper multi-hop route over the direct expensive edge"
      (is (= {:path [:a :b :c :d] :weight 3.0} (a/shortest-path gr :a :d)))
      (is (= 3.0 (a/shortest-path-length gr :a :d))))
    (testing "unreachable returns nil"
      (is (nil? (a/shortest-path gr :d :a))))))

(deftest shortest-path-preserves-edge-identity
  (let [gr (g/make-graph {:directed? true
                          :weighted? true
                          :allow-multiple-edges? true})
        _ (g/add-edge gr :a :b 1.0)
        cheap (g/get-edge gr :a :b)
        _ (g/add-edge gr :a :b 9.0)
        result (a/shortest-path gr :a :b)]
    (is (= [:a :b] (:path result)))
    (is (= [cheap] (:edges result)))
    (is (= 1.0 (:weight result)))))

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
      (is (= #{:d} (a/isolated-vertices gr)))))
  (testing "density ignores loops and parallel edges"
    (let [gr (g/make-graph {:allow-multiple-edges? true
                            :edges [[:a :b] [:a :b] [:a :a]]})]
      (is (= 1.0 (a/density gr)))
      (is (<= (a/density gr) 1.0)))))

(deftest planarity-algorithms
  (let [cycle (g/graph [[:a :b] [:b :c] [:c :d] [:d :a]])]
    (is (true? (a/planar? cycle)))
    (is (= #{:a :b :c :d} (set (keys (a/planar-embedding cycle)))))
    (is (nil? (a/kuratowski-subdivision cycle))))
  (let [k5 (g/graph [[1 2] [1 3] [1 4] [1 5]
                     [2 3] [2 4] [2 5] [3 4] [3 5] [4 5]])
        subdivision (a/kuratowski-subdivision k5)]
    (is (false? (a/planar? k5)))
    (is (= #{1 2 3 4 5} (:vertices subdivision)))
    (is (= 10 (count (:edges subdivision))))))

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

(deftest isomorphism-and-tree-similarity-variants
  (is (true? (a/subgraph-isomorphic?
              (g/graph [[:a :b] [:b :c]])
              (g/graph [[1 2]]))))
  (is (false? (a/subgraph-isomorphic?
               (g/graph [[:a :b]])
               (g/graph [[1 2] [2 3]]))))
  (let [t1 (g/graph [[:a :b] [:a :c]])
        t2 (g/graph [[1 2] [1 3]])]
    (is (true? (a/tree-isomorphic? t1 t2)))
    (is (true? (a/tree-isomorphic? t1 :a t2 1)))
    (is (true? (a/color-refinement-isomorphic? t1 t2))))
  (is (= 1.0
         (a/tree-edit-distance (g/graph [[:a :b]]) :a
                               (g/graph [[:a :b] [:a :c]]) :a))))

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
  (testing "DAG sorts with sources before sinks and has no cycle"
    (let [dag (g/digraph [[:a :b] [:a :c] [:b :d] [:c :d]])
          order (a/topological-sort dag)]
      (is (false? (a/cycle? dag)))
      (is (= :a (first order)))
      (is (= :d (last order)))
      (is (= #{:a :b :c :d} (set order)))))
  (testing "directed cycle is detected and topo-sort returns nil"
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

(deftest matching-preserves-parallel-edge-identity
  (let [gr (g/make-graph {:allow-multiple-edges? true})
        _ (g/add-edge gr :a :b)
        first-edge (g/get-edge gr :a :b)
        _ (g/add-edge gr :a :b)
        result (a/maximum-matching gr)]
    (is (= 1 (count (:edge-objects result))))
    (is (contains? (set (g/all-edges gr :a :b))
                   (first (:edge-objects result))))
    (is (= #{[:a :b]} (:edges result)))
    (is (= first-edge (first (:edge-objects result))))))

(deftest dulmage-mendelsohn-decomposition
  (let [gr (g/graph [[:a :x] [:a :y] [:b :y]])
        result (a/dulmage-mendelsohn gr #{:a :b} #{:x :y})]
    (is (= #{:a :b :x :y}
           (apply set/union
                  (:partition1-dominated result)
                  (:partition2-dominated result)
                  (:perfect-matched result))))
    (is (vector? (:perfect-matched result))))
  (is (map? (a/dulmage-mendelsohn
             (g/graph [[:a :x]]) #{:a} #{:x} {:fine? true}))))

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

(deftest max-flow-preserves-parallel-edge-identity
  (let [gr (g/make-graph {:directed? true
                          :weighted? true
                          :allow-multiple-edges? true})
        _ (g/add-edge gr :s :t 2.0)
        first-edge (g/get-edge gr :s :t)
        _ (g/add-edge gr :s :t 5.0)
        result (a/max-flow gr :s :t)]
    (is (= 2 (count (:edge-flow result))))
    (is (= #{first-edge}
           (set (filter #(= 2.0 (get-in result [:edge-flow %]))
                        (keys (:edge-flow result))))))
    (is (= 7.0 (:value result)))))

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

(deftest tsp-algorithms
  (let [k4 (g/weighted-graph [[:a :b 1.0] [:b :c 1.0] [:c :d 1.0]
                              [:d :a 1.0] [:a :c 2.0] [:b :d 2.0]])]
    (is (= 4.0 (:weight (a/tsp-tour k4 {:method :held-karp}))))
    (doseq [method [:nearest-neighbor :christofides :greedy :nearest-insertion
                    :random :two-opt :palmer]]
      (let [{:keys [tour weight]} (a/tsp-tour k4 {:method method})]
        (is (= #{:a :b :c :d} (set tour)))
        (is (= (first tour) (last tour)))
        (is (number? weight))))
    (is (map? (a/tsp-tour k4)))))

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
    (testing "every vertex gets a score"
      (is (= #{:hub :a :b :c} (set (keys (a/betweenness-centrality star)))))
      (is (= #{:hub :a :b :c} (set (keys (a/pagerank star))))))
    (testing "hub is the most central vertex"
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

(deftest graph-clustering-algorithms
  (let [gr (g/graph [[:a :b] [:b :c] [:c :a]
                     [:d :e] [:e :f] [:f :d] [:c :d]])]
    (doseq [method [:girvan-newman :k-spanning-tree]]
      (let [clusters (a/clustering gr {:method method :k 2})]
        (is (= 2 (count clusters)))
        (is (= #{:a :b :c :d :e :f} (apply set/union clusters)))))
    (let [clusters (a/clustering gr {:method :label-propagation})]
      (is (= #{:a :b :c :d :e :f} (apply set/union clusters))))))

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
