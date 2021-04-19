(ns re-frisk.ui.timeline
  (:require-macros [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :refer [reaction]])
  (:require [re-frisk.ui.components.components :as components]
            [re-frisk.utils :as utils]))

(def timeout (atom nil))

(defn change-zoom [tool-state inc?]
  (let [curr (:timeline-zoom @tool-state)]
    (when (or (and (not inc?) (>= curr 0.05))
              (and inc? (< curr 12)))
      (swap! tool-state update :timeline-zoom
             #(if inc?
                (+ % (if (< curr 0.1) 0.01 curr))
                (- % (if (<= curr 0.1) 0.01 (/ curr 2)))))
      (when-let [indx (get-in @tool-state [:selected-event :indx])]
        (when @timeout (js/clearTimeout @timeout))
        (reset! timeout (js/setTimeout #(utils/scroll-timeline-event-item (:doc @tool-state) indx) 500))))))

(defn buttons [tool-state]
  [:div {:style {:position :absolute :top 70 :left 0}}
   [:div {:style {:display :flex :flex-direction :row}}
    [components/small-button {:on-click #(change-zoom tool-state false)} "-"]
    [:div {:style {:width 5}}]
    [components/small-button {:on-click #(change-zoom tool-state true)} "+"]]])

(defn timeline [re-frame-data tool-state]
  (let [indx     (reaction (get-in @tool-state [:selected-event :indx]))
        mult     (reaction (:timeline-zoom @tool-state))
        selected (reaction (doall (map #(assoc % :selected? (= @indx (:indx %))
                                                 :width (let [width (* (:duration %) @mult)]
                                                          (int (if (< width 2) 2 width)))
                                                 :color (if (= @indx (:indx %))
                                                          "#df691a"
                                                          (if (= (:op-type %) :event) :blue "#219653")))
                                       @(:events re-frame-data))))]
    (fn []
      (let [mult     @mult
            devi     (/ 200 mult)
            events   @selected
            all-time (int (+ (- (:end (last events)) (:start (first events))) devi))]
        [:div {:style {:height           100 :width "100%" :overflow-x :auto :overflow-y :hidden
                       :background-color "#f3f3f3" :max-height 100}}
         [:div {:style {:display  :flex :flex-direction :row :width "100%" :height 100
                        :position :relative :overflow-x :auto}}
          (for [x (range (int (/ all-time devi)))]
            ^{:key (str "timelime-time-item" x)}
            [:div {:style {:width             (* devi mult) :min-width (* devi mult) :height 90
                           :border-left-width 1 :border-left-color :gray :border-left-style :solid
                           :color             :gray :font-size 10}}
             (str (int (* x devi)) " ms")])
          (for [{:keys [indx color width position] :as item} events]
            ^{:key (str "timeline-event-item" indx)}
            [:div {:id    (str "timeline-event-item" indx)
                   :on-click (fn [event]
                               (swap! tool-state assoc :selected-event item)
                               (utils/scroll-event-list-item (:doc @tool-state) indx)
                               (.preventDefault event))
                   :style {:left   (int (* position mult)) :position :absolute :width width :min-width width
                           :top    20
                           :height 70 :background-color color :opacity 0.5}}])]
         [buttons tool-state]]))))

(defn timeline-visibility [re-frame-data tool-state]
  (fn []
    (when (:timeline-opened? @tool-state)
      [timeline re-frame-data tool-state])))