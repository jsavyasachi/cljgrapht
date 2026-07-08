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
