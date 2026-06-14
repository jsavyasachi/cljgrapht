;; Benchmark cljgrapht against the pure-Clojure graph libraries loom and ubergraph.
;;
;;   cd bench && clojure -M bench.clj                       # 2,000 vertices
;;   clojure -J-Dn=10000 -J-Dextra=40000 -M bench.clj       # 10,000 vertices
;;
;; Same seeded random weighted digraph built in each library; construction is
;; excluded from the algorithm timings. Times are criterium quick-bench means.
(require '[loom.graph :as lg] '[loom.alg :as la]
         '[ubergraph.core :as uc] '[ubergraph.alg :as ua]
         '[cljgrapht.core :as cg] '[cljgrapht.algo :as ca]
         '[criterium.core :as crit])

(def N (Integer/parseInt (System/getProperty "n" "2000")))
(def EXTRA (Integer/parseInt (System/getProperty "extra" "8000")))

;; 0->1->...->N-1 backbone (guarantees a reachable target) plus EXTRA random edges.
(def rng (java.util.Random. 42))
(defn ri [n] (.nextInt rng n))
(def edges
  (vec (concat
        (for [i (range (dec N))] [i (inc i) (+ 1.0 (* 9.0 (.nextDouble rng)))])
        (for [_ (range EXTRA)] [(ri N) (ri N) (+ 1.0 (* 9.0 (.nextDouble rng)))]))))
(def undirected-edges (mapv (fn [[u v _]] [u v]) edges))

(defn mean-ms [f] (* 1000.0 (first (:mean (crit/quick-benchmark (f) {})))))

(def loom-wdg (apply lg/weighted-digraph edges))
(def loom-ug  (apply lg/graph undirected-edges))
(def uber-wdg (apply uc/digraph (map (fn [[u v w]] [u v {:weight w}]) edges)))
(def uber-ug  (apply uc/graph undirected-edges))
(def cg-wdg   (cg/weighted-digraph edges))
(def cg-ug    (cg/graph undirected-edges))

(def s 0)
(def t (dec N))

(defn row [label f] (println (format "%-26s %10.3f ms" label (mean-ms f))))

(println (format "=== %d vertices, %d edges ===" N (count edges)))
(println "-- construction --")
(row "loom"      #(apply lg/weighted-digraph edges))
(row "ubergraph" #(apply uc/digraph (map (fn [[u v w]] [u v {:weight w}]) edges)))
(row "cljgrapht" #(cg/weighted-digraph edges))
(println "-- weighted shortest path (Dijkstra) --")
(row "loom"      #(la/dijkstra-path loom-wdg s t))
(row "ubergraph" #(ua/shortest-path uber-wdg {:start-node s :end-node t :cost-attr :weight}))
(row "cljgrapht" #(ca/shortest-path cg-wdg s t))
(println "-- connected components --")
(row "loom"      #(doall (la/connected-components loom-ug)))
(row "ubergraph" #(doall (la/connected-components uber-ug)))
(row "cljgrapht" #(doall (ca/connected-components cg-ug)))
