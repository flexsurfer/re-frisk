(ns re-frisk.ui.trace
  (:require-macros [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :refer [reaction]])
  (:require [re-frisk.ui.components.components :as components]
            [re-frisk.utils :as utils]
            [re-frisk.ui.components.colors :as colors]))

(defn subs-count [label val color duration-ms]
  [:div {:style {:display        :flex :align-items :center :margin-right 10
                 :flex-direction :column}}
   label
   [:div {:style {:display :flex :align-items :center}}
    (when val
      [:div {:style {:display       :flex :margin 5 :padding-left 4 :padding-right 4 :background-color color
                     :border-radius 4 :color :white}}
       val])
    duration-ms]])

(defn subs-details [{:keys [duration-ms run-count created-count disposed-count subs render-count force-count
                            run-duration-ms created-duration-ms disposed-duration-ms render-duration-ms
                            created-count-cached created-duration-cached-ms force-duration-ms
                            create-class-count create-class-duration-ms shoud-update-count shoud-update-duration-ms]}]
  [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                 :padding 8 :flex-direction :column}}
   [:div {:style {:display :flex :flex-direction :row :margin-bottom 8 :border-bottom "solid 1px #CCCCCC"}}
    [subs-count "Total time" nil nil duration-ms]
    [:div {:style {:width 1 :height "90%" :background-color "#CCCCCC" :margin-right 10}}]
    (when (pos? created-count)
      [subs-count "Created" created-count colors/sub-create created-duration-ms])
    (when (pos? created-count-cached)
      [subs-count "Cached" created-count-cached colors/sub-create-cached created-duration-cached-ms])
    (when (pos? run-count)
      [subs-count "Run" run-count colors/sub-run run-duration-ms])
    (when (pos? disposed-count)
      [subs-count "Disposed" disposed-count colors/sub-dispose disposed-duration-ms])
    (when (or (pos? render-count) (pos? create-class-count) (pos? force-count))
      [:div {:style {:width 1 :height "90%" :background-color "#CCCCCC" :margin-left 10 :margin-right 10}}])
    (when (pos? create-class-count)
      [subs-count "Create class" create-class-count colors/sub-create-cached create-class-duration-ms])
    (when (pos? force-count)
      [subs-count "Force Update" force-count colors/force-update force-duration-ms])
    (when (pos? shoud-update-count)
      [subs-count "Should update" shoud-update-count colors/sub-create-cached shoud-update-duration-ms])
    (when (pos? render-count)
      [subs-count "Render" render-count colors/render render-duration-ms])]
   [components/scroller
    (for [{:keys [id duration-ms op-type operation cached?]} (sort-by :duration > subs)]
      ^{:key (str "item" id)}
      [:div {:style {:display :flex :flex-direction :row :flex 1}}
       [:div {:style {:display :flex :min-width 100
                      :color (get colors/sub-colors (if cached? :sub/create-cached op-type))}}
        (str op-type)]
       [:div {:style {:display :flex :min-width 100 :width "100%"}} (str operation)]
       [:div {:style {:display :flex :min-width 60}} duration-ms]])]])

(defn subs-item [{:keys [duration-ms run-count created-count disposed-count render-count force-count]}]
  [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between}}
   [:div "reagent"]
   [:div {:style {:display :flex :flex-direction :row}}
    duration-ms
    (when-not (zero? created-count)
      [:div {:style {:background-color colors/sub-create :margin-left 5}}
       created-count])
    (when-not (zero? run-count)
      [:div {:style {:background-color colors/sub-run :margin-left 5}}
       run-count])
    (when-not (zero? disposed-count)
      [:div {:style {:background-color colors/sub-dispose :margin-left 5}}
       disposed-count])
    (when-not (zero? render-count)
      [:div {:style {:background-color colors/render :margin-left 5}}
       render-count])
    (when-not (zero? force-count)
      [:div {:style {:background-color colors/force-update :margin-left 5}}
       force-count])]])

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

(defn trace-event-item [{:keys [name duration-ms effects]}]
  [:div {:style {:display :flex :flex-direction :row :flex 1 :justify-content :space-between :align-items :center}}
   [:div {:style {:display :flex :overflow :hidden}} name]
   [:div {:style {:display :flex :flex-direction :row}}
    [:div {:style {:display :flex :font-size 11 :margin-left 5}}
     duration-ms]
    (when-let [cnt (count effects)]
      (when (pos? cnt)
        [:div {:style {:background-color colors/effect :margin-left 5}}
         cnt]))]])
