(ns re-frisk.ui.reagent-views
  (:require [re-frisk.ui.components.components :as components]
            [re-frisk.subs-graph :as subs-graph]
            [reagent.core :as reagent]
            [re-frisk.ui.components.frisk :as frisk]
            [re-frisk.ui.components.colors :as colors]))

(defn input-signals [_ _]
  (let [state-atom (reagent/atom frisk/expand-by-default)]
    (fn [name subs]
      (when-let [signals (get @subs-graph/view->reactions name)]
        [frisk/Root-Simple (into {}
                                 (map (fn [item]
                                        (let [op (get @subs-graph/reaction->operation item)]
                                          (if op
                                            {op (get subs [[op] []])}
                                            {:reagent :atom})))
                                      signals))
         0 state-atom]))))

(defn views [re-frame-data]
  (let [data @(:views re-frame-data)
        subs @(:subs re-frame-data)]
    [:div {:style {:display :flex :flex 1 :background-color "#f3f3f3" :color "#444444"
                   :padding 8 :flex-direction :column}}
     [:div {:style {:margin-bottom 10 :border-bottom "solid 1px #000000"}} "Mounted views (" (count data) ") in mount order"]
     [components/scroller
      (for [{:keys [name]} (sort-by :order (vals data))]
        ^{:key (str "item" name)}
        [:div {:style {:display :flex :flex-direction :column :border-bottom "solid 1px #CCCCCC"}}
         [:div {:style {:min-width "200" :color colors/render}} name]
         [input-signals name subs]])]]))
