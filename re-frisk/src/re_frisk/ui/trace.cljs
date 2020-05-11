(ns re-frisk.ui.trace
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frisk.ui.components.components :as components]
            [re-frisk.utils :as utils]
            [re-frisk.ui.components.colors :as colors]))

(defn subs-count [label val color duration-ms]
  [:div {:style {:display        :flex :align-items :center :margin-right 10
                 :flex-direction :column}}
   label
   [:div {:style {:display :flex :align-items :center}}
    [:div {:style {:display       :flex :margin 5 :padding-left 4 :padding-right 4 :background-color color
                   :border-radius 4 :color :white}}
     val]
    duration-ms]])

(defn subs-details [{:keys [duration-ms run-count created-count disposed-count subs render-count
                            run-duration-ms created-duration-ms disposed-duration-ms render-duration-ms
                            created-count-cached created-duration-cached-ms]}]
  [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                 :padding 8 :flex-direction :column}}
   [:div {:style {:margin-top 8 :margin-bottom 8}} "Total time: " duration-ms]
   [:div {:style {:display :flex :flex-direction :row :margin-bottom 8}}
    [subs-count "Created" created-count colors/sub-create created-duration-ms]
    [subs-count "Cached" created-count-cached colors/sub-create-cached created-duration-cached-ms]
    [subs-count "Run" run-count colors/sub-run run-duration-ms]
    [subs-count "Disposed" disposed-count colors/sub-dispose disposed-duration-ms]
    [subs-count "Render" render-count colors/render render-duration-ms]]
   [components/scroller
    (for [{:keys [id duration-ms op-type operation cached?]} subs]
      ^{:key (str "item" id)}
      [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between}}
       [:div {:style {:display :flex :min-width 100
                      :color (get colors/sub-colors (if cached? :sub/create-cached op-type))}}
        (str op-type)]
       [:div {:style {:display :flex :min-width 100}} (str operation)]
       [:div duration-ms]])]])

(defn subs-item [{:keys [duration-ms run-count created-count disposed-count render-count]}]
  [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between}}
   [:div "subs"]
   [:div {:style {:display :flex :flex-direction :row}}
    duration-ms
    (when-not (zero? created-count)
      [:div {:style {:background-color colors/sub-create :margin-left 5}}
       created-count])
    [:div {:style {:background-color colors/sub-run :margin-left 5}}
     run-count]
    (when-not (zero? disposed-count)
      [:div {:style {:background-color colors/sub-dispose :margin-left 5}}
       disposed-count])
    (when-not (zero? render-count)
      [:div {:style {:background-color colors/render :margin-left 5}}
       render-count])]])

(defn trace-item [{:keys [op-type]} tool-state]
  ;(when-not (= op-type :re-frame.router/fsm-trigger)
  (fn [{:keys [selected? op-type indx] :as item} _]
    [:a
     {:href     "#"
      :id       (str "events-list-item" indx)
      :class    (str "list-group-item" (when selected? " active"))
      :style    {:padding           0 :font-size 11 :opacity "0.7"
                 :border-left-width 2 :white-space :pre :width "100%"}
      :on-click (fn [event]
                  (swap! tool-state assoc :selected-event item)
                  (utils/scroll-timeline-event-item (:doc @tool-state) indx)
                  (.preventDefault event))}
     (if (= :subs op-type)
       [subs-item item]
       [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between}}
        [:div (str op-type)]])]))

(defn trace-event-item [{:keys [name duration-ms]}]
  [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between :align-items :center}}
   [:div {:style {:display :flex :overflow :hidden}} name]
   [:div {:style {:display :flex :font-size 11 :margin-left 5}}
    duration-ms]])