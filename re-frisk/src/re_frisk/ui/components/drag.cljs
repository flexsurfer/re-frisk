(ns re-frisk.ui.components.drag
  (:require [goog.events :as goog-events]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent])
  (:import [goog.events EventType]))

(defonce draggable (reagent/atom {}))

(defn mouse-move-handler [evt]
  (swap! draggable assoc :x (- (.-clientX evt) (:offset @draggable))))

(defn mouse-up-handler [evt]
    (goog-events/unlisten js/window EventType.MOUSEMOVE mouse-move-handler)
    (swap! draggable dissoc :offset))

(defn mouse-down-handler [evt]
  (swap! draggable assoc :offset (- (.-clientX evt) (.-left (.getBoundingClientRect (.-target evt)))))
  (goog-events/listen js/window EventType.MOUSEMOVE mouse-move-handler))

(goog-events/listen js/window EventType.MOUSEUP mouse-up-handler)