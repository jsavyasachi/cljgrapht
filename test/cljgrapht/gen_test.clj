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
