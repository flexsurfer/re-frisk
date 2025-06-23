(ns re-frisk.ui.components.drag
  (:require [goog.events :as goog-events]
            [reagent.core :as reagent])
  (:import [goog.events EventType]))

(defonce draggable (reagent/atom {}))

(defn mouse-move-handler [evt]
  (swap! draggable (fn [{:keys [offset] :as state}]
                     (assoc state :width (- (.-innerWidth js/window)
                                            (.-clientX evt)
                                            ;; Toggle button is 30px outside of the panel.
                                            (+ 30 offset))))))

(defn mouse-up-handler [evt]
    (goog-events/unlisten js/window EventType.MOUSEMOVE mouse-move-handler)
    (swap! draggable dissoc :offset))

(defn mouse-down-handler [evt]
  ;; Offset is the distance from pointer to the left corner of the toggle button,
  ;; it is used on the move handler to get the full width of the panel from move coordinate.
  (swap! draggable assoc :offset (- (.-left (.getBoundingClientRect (.-target evt))) (.-clientX evt)))
  (goog-events/listen js/window EventType.MOUSEMOVE mouse-move-handler))

(goog-events/listen js/window EventType.MOUSEUP mouse-up-handler)
