(defproject clidget-sample ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.5.1"]

                 [ring/ring-core "1.2.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]

                 [prismatic/dommy "0.1.1"]

                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/tools.reader "0.8.1"]

                 [jarohen/frodo-core "0.2.10"]]

  :plugins [[jarohen/lein-frodo "0.2.10"]
            [lein-cljsbuild "1.0.0"]
            [lein-pdo "0.1.1"]
            [com.keminglabs/cljx "0.3.1"]]

  :frodo/config-resource "clidget-sample-config.edn"

  :source-paths ["src/clojure" "target/generated/clj" "../src"]

  :resource-paths ["resources" "target/resources"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]}

  :cljsbuild {:builds {:dev
                       {:source-paths ["src/cljs" "target/generated/cljs" "../src"]
                        :compiler {:output-to "target/resources/js/clidget-sample.js"
                                   :output-dir "target/resources/js/"
                                   :optimizations :whitespace
                                   :pretty-print true

                                   ;; uncomment for source-maps
                                        ; :source-map "target/resources/js/clidget-sample.js.map"
                                   }}

                       :prod
                       {:source-paths ["src/cljs" "target/generated/cljs" "../src"]
                        :compiler {:output-to "target/resources/js/clidget-sample.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["externs/jquery.js"]}}}}

  :aliases {"dev" ["pdo" "cljx" "auto," "cljsbuild" "auto" "dev," "frodo"]
            "start" ["do" "cljx" "once," "cljsbuild" "once" "prod," "trampoline" "frodo"]})
