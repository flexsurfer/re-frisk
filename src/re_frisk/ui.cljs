(ns re-frisk.ui
  (:require [reagent.core :as r]
            [goog.events :as e])
  (:import [goog.events EventType]))

;https://github.com/borkdude/draggable-button-in-reagent

(defonce draggable (r/atom {:x 0 :y 0}))

(defn get-client-rect [evt]
  (let [r (.getBoundingClientRect (.-target evt))]
    {:left (.-left r), :top (.-top r)}))

(defn mouse-move-handler [offset]
  (fn [evt]
    (let [x (- (.-clientX evt) (:x offset))
          y (- (.-clientY evt) (:y offset))]
      (reset! draggable {:x x :y y}))))

(defn mouse-up-handler [on-move]
  (fn me [evt]
    (e/unlisten js/window EventType.MOUSEMOVE on-move)))

(defn mouse-down-handler [e]
  (let [{:keys [left top]} (get-client-rect e)
        offset             {:x (- (.-clientX e) left)
                            :y (- (.-clientY e) top)}
        on-move            (mouse-move-handler offset)]
    (e/listen js/window EventType.MOUSEMOVE on-move)
    (e/listen js/window EventType.MOUSEUP (mouse-up-handler on-move))))

(defn re-frisk-shell [frisk]
  (fn []
    [:div {:style {:position "fixed"
                   :left (str (:x @draggable) "px")
                   :top (str (:y @draggable) "px")
                   :z-index 999}}
     [:div {:style {:width "80px"
                    :height "20px"
                    :background-color "#CCCCCC"
                    :cursor "move"}
            :on-mouse-down mouse-down-handler} "re-frisk"]
     frisk]))