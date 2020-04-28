(defproject re-frisk-remote "1.2.0"
  :description "Take the full control on your re-frame app"
  :url "https://github.com/flexsurfer/re-frisk"
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src" "dev" "re-frisk/src"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]
                 [re-com "2.8.0"]
                 ;;REMOTE
                 [ring/ring-core "1.8.0"]
                 [ring-cors "0.1.8"]
                 [http-kit "2.2.0"]
                 [com.taoensso/sente "1.11.0"]
                 [compojure "1.5.2"]
                 [com.cognitect/transit-clj  "0.8.319"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [javax.servlet/servlet-api "2.5"]])