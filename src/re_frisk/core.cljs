(ns re-frisk.core
  (:require [reagent.core :as r]
            [re-frisk.data :refer [re-frame-events re-frame-data initialized]]
            [re-frisk.devtool :as d]
            [re-frisk.ui :as ui]
            [datafrisk.core :as f]
            [re-frame.core :refer [reg-sub reg-event-db subscribe] :as rfr]))

(defn post-event-callback [value]
  (swap! re-frame-events conj value))

(defn- render-re-frisk [params]
  (let [div (js/document.createElement "div")]
    (js/document.body.appendChild div)
    (r/render [ui/re-frisk-shell re-frame-data (merge {:on-click d/open-debugger-window} params)] div)))

(defn enable-re-frisk! [& params]
  (when-not @initialized
    (do
      (reg-sub ::db (fn [db _] db))
      (reset! re-frame-data {:app-db (subscribe [::db])})
      (reset! initialized true)
      (rfr/add-post-event-callback post-event-callback)
      (js/setTimeout render-re-frisk 100 (first params)))))

(defn enable-frisk! [& params]
  (when-not @initialized
    (do
      (reset! initialized true)
      (js/setTimeout render-re-frisk 100 (first params)))))

(defn add-data [key data]
  (when @initialized
    (swap! re-frame-data assoc key data)))

(defn add-in-data [keys data]
  (when @initialized
    (swap! re-frame-data assoc-in keys data)))

(defn reg-view [view subs events]
  (when (:app-db @re-frame-data)
    (do
      (swap! re-frame-data assoc-in [:views view :events] events)
      (swap! re-frame-data assoc-in [:views view :subs] (into {} (map #(hash-map % (subscribe [%])) subs)))
      (doseq [s subs]
        (swap! re-frame-data assoc-in [:subs s] (subscribe [s]))))))

(defn unmount-view [view]
  (when (:app-db @re-frame-data)
    (swap! re-frame-data update-in [:views] dissoc view)))

(reg-event-db :re-frisk/update-db (fn [db [_ value]] value))

