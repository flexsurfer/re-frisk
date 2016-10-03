(ns re-frisk.debugger
  (:require [hiccups.runtime :as h]
            [re-frisk.data :refer [re-frame-data]]
            [reagent.core :as r]
            [datafrisk.core :as f])
  (:require-macros [hiccups.core :refer [html]]
                   [reagent.ratom :refer [reaction]]))

(defonce deb-data (r/atom {:w-c true}))

(defn debugger-shell []
  (let [expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        state-atom (r/atom expand-by-default)
        ;; i have two issues with the cljs data structures after passing it between windows
        ;; first problem frisk library doesn't colored keywords right
        ;; second - reagent doesn't see ratom changes, this hack to avoid second issue
        _ (js/setInterval #(swap! state-atom assoc :t (rand)) 100)]
    (fn []
      [:div {:style {:backgroundColor "#FAFAFA"
                     :fontFamily "Consolas,Monaco,Courier New,monospace"
                     :fontSize "12px"
                     :height "100%"
                     :width "100%"
                     :top "0"
                     :left "0"
                     :z-index "1000"
                     :position "absolute"
                     :overflow "auto"}}
       [:div
        (map-indexed (fn [id x]
                       ^{:key id} [f/Root x id state-atom]) [(:data @deb-data)])]])))

(defn run [data]
  (swap! deb-data assoc :data data)
  (when-not (:rendered @deb-data)
    (let [div (js/document.createElement "div")]
      (js/document.body.appendChild div)
      (swap! deb-data assoc :rendered true)
      (r/render [debugger-shell] div))))

(defn debugger-page [src]
  [:html
   [:head
    [:title "re-frisk debugger"]
    [:meta {:charset "UTF-8"}]
    [:meta
     {:content "width=device-width, initial-scale=1", :name "viewport"}]]
   [:body  {:style {:margin "0" :padding "0"}};}}
    [:div#app {:style {:height "100%" :width "100%"}}
     [:h2 "re-frisk debugger"]
     [:p "ENJOY!"]]]
   [:script {:type "text/javascript", :src src}]])

(defn open-debugger-window []
  (let [w (js/window.open "" "Debugger" "width=500,height=400,resizable=yes,scrollbars=yes,status=no,directories=no,toolbar=no,menubar=no")
        d (.-document w)]
    (swap! deb-data assoc :w w)
    (.open d)
    (.write d (html (debugger-page (:p @deb-data))))
    (aset w "onload" #(do
                       (swap! deb-data assoc :w-c false)
                       ((:f @deb-data) (:w @deb-data) @re-frame-data)))
    (aset w "onunload" #(swap! deb-data assoc :w-c true))
    (.close d)))

(defn register [p f]
  (swap! deb-data assoc :p p)
  (swap! deb-data assoc :f f))

