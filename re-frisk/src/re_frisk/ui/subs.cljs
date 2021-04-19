(ns re-frisk.ui.subs
  (:require [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [re-frisk.subs-graph :as subs-graph]))

(defn global-subs-graph-container []
  (reagent/create-class
   {:component-did-mount #(subs-graph/create)
    :component-will-unmount #(subs-graph/destroy)
    :reagent-render
    (fn []
      [:div {:id "global-subs-graph-container"
             :style {:height "100%" :width "100%" :background-color "#f3f3f3"}}])}))

(defn subs-visibility [re-frame-data tool-state]
  (when (:graph-opened? @tool-state)
    [global-subs-graph-container]))

(defonce timeout (atom nil))

(defn event-subs-graph-container [item]
  (fn [item]
    (when @timeout (js/clearTimeout @timeout))
    (reset! timeout (js/setTimeout #(subs-graph/create-event-subs item) 200))
    [:div {:id "event-subs-graph-container"
           :style {:height "100%" :width "100%" :background-color "#f3f3f3"}}]))