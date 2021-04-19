(ns re-frisk.ui.events
  (:require-macros [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :refer [reaction]])
  (:require
   [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
   [re-frisk.inlined-deps.reagent.v1v0v0.reagent.dom :as rdom]
   [clojure.string :as string]
   [re-com.core :as re-com]
   [re-frisk.ui.components.frisk :as frisk]
   [re-frisk.ui.components.components :as components]
   [re-frisk.ui.trace :as trace]
   [re-frisk.utils :as utils]
   [re-frisk.ui.subs :as subs]
   [re-frisk.ui.components.colors :as colors]))

(defn event-item [_ tool-state checkbox-trace-val]
  (fn [{:keys [color name app-db-diff selected? op-type indx] :as item} _]
    [:a
     {:href     "#"
      :id       (str "events-list-item" indx)
      :class    (str "list-group-item" (when selected? " active"))
      :style    (merge {:padding     5 :font-size 13 :border-left-width 2
                        :padding-right 0
                        :white-space :pre :width "100%"}
                       (when (and (nil? app-db-diff) (not selected?))
                         {:opacity "0.7"})
                       (when-not (string/blank? color)
                         {:border-left-color (str "#" color)}))
      :on-click (fn [event]
                  (swap! tool-state assoc :selected-event item)
                  (utils/scroll-timeline-event-item (:doc @tool-state) indx)
                  (.preventDefault event))}
     (if (and op-type checkbox-trace-val)
       [trace/trace-event-item item]
       [:span name])]))

(defn event-list-item [_ tool-state checkbox-trace-val]
  (fn [{:keys [trace? op-type] :as item} _]
    (if (and trace? (not= :event op-type))
      [trace/trace-item item tool-state]
      [event-item item tool-state checkbox-trace-val])))

(defn events-scroller [filtered-events tool-state _]
  (reagent/create-class
   {:display-name "re_frisk.debugger-messages"
    :component-did-update
    (fn [this]
      (let [n (rdom/dom-node this)]
        (when (:scroll-bottom? @tool-state)
          (set! (.-scrollTop n) (.-scrollHeight n)))))
    :reagent-render
    (fn [_ _ checkbox-trace-val]
      [components/scroller
       {:on-scroll #(let [t (.-target %)]
                      (swap! tool-state assoc
                             :scroll-bottom?
                             (= (- (.-scrollHeight t) (.-offsetHeight t)) (.-scrollTop t))))}
       (for [item @filtered-events]
         ^{:key (str "item" (:indx item) checkbox-trace-val)}
         [event-list-item item tool-state checkbox-trace-val])])}))

(defn events-list-view [re-frame-data tool-state]
  (let [truncate-checkbox-val (reagent/atom true)
        checkbox-trace-val    (reagent/atom false)
        text-val              (reagent/atom "")
        max-text-val          (reagent/atom "")
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
        max-traces-filtered-events
        (reaction
         (if (> (int @max-text-val) 0)
           (take-last (int @max-text-val) @traces-filtered-events)
           @traces-filtered-events))
        filtered-events
        (reaction
         (if (= @text-val "")
           @max-traces-filtered-events
           (filter (utils/filter-event @text-val) @max-traces-filtered-events)))
        sorted-events
        (reaction (sort-by :indx @filtered-events))]
    (fn []
      [re-com/v-box :size "1"
       :children
       [;events filter
        [re-com/h-box
         :children
         [[re-com/box :size "1"
           :child
           [:input {:type "text"
                    :style {:height :auto :padding "0" :width "100%"}
                    :value @text-val
                    :placeholder "Filter events"
                    :on-change #(reset! text-val (-> % .-target .-value))}]]
          [components/small-button {:on-click #(reset! text-val "") :active? false} "X"]]]
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
           :label "traces"]
          [re-com/gap :size "100%"]
          [re-com/h-box
           :children
           [[:input {:type "text"
                     :style {:height "20px" :padding "0" :width "30px"}
                     :value @max-text-val
                     :placeholder "max"
                     :on-change #(reset! max-text-val (-> % .-target .-value))}]
            [components/small-button {:on-click #(reset! max-text-val nil) :active? false} "X"]]]]]
        ;events
        [events-scroller sorted-events tool-state @checkbox-trace-val]]])))

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
           [[re-com/label :label @evnt-key :style {:margin "4px"}]
            [re-com/label :label "#" :style {:margin "4px"}]
            [:input
             {:style     {:width "60px"} :placeholder "000000" :type "text" :max-length "6"
              :value     @color
              :on-change #(swap! tool-state assoc-in
                                 [:events-colors @evnt-key]
                                 (-> % .-target .-value))}]]])]])))

(defn event-count [label val color duration-ms]
  [:div {:style {:display        :flex :align-items :center :margin-right 10
                 :flex-direction :row}}
   label ":"
   [:div {:style {:display :flex :align-items :center}}
    (if (string/blank? val)
      [:div {:style {:width 10}}]
      [:div {:style {:display       :flex :margin 5 :padding-left 4 :padding-right 4 :background-color color
                     :border-radius 4 :color :white}}
       val])
    duration-ms]])

(defn event-content [_]
  (let [state-atom (reagent/atom frisk/expand-by-default)]
    (fn [item]
      (let [{:keys [event app-db-diff trace? duration-ms handler-duration-ms
                    fx-duration-ms effects coeffects]}
            item]
        [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                       :padding 8 :flex-direction :column}}
         (when duration-ms
           [:div {:style {:display :flex :flex-direction :row :align-items :center
                          :margin-bottom 10 :border-bottom "solid 1px #CCCCCC"}}
            [:div {:style {:margin-right 10}} "Total time: " duration-ms]
            [:div {:style {:margin-right 10}} " | "]
            [event-count "Handler" "" colors/render handler-duration-ms]
            [:div {:style {:margin-right 10}} " | "]
            [event-count "Effects" (count effects) colors/effect fx-duration-ms]])
         [frisk/Root (if trace?
                       item
                       (merge {:event event}
                              (if duration-ms
                                {:effects (cond-> effects
                                            app-db-diff
                                            (assoc :db app-db-diff))
                                 :coeffects coeffects}
                                {:app-db-diff app-db-diff})))
          0 state-atom]]))))

(defn frisk-view [tool-state]
  (let []
    (fn [_]
      (let [subs-graph-opened? (:subs-graph-opened? @tool-state)
            {:keys [subs?] :as item}
            (:selected-event @tool-state)]
        (when item
          (if subs?
            (if subs-graph-opened?
              [subs/event-subs-graph-container item]
              [trace/subs-details item])
            [event-content item]))))))