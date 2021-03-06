(ns re-frisk-remote.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [re-frame.core :as re-frame]
   [re-frame.db :as db]
   [re-frame.trace]
   [re-frisk.trace :as trace]
   [re-frisk.utils :as utils]
   [re-frisk.diff.diff :as diff]
   [re-frisk-remote.delta.delta :as delta]
   [taoensso.sente.packers.transit :as sente-transit]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre]
   [cognitect.transit :as transit]
   [re-frisk.reagent.impl.component :refer [patch-wrap-funs]]
   [re-frisk.reagent.impl.batching :refer [patch-next-tick]]
   [re-frisk.stat :as stat]))

;; if there are no opened tool web clients we don't want to send any data
;; either nil (do not send), or a map with the following optional keys:
;; :prev-app-db -- app DB last time :refrisk/app-db was sent
;; :prev-event-app-db -- app DB last time :refrisk/events was sent
;; :prev-subs -- subs last time :refrisk/subs was sent
(defonce send-state (atom nil))
(defonce initialized (atom false))

(defonce chsk-send (atom {}))
(defonce normalize-db-fn (atom nil))
(defonce ignore-events (atom nil))

(defn- send [message]
  (when (and message @send-state @chsk-send)
    (@chsk-send [:refrisk/message message])))

(defn- get-db []
  (let [db @db/app-db]
    (if @normalize-db-fn (@normalize-db-fn db) db)))

(defn- send-subs-delta []
  (let [subs (utils/get-subs)]
    (when-let [d (delta/delta (:prev-subs @send-state) subs)]
      (swap! send-state assoc :prev-subs subs)
      (send [:refrisk/subs-delta d]))))

(defn- send-app-db-delta []
  (let [db (get-db)]
    (when-let [d (delta/delta (:prev-app-db @send-state) db)]
      (swap! send-state assoc :prev-app-db db)
      (send [:refrisk/app-db-delta d]))))

(defn send-db-and-subs []
  (send-subs-delta)
  (send-app-db-delta))

(defonce init-stat-sent (atom false))

(defn send-init-stat []
  (when-not @init-stat-sent
    (reset! init-stat-sent true)
    (send [:refrisk/init-stat (stat/get-re-frame-handlers)])))

(defn- post-event-callback [value queue]
  (when @send-state
    (let [db   (get-db)
          ;;This diff may be expensive
          diff (diff/diff (:prev-event-app-db @send-state) db)]
      (send-init-stat)
      (swap! send-state assoc :prev-event-app-db db)
      (when (or (not @ignore-events) (not (get @ignore-events (first value))))
        (send [:refrisk/event {:event       value
                               :app-db-diff diff
                               :queue       queue}]))
      (utils/call-and-chill send-db-and-subs 500))))

(defn send-views [views]
  (when (seq views)
    (send [:refrisk/views views])))

(defn trace-cb [traces]
  (when @send-state
    (send-init-stat)
    (utils/call-and-chill send-db-and-subs 500)
    (let [traces (trace/update-views-and-get-traces send-views traces)]
      (doseq [[i trace] (map-indexed vector (trace/normalize-traces traces @ignore-events))]
        (js/setTimeout #(send [:refrisk/event trace]) (* i 20))))))

(defmulti event-msg-handler "Sente `event-msg`s handler" :id)

(defmethod event-msg-handler :chsk/state
  [{[{was-open? :open?} {now-open? :open?}] :?data :as msg}]
  (if (not= was-open? now-open?)
    (reset! send-state nil)))

(defn- enabled []
  (if @send-state
    (do
      (send [:refrisk/subs (:prev-subs @send-state)])
      (send [:refrisk/app-db (:prev-app-db @send-state)]))
    (let [db   (get-db)
          subs (utils/get-subs)]
      (reset! send-state {:prev-event-app-db db
                          :prev-app-db       db
                          :prev-subs         subs})
      (send [:refrisk/subs subs])
      (send [:refrisk/app-db db]))))

(defmethod event-msg-handler :chsk/recv
  [{[type data] :?data}]
  (case type
    :refrisk/enable (enabled)
    :refrisk/disable (reset! send-state nil)))

(defmethod event-msg-handler :default [msg]
  nil)

(defn- start-socket-and-router [host]
  (timbre/merge-config! {:ns-blacklist ["taoensso.sente" "taoensso.sente.*"]})
  (let [{:keys [send-fn ch-recv]}
        (sente/make-channel-socket-client!
         "/chsk"
         nil
         {:type     :auto
          :host     host
          :protocol :http
          :params   {:kind :re-frisk-remote}
          :packer   (sente-transit/get-transit-packer
                     :json
                     {:handlerForForeign #(transit/write-handler (fn [] "ForeignType") (fn [] ""))}
                     {})})]
    (reset! chsk-send send-fn)
    (sente/start-client-chsk-router! ch-recv event-msg-handler)))

(defn patch-reagent! []
  (patch-wrap-funs)
  (patch-next-tick))

(defn enable-re-frisk-remote! [& [{:keys [host] :as opts}]]
  (when-not @initialized
    (reset! initialized true)
    (reset! normalize-db-fn (:normalize-db-fn opts))
    (reset! ignore-events (:ignore-events opts))
    (start-socket-and-router (or host "localhost:4567"))
    (if (re-frame.trace/is-trace-enabled?)
      (do
        (patch-reagent!)
        (re-frame.trace/register-trace-cb :re-frisk-trace trace-cb))
      (re-frame/add-post-event-callback post-event-callback))))

(defn enable [& [params]]
  (enable-re-frisk-remote! params))