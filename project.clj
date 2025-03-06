(defproject re-frisk-remote "1.6.0"
  :description "Take full control of re-frame app"
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src" "dev" "re-frisk/src"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.4"]
                 [reagent "1.1.1"]
                 [re-frame "1.2.0"]
                 ;; handlerForForeign
                 [com.cognitect/transit-cljs "0.8.256"]
                 [re-frisk/sente "1.20.0"]
                 ;; ns-blacklist
                 [com.taoensso/timbre "6.6.1"]

                 ;; REMOTE SERVER
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/core.async "1.1.587"]
                 ;; ring.middleware
                 [ring/ring-core "1.8.0"]
                 ;; ring.middleware.cors
                 [ring-cors "0.1.8"]
                 ;; http server
                 [http-kit "2.8.0"]
                 ;; routing
                 [compojure "1.5.2"]
                 ;; REMOTE CLIENT
                 ;; client UI
                 [re-com "2.8.0"]])
