(ns re-frisk.ui.reagent-views
  (:require [re-frisk.ui.components.components :as components]
            [re-frisk.subs-graph :as subs-graph]
            [re-frisk.ui.components.frisk :as frisk]
            [re-frisk.ui.components.colors :as colors]
            [re-com.core :as re-com]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]))

(defn input-signals [name subs]
  (when-let [signals (get @subs-graph/view->reactions name)]
    [frisk/Root-Simple (into {}
                             (map (fn [item]
                                    (let [op (get @subs-graph/reaction->operation item)]
                                      (if op
                                        {op (get subs [[op] []])}
                                        {:reagent :atom})))
                                  signals))]))

(defn views [_]
  (let [checkbox-sorted-val (reagent/atom false)]
    (fn [re-frame-data]
      (let [data @(:views re-frame-data)
            subs @(:subs re-frame-data)
            chb-val @checkbox-sorted-val
            view->reactions @subs-graph/view->reactions]
        [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                       :padding 8 :flex-direction :column}}
         [:div {:style {:margin-bottom 10 :border-bottom "solid 1px #000000" :display :flex
                        :flex-direction :row}}
          "Mounted views (" (count data) ") in mount order"
          [:div {:style {:width 10 :margin-left 10}} " | "]
          [re-com/checkbox
           :model checkbox-sorted-val
           :on-change #(do (swap! checkbox-sorted-val not) nil)
           :label "subs"]]
         [components/scroller
          (for [{:keys [name]} (sort-by :order (vals data))]
            (when (or (not chb-val) (get view->reactions name))
              ^{:key (str "item" name)}
              [:div {:style {:display :flex :flex-direction :column :border-bottom "solid 1px #CCCCCC"}}
               [:div {:style {:min-width "200" :color colors/render}} name]
               [input-signals name subs]]))]]))))
