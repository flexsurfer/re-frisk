(defproject re-frisk "0.5.1"
  :description "Visualize re-frame pattern data in your re-frame apps as a tree structure."
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.6.1"

  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.2"]
                 [re-frisk-shell "0.5.0"]
                 [com.cognitect/transit-cljs "0.8.243"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.13"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/re-frisk/js/compiled" "target"]
  :figwheel {:http-server-root "re-frisk"}
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "dev"]
                :figwheel {:on-jsload "re-frisk.demo/on-js-reload"}

                :compiler {:main re-frisk.demo
                           :asset-path "js/compiled/out/re-frisk"
                           :output-to "resources/re-frisk/js/compiled/re_frisk.js"
                           :output-dir "resources/re-frisk/js/compiled/out/re-frisk"
                           :source-map-timestamp true
                           :preloads [devtools.preload
                                      re-frisk.preload]
                           :external-config {:re-frisk {:enabled true}}}}
               {:id "reagent"
                :source-paths ["src" "dev"]
                :figwheel {:on-jsload "re-frisk.reagent_demo/on-js-reload"}
                :compiler {:main re-frisk.reagent-demo
                           :asset-path "js/compiled/out/reagent"
                           :output-to "resources/re-frisk/js/compiled/re_frisk_reagent.js"
                           :output-dir "resources/re-frisk/js/compiled/out/reagent"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.7"]
                                  [figwheel-sidecar "0.5.14"]
                                  [com.cemerick/piggieback "0.2.2"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
