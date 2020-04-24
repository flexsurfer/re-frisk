(ns re-frisk.core
  (:require [re-frame.core :as re-frame]
            [re-frame.db :as db]
            [re-frame.subs :as subs]
            [re-frisk.db :as data]
            [re-frisk.ui.views :as ui.views]
            [re-frisk.diff.diff :as diff]
            [reagent.core :as reagent]
            [re-frisk.utils :as utils]
            [re-frame.trace]
            [re-frisk.trace :as trace]))

(defonce initialized (atom false))
(defonce prev-event (atom {}))

(defonce re-frame-data
         {:app-db (reagent/atom "not connected")
          :events (reagent/atom [])
          :subs   (reagent/atom "not connected")})

(defn update-db-and-subs []
  (reset! (:subs re-frame-data) @subs/query->reaction)
  (reset! (:app-db re-frame-data) @db/app-db))

(defn trace-cb [traces]
  (swap! (:events re-frame-data)
         concat (->> traces
                    (trace/normalize-traces)
                    (map trace/normalize-durations)))
  (utils/call-and-chill update-db-and-subs 500))

(defn- post-event-callback [value queue]
  (let [app-db @db/app-db
        indx (count @(:events re-frame-data))
        ;;This diff may be expensive
        diff (diff/diff (:app-db @prev-event) app-db)]
    (reset! prev-event {:app-db app-db})
    (swap! (:events re-frame-data) conj {:event          value
                                         :app-db-diff    diff
                                         :indx           indx
                                         :queue          queue
                                         :truncated-name (utils/truncate-name (str (first value)))})
    (utils/call-and-chill update-db-and-subs 500)))

(defn enable-re-frisk! [& [opts]]
  (when-not @initialized
    (reset! initialized true)
    (swap! data/tool-state assoc :opts opts)
    (if (re-frame.trace/is-trace-enabled?)
      (re-frame.trace/register-trace-cb :re-frisk-trace trace-cb)
      (when-not (= (:events? opts) false)
        (reset! prev-event {:app-db @db/app-db})
        (re-frame/add-post-event-callback post-event-callback)))
    (js/setTimeout ui.views/mount-internal 100 re-frame-data)))

(defn enable [& [opts]]
  (enable-re-frisk! opts))