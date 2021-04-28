(ns re-frisk.reagent.impl.batching
  (:require
   [reagent.impl.batching :as batching]
   [re-frame.trace :as trace :include-macros true]
   [clojure.string :as string]
   [reagent.impl.component :as component]))

(def operation-name (memoize (fn [c] (last (string/split (component/component-name c) #" > ")))))

(defonce original-next-tick reagent.impl.batching/next-tick)

(defn next-tick
  [f]
  ;; Schedule a trace to be emitted after a render if there is nothing else scheduled after that render.
  ;; This signals the end of the epoch.
  (original-next-tick
   (fn []
     (trace/with-trace
       {:op-type :raf}
       (f)
       (trace/with-trace {:op-type :raf-end})
       (when (false? (.-scheduled? reagent.impl.batching/render-queue))
         (trace/with-trace {:op-type :reagent/quiescent}))))))

(defonce original-run-queue reagent.impl.batching/run-queue)

(defn run-queue [a]
  ;; sort components by mount order, to make sure parents
  ;; are rendered before children
  (.sort a batching/compare-mount-order)
  (dotimes [i (alength a)]
    (let [^js/React.Component c (aget a i)]
      (when (true? (.-cljsIsDirty c))
        (trace/with-trace
         {:op-type :force-update
          :operation (operation-name c)}
         (.forceUpdate c))))))

(defn patch-next-tick
  []
  (set! reagent.impl.batching/next-tick next-tick)
  (set! reagent.impl.batching/run-queue run-queue))