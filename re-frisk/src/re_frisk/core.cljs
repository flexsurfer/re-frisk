(ns re-frisk.core
  (:require [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [re-frisk.db :as data]
            [re-frisk.ui :as ui]
            [re-frisk.diff.diff :as diff]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [re-frisk.utils :as utils]
            [re-frame.trace]
            [re-frisk.trace :as trace]
            [re-frisk.subs-graph :as subs-graph]
            [re-frame.interop :as interop]
            [re-frisk.stat :as stat]
            [re-frisk.reagent.impl.batching :refer [patch-next-tick]]
            [re-frisk.reagent.impl.component :refer [patch-wrap-funs]]))

(defonce initialized (atom false))
(defonce prev-event (atom {}))

(defonce re-frame-data
         {:app-db (reagent/atom "no data")
          :events (reagent/atom [])
          :subs   (reagent/atom "no data")
          :stat   (reagent/atom {})
          :views  (reagent/atom {})})

(defn update-db-and-subs []
  ;;we need to deref all subscriptions, overwise they won't be deactivated
  (reset! (:subs re-frame-data) (utils/get-subs))
  (reset! (:app-db re-frame-data) @db/app-db))

(defn update-views [views]
  (when (seq views)
    (reset! (:views re-frame-data) views)))

(defn trace-cb [traces]
  (when-not (:paused? @data/tool-state)
    (let [ignore-events (get-in @data/tool-state [:opts :ignore-events])
          traces (trace/update-views-and-get-traces update-views traces)
          normalized  (trace/normalize-traces traces ignore-events)
          first-event (or (first @(:events re-frame-data)) (first normalized))]
      (when (seq normalized)
        (swap! (:events re-frame-data)
               concat
               (map (trace/normalize-durations first-event)
                    normalized))
        (stat/init-stat re-frame-data)
        (stat/update-trace-stat re-frame-data normalized)
        (js/setTimeout #(subs-graph/update-subs (filter :subs? normalized)) 100)
        (utils/call-and-chill update-db-and-subs 500)))))

(defn- post-event-callback [value queue]
  (when-not (:paused? @data/tool-state)
    (let [ignore-events (get-in @data/tool-state [:opts :ignore-events])
          app-db @db/app-db
          indx   (count @(:events re-frame-data))
          ;;This diff may be expensive
          diff   (diff/diff (:app-db @prev-event) app-db)]
      (reset! prev-event {:app-db app-db})
      (stat/init-stat re-frame-data)
      (stat/update-event-stat re-frame-data (first value))
      (when (or (not ignore-events) (not (get ignore-events (first value))))
        (swap! (:events re-frame-data) conj {:event          value
                                             :app-db-diff    diff
                                             :indx           indx
                                             :queue          queue
                                             :truncated-name (utils/truncate-name (str (first value)))}))
      (utils/call-and-chill update-db-and-subs 500))))

(defn find-error-trace []
  #_(select-keys
     (:trace
      (reduce (fn [acc {:keys [operation] :as trace}]
                (cond-> (assoc acc :prev trace)
                        (and (vector? operation) (= (last operation) :exception))
                        (assoc :trace (:prev acc))))
              {}
              @re-frame.trace/traces))
     [:operation :op-type]))

(defn register-exception-handler []
  (let [gOldOnError js/window.onerror]
    (set! js/window.onerror
          (fn [error-msg url line-number]
            (swap! (:events re-frame-data)
                   concat
                   [{:event          [:exception]
                     :truncated-name :exception
                     :error?         true
                     :indx           (count @(:events re-frame-data))
                     :error          (merge
                                      (find-error-trace)
                                      {:msg  error-msg
                                       :url  url
                                       :line line-number})}])
            (if gOldOnError
              (gOldOnError error-msg url line-number)
              false)))))

(defn patch-reagent! []
  (patch-wrap-funs)
  (patch-next-tick))

(defn enable-re-frisk! [& [opts]]
  (when-not @initialized
    (reset! initialized true)
    (swap! data/tool-state assoc :opts opts)
    #_(register-exception-handler)
    (if (re-frame.trace/is-trace-enabled?)
      (do
        (patch-reagent!)
        (re-frame.trace/register-trace-cb :re-frisk-trace trace-cb))
      (when-not (= (:events? opts) false)
        (reset! prev-event {:app-db @db/app-db})
        (re-frame/add-post-event-callback post-event-callback)))
    (subs-graph/set-root-node (interop/reagent-id db/app-db))
    (js/setTimeout ui/mount-internal 100 re-frame-data)))

(defn enable [& [opts]]
  (enable-re-frisk! opts))