(ns re-frisk.ui.stat
  (:require [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [re-frisk.ui.components.components :as components]
            [re-frisk.utils :as utils]
            [re-frisk.clipboard :as clipboard]))

(def current-reg (reagent/atom :event))

(defn reg-button [key data]
  (let [cnt (count (get data key))
        active (= key @current-reg)]
    [:div {:style (cond-> {:display       :flex :margin 5 :padding-left 4 :padding-right 4
                           :cursor :pointer :border-radius 4 :color (if active :white :gray)}
                    active
                    (assoc :background-color "#df691a")
                    (not active)
                    (assoc :border-width 1 :border-color :gray :border-style :solid))
           :on-click #(reset! current-reg key)}
     (str key " (" cnt ")")]))

(def show-copied (reagent/atom nil))

(defn share-button [re-frame-data]
  (let [data @(:stat re-frame-data)]
    [:div
     [:div {:style    {:display      :flex :margin 5 :padding-left 4 :padding-right 4
                       :cursor       :pointer :border-radius 4 :color "#df691a"
                       :border-width 1 :border-color "#df691a" :border-style :solid}
            :on-click (fn []
                        (reset! show-copied true)
                        (js/setTimeout #(reset! show-copied false) 2000)
                        (clipboard/copy-to-clip (str "*stats for my re-frame project by re-frisk* \n"
                                                     "db: " (count  @(:app-db re-frame-data)) " | "
                                                     "fx: " (count (get data :fx)) " | "
                                                     "cofx: " (count (get data :cofx)) " | "
                                                     "event: " (count (get data :event)) " | "
                                                     "sub: " (count (get data :sub))
                                                     " (" (count @(:subs re-frame-data)) ")")))}
      "share"]
     (when @show-copied
       [:div {:style {:position :absolute :background-color :white :border-radius 4}} "copied"])]))

(def sort-by-key (reagent/atom :cnt))
(defn sort-fn [key item]
  (let [{:keys [cnt ms]} (second item)]
    (if (= key :cnt)
      cnt
      (if (and (pos? ms) (pos? cnt)) (/ ms cnt) 0))))

(defn stat [re-frame-data]
  (let [data @(:stat re-frame-data)]
    [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                   :padding 8 :flex-direction :column}}
     [:div {:style {:display :flex :flex-direction :row :margin-bottom 8}}
      [reg-button :fx data]
      [reg-button :cofx data]
      [reg-button :event data]
      [reg-button :sub data]
      [:div {:style {:display :flex  :flex 1}}]
      [share-button re-frame-data]]
     [:div {:style {:display :flex :flex-direction :row :border-bottom "solid 1px #000000"}}
      [:div {:style {:width "100%"}} @current-reg]
      [:div {:style {:width 100 :cursor :pointer} :on-click #(reset! sort-by-key :cnt)} "runs"]
      [:div {:style {:width 100 :cursor :pointer} :on-click #(reset! sort-by-key :ms)} "avg. time"]]
     [components/scroller
      (for [[key {:keys [cnt ms]}] (sort-by #(sort-fn @sort-by-key %) > (get data @current-reg))]
        ^{:key (str "item" key)}
        [:div {:style {:display :flex :flex-direction :row :flex 1 :border-bottom "solid 1px #CCCCCC"}}
         [:div {:style {:width "100%"}} (str key)]
         [:div {:style {:width 100}} (when (pos? cnt) cnt)]
         [:div {:style {:width 100}} (when (and (pos? ms) (pos? cnt)) (utils/str-ms (/ ms cnt)))]])]]))

