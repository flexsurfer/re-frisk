(defproject re-frisk-remote "1.3.1"
  :description "Take full control of re-frame app"
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src" "dev" "re-frisk/src"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]
                 ;; handlerForForeign
                 [com.cognitect/transit-cljs "0.8.256"]
                 ;; web communications (fork is used because of https://github.com/ptaoussanis/sente/pull/357)
                 [re-frisk/sente "1.15.0"]
                 ;; ns-blacklist
                 [com.taoensso/timbre "4.10.0"]

                 ;; REMOTE SERVER
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/core.async "1.1.587"]
                 ;; ring.middleware
                 [ring/ring-core "1.8.0"]
                 ;; ring.middleware.cors
                 [ring-cors "0.1.8"]
                 ;; http server
                 [http-kit "2.2.0"]
                 ;; routing
                 [compojure "1.5.2"]
                 ;; REMOTE CLIENT
                 ;; client UI
                 [re-com "2.8.0"]])
