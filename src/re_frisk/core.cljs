(ns re-frisk.core
  (:require [reagent.core :as reagent]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe] :as re-frame]
            [re-frisk.data :refer [re-frame-events re-frame-data initialized deb-data]]
            [re-frisk.devtool :as dev]
            [re-frisk-shell.core :as ui]
            [re-frisk.help :as help]))

(defn post-event-callback [value]
  (let [cntx ((first value) (:contexts @deb-data))]
    (swap! re-frame-events conj
           (if cntx
             (assoc cntx :event value)
             value))))

(defn- render-re-frisk [params]
  (let [div (.createElement js/document "div")]
    (.appendChild (.-body js/document) div)
    (set! js/onbeforeunload
          #(when (:win @deb-data)
             (.alert (:win @deb-data) "Application has been closed or refreshed. Debugger has been stoped!")))
    (reagent/render [ui/re-frisk-shell re-frame-data deb-data (merge {:on-click dev/open-debugger-window} params)] div)))

(defn enable-re-frisk! [& [{:keys [kind->id->handler?] :as opts}]]
  (when-not @initialized
    (if re-frame.core/reg-sub
      (re-frame.core/reg-sub ::db (fn [db _] db))
      (re-frame.core/register-sub ::db (fn [db _] (reaction @db))))
    (reset! re-frame-data (merge (help/re-frame-handlers kind->id->handler?)
                                 {:app-db (subscribe [::db])}))
    (reset! initialized true)
    (swap! deb-data assoc :prefs opts)
    (when-not (= (:events? opts) false)
      (re-frame/add-post-event-callback post-event-callback))
    (js/setTimeout render-re-frisk 100 opts)))

(defn enable-frisk! [& params]
  (when-not @initialized
    (do
      (reset! initialized true)
      (js/setTimeout render-re-frisk 100 (first params)))))

(defn add-data [key data]
    (swap! re-frame-data assoc key data))

(defn add-in-data [keys data]
    (swap! re-frame-data assoc-in keys data))

(def watch-context
  (re-frame.core/->interceptor
    :id      :re-frisk-watch-context
    :before  (fn [context]
               (swap! deb-data assoc-in [:contexts (-> context :coeffects :event first) :before] context)
               context)))

(defn reg-view [view subs events]
  (when (:app-db @re-frame-data)
    (do
      (swap! re-frame-data assoc-in [:views view :events] (set events))
      (swap! re-frame-data assoc-in [:views view :subs] (into {} (map #(hash-map % (subscribe [%])) subs)))
      (doseq [s subs]
        (swap! re-frame-data assoc-in [:subs s] (subscribe [s]))))))

(defn unmount-view [view]
  (when (:app-db @re-frame-data)
    (swap! re-frame-data update-in [:views] dissoc view)))

((or re-frame.core/reg-event-db re-frame.core/register-handler)
 :re-frisk/update-db (fn [_ [_ value]] value))

(comment (reg-view) (unmount-view))

