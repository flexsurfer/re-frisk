(ns re-frisk.core
  (:require [reagent.core :as r]
            [re-frisk.ui :as ui]
            [datafrisk.core :as f]
            [re-frame.core :refer [def-sub subscribe]])
  (:import [goog.events EventType]))

(defonce re-frame-data (r/atom {}))

(defn render-re-frisk []
  (let [div (js/document.createElement "div")]
    (js/document.body.appendChild div)
    (r/render [ui/re-frisk-shell [f/FriskInline @re-frame-data]] div)))

(defn- enable-re-frisk! []
  (when-not (:app-db @re-frame-data)
    (do
      (def-sub :db (fn [db _] db))
      (reset! re-frame-data {:views (r/atom {})
                             :subs (r/atom {})
                             :app-db (subscribe [:db])})
      (js/setTimeout render-re-frisk 100))))

(defn reg-view [view subs events]
  (do
    (enable-re-frisk!)
    (swap! (:views @re-frame-data) assoc-in [view :events] events)
    (swap! (:views @re-frame-data) assoc-in [view :subs] (into {} (map #(hash-map % (subscribe [%])) subs)))
    (doseq [s subs]
      (swap! (:subs @re-frame-data) assoc-in [s] (subscribe [s])))))

(defn unmount-view [view]
  (when (:app-db @re-frame-data)
    (swap! (:views @re-frame-data) dissoc view)))

