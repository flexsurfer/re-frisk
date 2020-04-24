(ns re-frisk-remote.server.defaults
  "Middleware for providing a handler with sensible defaults."
  (:require [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.content-type :refer [wrap-content-type]]))

(def site-defaults
  "A default configuration for a browser-accessible website, based on current
  best practice."
  {:params    {:urlencoded true
               :multipart  true
               :nested     true
               :keywordize true}
   :cookies   true
   :session   {:flash true
               :cookie-attrs {:http-only true, :same-site :strict}}
   :security  {:anti-forgery   true
               :xss-protection {:enable? true, :mode :block}
               :frame-options  :sameorigin
               :content-type-options :nosniff}
   :static    {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects     true
               :content-types          true
               :default-charset        "utf-8"}})

(defn- wrap [handler middleware options]
  (if (true? options)
    (middleware handler)
    (if options
      (middleware handler options)
      handler)))

(defn- wrap-multi [handler middleware args]
  (wrap handler
        (fn [handler args]
          (if (coll? args)
            (reduce middleware handler args)
            (middleware handler args)))
        args))

(defn wrap-defaults
  "Wraps a handler in default Ring middleware, as specified by the supplied
  configuration map.

  See: api-defaults
       site-defaults
       secure-api-defaults
       secure-site-defaults"
  [handler config]
  (-> handler
      (wrap wrap-keyword-params   (get-in config [:params :keywordize] false))
      (wrap wrap-nested-params    (get-in config [:params :nested] false))
      (wrap wrap-multipart-params (get-in config [:params :multipart] false))
      (wrap wrap-params           (get-in config [:params :urlencoded] false))
      (wrap-multi wrap-resource   (get-in config [:static :resources] false))
      (wrap wrap-content-type     (get-in config [:responses :content-types] false))))