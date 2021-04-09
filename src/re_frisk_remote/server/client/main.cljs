(ns re-frisk-remote.server.client.main
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [reagent.dom :as rdom]
   [taoensso.sente :as sente]
   [taoensso.sente.packers.transit :as sente-transit]
   [re-frisk.core :as re-frisk]
   [re-frisk.db :as db]
   [re-frisk.ui.views :as ui]
   [re-frisk.utils :as utils]
   [re-frisk-remote.delta.delta :as delta]
   [re-frisk.trace :as trace]
   [re-frisk.subs-graph :as subs-graph]
   [re-frisk.stat :as stat]))

(defn update-app-db [val]
  (reset! (:app-db re-frisk/re-frame-data) val))

(defn update-subs [val]
  (reset! (:subs re-frisk/re-frame-data) val))

(defn update-init-stat [val]
  (reset! (:stat re-frisk/re-frame-data) val))

(defn update-views [val]
  (reset! (:views re-frisk/re-frame-data) val))

(defn apply-app-db-delta [val]
  (try
    (swap! (:app-db re-frisk/re-frame-data) delta/apply val)
    (swap! db/tool-state dissoc :app-db-delta-error)
    (catch :default e
      (swap! db/tool-state assoc :app-db-delta-error true))))

(defn apply-subs-delta [val]
  (try
    (swap! (:subs re-frisk/re-frame-data) delta/apply val)
    (swap! db/tool-state dissoc :subs-delta-error)
    (catch :default e
      (swap! db/tool-state assoc :subs-delta-error true))))

(defn update-events [{:keys [event op-type subs?] :as value}]
  (when-not (:paused? @db/tool-state)
    (let [events @(:events re-frisk/re-frame-data)
          first-event (first events)
          value (assoc value :indx (count events))]
      (swap! (:events re-frisk/re-frame-data)
             conj (if op-type
                    ((trace/normalize-durations (or first-event value)) value)
                    (assoc value :truncated-name (utils/truncate-name (str (first event))))))
      (if op-type
        (stat/update-trace-stat re-frisk/re-frame-data [value])
        (stat/update-event-stat re-frisk/re-frame-data (first event)))
      (when subs?
        (js/setTimeout #(subs-graph/update-subs [value]) 100)))))

;SENTE HANDLERS
(defmulti -event-msg-handler "Multimethod to handle Sente `event-msg`s" :id)

(defn event-msg-handler
  [{:keys [id ?data event] :as ev-msg}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default [_])

(defmethod -event-msg-handler :chsk/recv
  [{[msg-type [type data]] :?data}]
  (when (= msg-type :refrisk/message)
    (case type
      :refrisk/app-db (update-app-db data)
      :refrisk/event (update-events data)
      :refrisk/subs (update-subs data)
      :refrisk/app-db-delta (apply-app-db-delta data)
      :refrisk/subs-delta (apply-subs-delta data)
      :refrisk/init-stat (update-init-stat data)
      :refrisk/views (update-views data)
      :noop)))

(defn mount []
  (swap! db/tool-state assoc :doc js/document)
  (subs-graph/init js/window js/document)
  (rdom/render [ui/main-view re-frisk/re-frame-data db/tool-state js/document]
               (.getElementById js/document "app")))

;ENTRY POINT
(defn ^:export main [& [port]]
  (let [{:keys [chsk ch-recv state]}
        (sente/make-channel-socket-client!
         "/chsk"
         nil
         {:type   :auto
          :host   (str "localhost:" (or port js/location.port))
          :packer (sente-transit/get-transit-packer)
          :params {:kind :re-frisk-client}})]
    (sente/start-client-chsk-router! ch-recv event-msg-handler)
    (mount)))

(defn on-js-reload []
  (mount))