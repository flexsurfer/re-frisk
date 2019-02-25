(defproject re-frisk "0.5.4"
  :description "Visualize re-frame pattern data in your re-frame apps as a tree structure."
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.9.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.1"]
                 [re-frisk-shell "0.5.2"]
                 [com.cognitect/transit-cljs "0.8.256"]]

  :plugins [[lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.18"]
            [lein-doo "0.1.11"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/re-frisk/js/compiled" "target"]
  :figwheel {:http-server-root "re-frisk"}
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "dev"]
                :figwheel {:on-jsload "re-frisk.demo/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}
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
                :figwheel {:on-jsload "re-frisk.reagent_demo/on-js-reload"
                           :open-urls ["http://localhost:3449/reagent.html"]}
                :compiler {:main re-frisk.reagent-demo
                           :asset-path "js/compiled/out/reagent"
                           :output-to "resources/re-frisk/js/compiled/re_frisk_reagent.js"
                           :output-dir "resources/re-frisk/js/compiled/out/reagent"
                           :source-map-timestamp true
                           :preloads [devtools.preload
                                      frisk.preload]}}
               {:id "test"
                :source-paths ["src" "dev" "test"]
                :compiler {:main re-frisk.test-runner
                           :output-to "resources/test/test.js"
                           :optimizations :none
                           :target :nodejs}}]}

  :doo {:build "test"
        :alias {:default [:node]}}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [figwheel-sidecar "0.5.18"]
                                  [org.clojure/test.check "0.9.0"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]}})
