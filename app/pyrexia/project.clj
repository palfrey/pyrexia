(defproject pyrexia "0.1.0-SNAPSHOT"
  :description "Pyrexia: IoT temperature monitor"
  :url "https://github.com/palfrey/pyrexia"
  :license {:name "GNU AGPLv3"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [reagent "0.5.1"]
                 [environ "1.0.1"]
                 [com.andrewmcveigh/cljs-time "0.3.13"]]
  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.3.9"]
            [lein-cljfmt "0.3.0"]
            [lein-auto "0.1.2"]]

  :source-paths ["src"]

  :aliases {"format" ["auto" "do" ["cljfmt" "fix"] ["cljfmt" "fix" "project.clj"]]}
  :auto {:default {:paths ["."]}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]

                        :figwheel {:on-jsload "pyrexia.core/on-js-reload"}

                        :compiler {:main pyrexia.core
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/compiled/pyrexia.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true}}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/pyrexia.js"
                                   :main pyrexia.core
                                   :optimizations :advanced
                                   :pretty-print false}}]}

  :figwheel {:css-dirs ["resources/public/css"] ;; watch and update CSS
             :repl false ;; disable the REPL
})
