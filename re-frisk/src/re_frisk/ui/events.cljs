(ns re-frisk.ui.events
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as rdom]
   [clojure.string :as string]
   [re-com.core :as re-com]
   [re-frisk.ui.components.frisk :as frisk]
   [re-frisk.ui.components.components :as components]
   [re-frisk.ui.trace :as trace]
   [re-frisk.utils :as utils]
   [re-frisk.ui.subs :as subs]))

(defn event-item [_ tool-state]
  (fn [{:keys [color name app-db-diff selected? op-type indx] :as item} _]
    [:a
     {:href     "#"
      :id       (str "events-list-item" indx)
      :class    (str "list-group-item" (when selected? " active"))
      :style    (merge {:padding     5 :font-size 13 :border-left-width 2
                        :white-space :pre :width "100%"}
                       (when (and (nil? app-db-diff) (not selected?))
                         {:opacity "0.7"})
                       (when-not (string/blank? color)
                         {:border-left-color (str "#" color)}))
      :on-click (fn [event]
                  (swap! tool-state assoc :selected-event item)
                  (utils/scroll-timeline-event-item (:doc @tool-state) indx)
                  (.preventDefault event))}
     (if op-type
       [trace/trace-event-item item]
       [:span name])]))

(defn event-list-item [_ tool-state]
  (fn [{:keys [trace? op-type] :as item} _]
    (if (and trace? (not= :event op-type))
      [trace/trace-item item tool-state]
      [event-item item tool-state])))

(defn events-scroller [filtered-events tool-state]
  (reagent/create-class
   {:display-name "debugger-messages"
    :component-did-update
    (fn [this]
      (let [n (rdom/dom-node this)]
        (when (:scroll-bottom? @tool-state)
          (set! (.-scrollTop n) (.-scrollHeight n)))))
    :reagent-render
    (fn []
      [components/scroller
       {:on-scroll #(let [t (.-target %)]
                      (swap! tool-state assoc
                             :scroll-bottom?
                             (= (- (.-scrollHeight t) (.-offsetHeight t)) (.-scrollTop t))))}
       (for [item @filtered-events]
         ^{:key (str "item" (:indx item))}
         [event-list-item item tool-state])])}))

(defn events-list-view [re-frame-data tool-state]
  (let [truncate-checkbox-val (reagent/atom true)
        checkbox-trace-val    (reagent/atom false)
        text-val              (reagent/atom "")
        re-frame-events       (:events re-frame-data)
        colored-and-selected
        (reaction
         (let [clrs (:events-colors @tool-state)]
           (doall
            (map #(assoc % :selected? (= (get-in @tool-state [:selected-event :indx]) (:indx %))
                           :name (if @truncate-checkbox-val (:truncated-name %) (str (first (:event %))))
                           :color (get clrs (first (:event %))))
                 @re-frame-events))))
        traces-filtered-events
        (reaction
         (if @checkbox-trace-val
           @colored-and-selected
           (remove :trace? @colored-and-selected)))
        filtered-events
        (reaction
         (if (= @text-val "")
           @traces-filtered-events
           (filter (utils/filter-event @text-val) @traces-filtered-events)))]
    (fn []
      [re-com/v-box :size "1"
       :children
       [;events filter
        [re-com/h-box
         :children
         [[re-com/input-text :style {:height :auto :padding "0"} :width "100%"
           :model text-val :change-on-blur? false :placeholder "Filter events"
           :on-change #(reset! text-val %)]
          [components/small-button {:on-click #(reset! text-val "")} "X"]]]
        ;truncate checkbox
        [re-com/h-box :gap "5px"
         :children
         [[re-com/checkbox
           :model truncate-checkbox-val
           :on-change #(reset! truncate-checkbox-val %)
           :label "truncate"]
          [re-com/checkbox
           :model checkbox-trace-val
           :on-change #(reset! checkbox-trace-val %)
           :label "traces"]]]
        ;events
        [events-scroller filtered-events tool-state]]])))

(defn event-bar [tool-state]
  (let [evnt-key (reaction (first (get-in @tool-state [:selected-event :event])))
        subs?    (reaction (get-in @tool-state [:selected-event :subs?]))
        subs-graph-opened?  (reaction (get @tool-state :subs-graph-opened?))
        color    (reaction (if @evnt-key (@evnt-key (:events-colors @tool-state)) ""))]
    (fn []
      [re-com/h-box :align :center :style {:background-color "#4e5d6c"}
       :children
       [[re-com/label :style {:margin "4px"}
         :label (cond @evnt-key "Event"
                      @subs? "Subscriptions"
                      :else "Event / Trace")]
        (when @subs?
          [components/label-button {:on-click #(swap! tool-state update :subs-graph-opened? not)
                                    :active? @subs-graph-opened?}
           "Graph"])
        (when @evnt-key
          [re-com/h-box
           :children
           [[re-com/label :label @evnt-key :style {:margin "4px" :color "#df691a"}]
            [re-com/label :label "#" :style {:margin "4px"}]
            [:input
             {:style     {:width "60px"} :placeholder "000000" :type "text" :max-length "6"
              :value     @color
              :on-change #(swap! tool-state assoc-in
                                 [:events-colors @evnt-key]
                                 (-> % .-target .-value))}]]])]])))

(defn frisk-view [tool-state]
  (let [state-atom (reagent/atom frisk/expand-by-default)]
    (fn [_]
      (let [subs-graph-opened? (:subs-graph-opened? @tool-state)
            {:keys [event app-db-diff trace? duration-ms handler-duration-ms
                    fx-duration-ms subs?] :as item}
            (:selected-event @tool-state)]
        (when item
          (if subs?
            (if subs-graph-opened?
              [subs/event-subs-graph-container item]
              [trace/subs-details item])
            [frisk/Root (if trace?
                          item
                          (merge {:event       event
                                  :app-db-diff app-db-diff}
                                 (when duration-ms
                                   {:trace {:duration         duration-ms
                                            :handler-duration handler-duration-ms
                                            :fx-duration      fx-duration-ms}})))
             0 state-atom]))))))