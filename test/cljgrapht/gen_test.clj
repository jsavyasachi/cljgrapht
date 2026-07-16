(ns cljgrapht.gen-test
  (:require [clojure.test :refer [deftest testing is]]
            [cljgrapht.core :as g]
            [cljgrapht.gen :as gen]))

(defn- undirected-edge-set [gr]
  (set (map set (g/edges gr))))

(deftest deterministic-generators
  (testing "complete graph"
    (let [gr (gen/complete-graph 5)]
      (is (= #{0 1 2 3 4} (g/vertices gr)))
      (is (= 10 (count (g/edges gr))))
      (is (= #{#{0 1} #{0 2} #{0 3} #{0 4} #{1 2}
               #{1 3} #{1 4} #{2 3} #{2 4} #{3 4}}
             (undirected-edge-set gr)))))
  (testing "ring graph"
    (let [gr (gen/ring-graph 5)]
      (is (= #{0 1 2 3 4} (g/vertices gr)))
      (is (= 5 (count (g/edges gr))))
      (is (= #{#{0 1} #{1 2} #{2 3} #{3 4} #{0 4}}
             (undirected-edge-set gr)))))
  (testing "star graph"
    (let [gr (gen/star-graph 5)]
      (is (= #{0 1 2 3 4} (g/vertices gr)))
      (is (= 4 (count (g/edges gr))))
      (is (= #{#{0 1} #{0 2} #{0 3} #{0 4}}
             (undirected-edge-set gr)))))
  (testing "grid graph"
    (let [gr (gen/grid-graph 3 4)]
      (is (= (set (range 12)) (g/vertices gr)))
      (is (= (+ (* 3 (dec 4)) (* (dec 3) 4))
             (count (g/edges gr)))))))

(deftest seeded-random-generators
  (testing "G(n,p) is reproducible under seed"
    (is (= (undirected-edge-set (gen/gnp-random-graph 20 0.25 {:seed 42}))
           (undirected-edge-set (gen/gnp-random-graph 20 0.25 {:seed 42})))))
  (testing "Barabasi-Albert is reproducible under seed"
    (is (= (undirected-edge-set (gen/barabasi-albert-graph 4 2 20 {:seed 42}))
           (undirected-edge-set (gen/barabasi-albert-graph 4 2 20 {:seed 42})))))
  (testing "Watts-Strogatz is reproducible under seed"
    (is (= (undirected-edge-set (gen/watts-strogatz-graph 20 4 0.25 {:seed 42}))
           (undirected-edge-set (gen/watts-strogatz-graph 20 4 0.25 {:seed 42}))))))

(deftest additional-deterministic-generators
  (testing "complete bipartite graph"
    (let [gr (gen/complete-bipartite-graph 3 4)]
      (is (= (set (range 7)) (some-> gr g/vertices)))
      (is (= 12 (some-> gr g/edges count)))
      (is (every? (fn [[u v]]
                    (not= (< u 3) (< v 3)))
                  (some-> gr g/edges)))))
  (testing "linear graph"
    (let [gr (gen/linear-graph 5)]
      (is (= (set (range 5)) (some-> gr g/vertices)))
      (is (= #{#{0 1} #{1 2} #{2 3} #{3 4}}
             (some-> gr undirected-edge-set)))))
  (testing "wheel graph"
    (let [gr (gen/wheel-graph 6)]
      (is (= 6 (some-> gr g/vertices count)))
      (is (= 10 (some-> gr g/edges count)))
      (is (some #(= 5 (.degreeOf gr %)) (some-> gr g/vertices)))))
  (testing "hypercube graph"
    (let [gr (gen/hypercube-graph 3)]
      (is (= 8 (some-> gr g/vertices count)))
      (is (= 12 (some-> gr g/edges count)))
      (is (every? #(= 3 (.degreeOf gr %)) (some-> gr g/vertices)))))
  (testing "empty graph"
    (let [gr (gen/empty-graph 4)]
      (is (= (set (range 4)) (some-> gr g/vertices)))
      (is (empty? (some-> gr g/edges)))))
  (testing "generalized Petersen graph"
    (let [gr (gen/generalized-petersen-graph 5 2)]
      (is (= 10 (some-> gr g/vertices count)))
      (is (= 15 (some-> gr g/edges count)))
      (is (every? #(= 3 (.degreeOf gr %)) (some-> gr g/vertices)))))
  (testing "windmill and Dutch windmill graphs"
    (let [windmill (gen/windmill-graph :windmill 3 4)
          dutch (gen/windmill-graph :dutch-windmill 3 4)]
      (is (= [10 18] [(some-> windmill g/vertices count)
                       (some-> windmill g/edges count)]))
      (is (= [10 12] [(some-> dutch g/vertices count)
                       (some-> dutch g/edges count)]))))
  (testing "complement graph"
    (let [gr (gen/complement-graph (g/graph [[0 1] [1 2]]))]
      (is (= #{0 1 2} (some-> gr g/vertices)))
      (is (= #{#{0 2}} (some-> gr undirected-edge-set))))))

(deftest additional-random-generators
  (testing "G(n,m) graph"
    (let [a (gen/gnm-random-graph 20 30 {:seed 42})
          b (gen/gnm-random-graph 20 30 {:seed 42})]
      (is (= 20 (some-> a g/vertices count)))
      (is (= 30 (some-> a g/edges count)))
      (is (= (some-> a undirected-edge-set)
             (some-> b undirected-edge-set)))))
  (testing "random bipartite graphs"
    (let [gnm (gen/gnm-random-bipartite-graph 4 5 7 {:seed 42})
          gnp-a (gen/gnp-random-bipartite-graph 4 5 0.4 {:seed 42})
          gnp-b (gen/gnp-random-bipartite-graph 4 5 0.4 {:seed 42})]
      (is (= [9 7] [(some-> gnm g/vertices count)
                     (some-> gnm g/edges count)]))
      (is (every? (fn [[u v]] (not= (< u 4) (< v 4)))
                  (some-> gnm g/edges)))
      (is (= (some-> gnp-a undirected-edge-set)
             (some-> gnp-b undirected-edge-set)))
      (is (every? (fn [[u v]] (not= (< u 4) (< v 4)))
                  (some-> gnp-a g/edges)))))
  (testing "Barabasi-Albert forest"
    (let [a (gen/barabasi-albert-forest 3 20 {:seed 42})
          b (gen/barabasi-albert-forest 3 20 {:seed 42})]
      (is (= [20 17] [(some-> a g/vertices count)
                       (some-> a g/edges count)]))
      (is (= (some-> a undirected-edge-set)
             (some-> b undirected-edge-set)))))
  (testing "Kleinberg small-world graph"
    (let [a (gen/kleinberg-small-world-graph 4 1 1 2 {:seed 42})
          b (gen/kleinberg-small-world-graph 4 1 1 2 {:seed 42})]
      (is (= 16 (some-> a g/vertices count)))
      (is (= (some-> a undirected-edge-set)
             (some-> b undirected-edge-set)))))
  (testing "scale-free graph"
    (let [a (gen/scale-free-graph 20 {:seed 42})
          b (gen/scale-free-graph 20 {:seed 42})]
      (is (= 20 (some-> a g/vertices count)))
      (is (= (some-> a undirected-edge-set)
             (some-> b undirected-edge-set)))))
  (testing "random regular graph"
    (let [gr (gen/random-regular-graph 20 4 {:seed 42})]
      (is (= [20 40] [(some-> gr g/vertices count)
                       (some-> gr g/edges count)]))
      (is (every? #(= 4 (.degreeOf gr %)) (some-> gr g/vertices)))))
  (testing "Prüfer trees"
    (let [random-tree (gen/prufer-tree 10 {:seed 42})
          fixed-tree (gen/prufer-tree [3 3 3])]
      (is (= [10 9] [(some-> random-tree g/vertices count)
                      (some-> random-tree g/edges count)]))
      (is (= [5 4] [(some-> fixed-tree g/vertices count)
                     (some-> fixed-tree g/edges count)]))
      (is (= 4 (.degreeOf fixed-tree
                          (first (filter #(= 3 %) (g/vertices fixed-tree))))))))
  (testing "planted partition graph"
    (let [gr (gen/planted-partition-graph 3 4 1.0 0.0 {:seed 42})]
      (is (= [12 18] [(some-> gr g/vertices count)
                       (some-> gr g/edges count)]))))
  (testing "directed scale-free graph"
    (let [a (gen/directed-scale-free-graph
             0.4 0.4 1.0 1.0 30 -1 {:seed 42})
          b (gen/directed-scale-free-graph
             0.4 0.4 1.0 1.0 30 -1 {:seed 42})]
      (is (true? (some-> a .getType .isDirected)))
      (is (= 30 (some-> a g/edges count)))
      (is (= (some-> a g/edges frequencies)
             (some-> b g/edges frequencies)))))
  (testing "linearized chord diagram graph"
    (let [a (gen/linearized-chord-diagram-graph 10 3 {:seed 42})
          b (gen/linearized-chord-diagram-graph 10 3 {:seed 42})]
      (is (= [10 30] [(some-> a g/vertices count)
                       (some-> a g/edges count)]))
      (is (true? (some-> a .getType .isAllowingMultipleEdges)))
      (is (= (some-> a g/edges frequencies)
             (some-> b g/edges frequencies))))))

(deftest weighted-matrix-generators
  (testing "weighted directed matrix graph"
    (let [gr (gen/weighted-matrix-graph
              [[0.0 1.0 2.0]
               [3.0 0.0 4.0]
               [5.0 6.0 0.0]])]
      (is (true? (some-> gr .getType .isDirected)))
      (is (= #{0 1 2} (some-> gr g/vertices)))
      (is (= #{[0 1 1.0] [0 2 2.0]
               [1 0 3.0] [1 2 4.0]
               [2 0 5.0] [2 1 6.0]}
             (some-> gr g/edges set)))))
  (testing "weighted bipartite matrix graph"
    (let [gr (gen/weighted-bipartite-matrix-graph
              2 3 [[1.0 2.0 3.0]
                   [4.0 5.0 6.0]])]
      (is (= (set (range 5)) (some-> gr g/vertices)))
      (is (= 6 (some-> gr g/edges count)))
      (is (= #{1.0 2.0 3.0 4.0 5.0 6.0}
             (some->> gr g/edges (map #(nth % 2)) set)))
      (is (every? (fn [[u v _]] (not= (< u 2) (< v 2)))
                  (some-> gr g/edges))))))

(deftest named-graph-catalog
  (let [expected-names
        #{:doyle :petersen :durer :dodecahedron :desargues :nauru
          :mobius-kantor :bull :butterfly :claw :bucky-ball :clebsch
          :grotzsch :bidiakis-cube :blanusa-first-snark
          :blanusa-second-snark :double-star-snark :brinkmann :gosset
          :chvatal :kittell :coxeter :diamond :ellingham-horton-54
          :ellingham-horton-78 :errera :folkman :franklin :frucht
          :goldner-harary :heawood :herschel :hoffman :krackhardt-kite
          :klein-3-regular :klein-7-regular :moser-spindle :pappus
          :poussin :schlafli :tietze :thomsen :tutte
          :zachary-karate-club}
        supported gen/supported-named-graphs]
    (is (= expected-names (set supported)))
    (doseq [name supported]
      (let [gr (gen/named-graph name)]
        (is (pos? (some-> gr g/vertices count)) (str name " has vertices"))
        (is (pos? (some-> gr g/edges count)) (str name " has edges"))
        (is (every? integer? (some-> gr g/vertices))
            (str name " uses integer vertices"))))
    (testing "representative named graph orders and sizes"
      (doseq [[name expected]
              {:petersen [10 15]
               :bull [5 5]
               :butterfly [5 6]
               :chvatal [12 24]
               :diamond [4 5]
               :franklin [12 18]
               :frucht [12 18]
               :goldner-harary [11 27]
               :grotzsch [11 20]
               :moser-spindle [7 11]
               :bucky-ball [60 90]
               :zachary-karate-club [34 78]}]
        (let [gr (gen/named-graph name)]
          (is (= expected [(some-> gr g/vertices count)
                           (some-> gr g/edges count)])))))))
