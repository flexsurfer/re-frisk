(ns re-frisk.ui.views
  (:require-macros [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :refer [reaction]])
  (:require
   [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
   [re-com.core :as re-com]
   [re-frisk.ui.events :as events]
   [re-frisk.ui.components.splits :as splits]
   [re-frisk.ui.components.frisk :as frisk]
   [re-frisk.utils :as utils]
   [re-frisk.ui.timeline :as timeline]
   [re-frisk.ui.components.components :as components]
   [re-frisk.ui.subs :as subs]
   [re-frisk.ui.components.github :as github]
   [re-frisk.ui.stat :as stat]
   [re-frisk.ui.reagent-views :as reagent-views]))

(defn subs-view [subs checkbox-sorted-val]
  (let [state-atom (reagent/atom frisk/expand-by-default)]
    (fn [_]
      [frisk/Root (utils/sort-map @subs @checkbox-sorted-val checkbox-sorted-val) 0 state-atom])))

(defn watch-item [frisk-data item watchers]
  [re-com/v-box
   :children
   [[re-com/h-box :style {:background-color "#4e5d6c"}
     :children
     [[:div (str "Watching " item)]
      [re-com/gap :size "10px"]
      [components/label-button {:on-click #(swap! watchers disj item)
                                :active? false}
       "Stop"]]]
    [re-com/v-box :style {:padding 10 :color "#444444"}
     :children
     [[frisk/Root-Simple frisk-data]]]]])

(defn app-db-view [app-db tool-state]
  (let [state-atom          (reagent/atom frisk/expand-by-default)
        watchers            (reagent/atom #{})
        checkbox-sorted-val (reagent/atom true)]
    (fn [_]
      [re-com/v-box :size "1"
       :children
       [(when (seq @watchers)
          (let [app-db-derefed @app-db]
            [components/scroller
             {:style {:background-color "#f3f3f3"}}
             (for [watcher @watchers]
               ^{:key watcher}
               [watch-item (get-in app-db-derefed watcher) watcher watchers])]))
        [re-com/h-box
         :children
         [[re-com/label :label "app-db"]
          [re-com/gap :size "20px"]
          [re-com/checkbox
           :model checkbox-sorted-val
           :on-change (utils/on-change-sort app-db checkbox-sorted-val :re-frisk-sorted)
           :label "sort"]
          (when (:app-db-delta-error @tool-state)
            [re-com/label :label "update error" :style {:margin-left "4px" :color "#df691a"}])
          [re-com/gap :size "48px"]
          (when-let [filter (get-in @state-atom [:data-frisk 0 :filter])]
            (when (contains? (first filter) :expr)
              [components/label-button {:on-click #(swap! watchers conj (mapv :expr filter))
                                        :active? false}
               "Watch"]))]]
        [frisk/Root (utils/sort-map @app-db @checkbox-sorted-val checkbox-sorted-val) 0 state-atom]]])))

(defn frisks-view [re-frame-data tool-state doc]
  (let [subs-checkbox-sorted-val (reagent/atom true)
        open-event-split?        (reaction (boolean (get @tool-state :selected-event)))]
    (fn [_ _ _]
      [re-com/v-box :size "1" :style {:padding "0"}
       :children [[re-com/h-box :style {:background-color "#4e5d6c"}
                   :children
                   [[re-com/label :label "Active subscriptions"]
                    [re-com/gap :size "20px"]
                    [re-com/checkbox
                     :model subs-checkbox-sorted-val
                     :on-change (utils/on-change-sort (:subs re-frame-data) subs-checkbox-sorted-val [[:re-frisk-sorted] []])
                     :label "sort"]
                    (when (:subs-delta-error @tool-state)
                      [re-com/label :label "update error" :style {:margin-left "4px" :color "#df691a"}])]]
                  [splits/v-split :size "1" :initial-split "0" :style {:padding "0" :margin "0"} :document doc
                   ;; SUBS
                   :panel-1 [subs-view (:subs re-frame-data) subs-checkbox-sorted-val]
                   :panel-2 [splits/v-split :size "1" :initial-split "100" :style {:padding "0" :margin "0"}
                             :document doc :open-bottom-split? @open-event-split?
                             :panel-1 [re-com/v-box :size "1" :style {:background-color "#4e5d6c"}
                                       :children
                                       ;; APP-DB
                                       [[app-db-view (:app-db re-frame-data) tool-state]
                                        [events/event-bar tool-state]]]
                             ;; EVENT
                             :panel-2 [events/frisk-view tool-state]]]]])))

(defn controls [re-frame-data tool-state]
  (let [{:keys [timeline-opened? paused? graph-opened? stat-opened? views-opened?]} @tool-state]
    [re-com/h-box :style {:background-color "#4e5d6c"} :align :center
     :children
     [[components/label-button {:on-click #(swap! tool-state update :paused? not)
                                :active? paused?}
       (if paused? "Resume" "Pause")]
      [re-com/gap :size "5px"]
      [components/label-button
       {:on-click #(do (reset! (:events re-frame-data) [])
                       (swap! tool-state dissoc :selected-event))
        :active? false}
       "Clear"]
      [re-com/gap :size "1"]
      [components/label-button {:on-click #(do
                                             (swap! tool-state assoc :graph-opened? false)
                                             (swap! tool-state assoc :stat-opened? false)
                                             (swap! tool-state assoc :views-opened? false)
                                             (swap! tool-state update :timeline-opened? not))
                                :active? timeline-opened?}
       "Timeline"]
      [re-com/gap :size "5px"]
      [components/label-button {:on-click #(do
                                             (swap! tool-state assoc :timeline-opened? false)
                                             (swap! tool-state assoc :stat-opened? false)
                                             (swap! tool-state assoc :views-opened? false)
                                             (swap! tool-state update :graph-opened? not))
                                :active? graph-opened?}
       "Subs"]
      [re-com/gap :size "5px"]
      [components/label-button {:on-click #(do
                                             (swap! tool-state assoc :timeline-opened? false)
                                             (swap! tool-state assoc :graph-opened? false)
                                             (swap! tool-state assoc :stat-opened? false)
                                             (swap! tool-state update :views-opened? not))
                                :active? views-opened?}
       "Views"]
      [re-com/gap :size "5px"]
      [components/label-button {:on-click #(do
                                             (swap! tool-state assoc :timeline-opened? false)
                                             (swap! tool-state assoc :graph-opened? false)
                                             (swap! tool-state assoc :views-opened? false)
                                             (swap! tool-state update :stat-opened? not))
                                :active? stat-opened?}
       "Stat"]
      [re-com/gap :size "15px"]
      [github/link]
      [re-com/gap :size "5px"]]]))

(defn main-view [re-frame-data tool-state & [doc]]
  (let [open-graph-split? (reaction (get @tool-state :graph-opened?))
        open-stat-split? (reaction (get @tool-state :stat-opened?))
        open-views-split? (reaction (get @tool-state :views-opened?))]
    (fn []
      [splits/v-split :height "100%" :initial-split "0" :document doc :style {:padding "0" :margin "0"}
       :open-bottom-split? (or @open-graph-split? @open-stat-split? @open-views-split?)
       :close-bottom-split? (not (or @open-graph-split? @open-stat-split? @open-views-split?))
       :panel-1
       (if @open-views-split?
         [reagent-views/views re-frame-data]
         (if @open-stat-split?
           [stat/stat re-frame-data]
           [subs/subs-visibility re-frame-data tool-state]))
       :panel-2
       [re-com/v-box :size "1"
        :children
        [[timeline/timeline-visibility re-frame-data tool-state]
         [controls re-frame-data tool-state]
         [splits/h-split :size "1" :initial-split "25" :document doc
          ;;EVENTS
          :panel-1 [events/events-list-view re-frame-data tool-state]
          ;;MAIN (subs, app-db, event)
          :panel-2 [frisks-view re-frame-data tool-state doc]]]]])))