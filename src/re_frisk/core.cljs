(ns re-frisk.core
  (:require [reagent.core :as r]
            [re-frisk.ui :as ui]
            [datafrisk.core :as f]
            [re-frame.core :refer [reg-sub subscribe]])
  (:import [goog.events EventType]))

(defonce re-frame-data (r/atom {}))

(defn- render-re-frisk [params]
  (let [div (js/document.createElement "div")
        h (:height params)
        w (:width params)
        style {}
        style (merge style (when h {:height h :max-height h :overflow "auto"}))
        style (merge style (when w {:width w :max-width w :overflow "auto"}))]
    (js/document.body.appendChild div)
    (r/render [ui/re-frisk-shell [:div {:style style}
                                  [f/FriskInline @re-frame-data]] params] div)))

(defn enable-re-frisk! [& params]
  (when-not (:app-db @re-frame-data)
    (do
      (reg-sub :db (fn [db _] db))
      (reset! re-frame-data {:views (r/atom {})
                             :subs (r/atom {})
                             :app-db (subscribe [:db])})
      (js/setTimeout render-re-frisk 100 (first params)))))

(defn enable-frisk! [& params]
  (when-not (:app-db @re-frame-data)
    (js/setTimeout render-re-frisk 100 (first params))))

(defn reg-view [view subs events]
  (do
    (enable-re-frisk! {:x 0 :y 0})
    (swap! (:views @re-frame-data) assoc-in [view :events] events)
    (swap! (:views @re-frame-data) assoc-in [view :subs] (into {} (map #(hash-map % (subscribe [%])) subs)))
    (doseq [s subs]
      (swap! (:subs @re-frame-data) assoc-in [s] (subscribe [s])))))

(defn unmount-view [view]
  (when (:app-db @re-frame-data)
    (swap! (:views @re-frame-data) dissoc view)))

(defn add-data [key data]
  (swap! re-frame-data assoc key data))

