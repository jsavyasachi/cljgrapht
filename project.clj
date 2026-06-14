(defproject net.clojars.savya/cljgrapht "0.1.0"
  :description "Idiomatic Clojure graph library backed by JGraphT: fast algorithms over Clojure-value vertices"
  :url "https://github.com/jsavyasachi/cljgrapht"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.5"]
                 [org.jgrapht/jgrapht-core "1.5.3"]]
  :global-vars {*warn-on-reflection* true}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}
             :clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :clojure-1-12 {:dependencies [[org.clojure/clojure "1.12.5"]]}}
  :aliases {"all" ["with-profile" "+clojure-1-10:+clojure-1-11:+clojure-1-12"]})
