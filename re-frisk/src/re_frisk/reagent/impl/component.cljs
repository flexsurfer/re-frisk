(ns re-frisk.reagent.impl.component
  (:require
   [goog.object            :as gobj]
   [clojure.string         :as string]
   [re-frame.trace         :as trace :include-macros true]
   [re-frame.interop       :as interop]
   [reagent.impl.component :as component]
   [reagent.impl.batching  :as batch]
   [reagent.impl.util      :as util]
   [reagent.ratom          :as ratom]
   [reagent.debug          :refer-macros [dev? warn error warn-unless assert-callable]]))

(def operation-name (memoize (fn [c] (last (string/split (component/component-name c) #" > ")))))

;; Monkey patched reagent.impl.component/wrap-funs to hook into render
(defn wrap-funs [fmap compiler]
  (when (dev?)
    (let [renders (select-keys fmap [:render :reagentRender])
          render-fun (-> renders vals first)]
      (assert (not (:componentFunction fmap)) ":component-function is no longer supported, use :reagent-render instead.")
      (assert (pos? (count renders)) "Missing reagent-render")
      (assert (== 1 (count renders)) "Too many render functions supplied")
      (assert-callable render-fun)))
  (let [render-fun (or (:reagentRender fmap)
                       (:render fmap))
        legacy-render (nil? (:reagentRender fmap))
        name (or (:displayName fmap)
                 (util/fun-name render-fun)
                 (str (gensym "reagent")))
        fmap (reduce-kv (fn [m k v]
                          (assoc m k (component/get-wrapper k v)))
                        {} fmap)]
    (assoc fmap
      :displayName name
      :cljsLegacyRender legacy-render
      :reagentRender render-fun
      :componentDidMount (fn componentDidMount []
                           (this-as c
                             (trace/with-trace
                              {:op-type   :componentDidMount
                               :operation (operation-name c)
                               :tags      {:order (gobj/get c "cljsMountOrder")}})
                             (when-let [f (:componentDidMount fmap)]
                               (.call f c))))
      :componentWillUnmount (fn componentWillUnmount []
                              (this-as c
                                (trace/with-trace
                                 {:op-type   :componentWillUnmount
                                  :operation (operation-name c)})
                                (when-let [f (:componentWillUnmount fmap)]
                                  (.call f c))))
      ;:shouldComponentUpdate
      #_(fn shouldComponentUpdate [nextprops nextstate]
          (this-as c
            (trace/with-trace
             {:op-type   :should-upd
              :operation (operation-name c)}
             (when-let [f (:shouldComponentUpdate fmap)]
               (.call f c nextprops nextstate)))))
      :render (fn render []
                (this-as c
                  (trace/with-trace
                   {:op-type :render
                    :tags    (if-let [component-name (component/component-name c)]
                               {:component-name component-name}
                               {})
                    :operation (operation-name c)}
                   (if util/*non-reactive*
                     (component/do-render c compiler)
                     (let [^clj rat (gobj/get c "cljsRatom")
                           _        (batch/mark-rendered c)
                           res      (if (nil? rat)
                                      (ratom/run-in-reaction #(component/do-render c compiler) c "cljsRatom"
                                                             batch/queue-render component/rat-opts)
                                      (._run rat false))
                           cljs-ratom (gobj/get c "cljsRatom")]
                       (trace/merge-trace!
                        {:tags {:reaction (interop/reagent-id cljs-ratom)
                                :input-signals (when cljs-ratom
                                                 (map interop/reagent-id (gobj/get cljs-ratom "watching" :none)))}})
                       res))))))))

(defn patch-wrap-funs
  []
  (set! reagent.impl.component/wrap-funs wrap-funs))

;(defonce original-create-class reagent.impl.component/create-class)

#_(defn create-class
    [body compiler]
    (trace/with-trace
     {:op-type :create-class}
     (let [cmp (original-create-class body compiler)]
       (trace/merge-trace!
        {:operation (.-displayName cmp)})
       cmp)))
