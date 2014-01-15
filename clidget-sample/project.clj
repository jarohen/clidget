(defproject clidget-sample ""

  :description "A sample application to demo the Clidget library"
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
            [lein-pdo "0.1.1"]]

  :frodo/config-resource "clidget-sample-config.edn"

  :source-paths ["src/clojure" "../src"]

  :resource-paths ["resources" "target/resources"]

  :cljsbuild {:builds {:dev
                       {:source-paths ["src/cljs" "../src"]
                        :compiler {:output-to "target/resources/js/clidget-sample.js"
                                   :output-dir "target/resources/js/"
                                   :optimizations :whitespace
                                   :pretty-print true

                                   ;; uncomment for source-maps
                                        ; :source-map "target/resources/js/clidget-sample.js.map"
                                   }}}}

  :aliases {"dev" ["pdo" "cljsbuild" "auto" "dev," "frodo"]})
