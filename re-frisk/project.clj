(defproject re-frisk "1.5.0"
  :description "Take full control of re-frame app"
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src"]
  :plugins      [[thomasa/mranderson "0.5.3"]]
  :profiles {:mranderson {:mranderson {:project-prefix "re-frisk.inlined-deps"}
                          :dependencies ^:replace [^:source-dep [reagent "1.0.0"
                                                                 :exclusions [cljsjs/react
                                                                              cljsjs/react-dom
                                                                              cljsjs/react-dom-server]]]}}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [re-frame "0.12.0"]
                 [re-com "2.8.0"]])