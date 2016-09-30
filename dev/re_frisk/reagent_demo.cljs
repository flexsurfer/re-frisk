(ns re-frisk.reagent-demo
  (:require [reagent.core :as r]
            [re-frisk.core :refer [enable-frisk! add-data]]))

(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn shared-state []
  (let [val (r/atom "foo")
        _ (add-data :input-val val)]
    (fn []
      [:div
       [:p "The value is now: " @val]
       [:p "Change it here: " [atom-input val]]])))

(defn ^:export run
  []
  (enable-frisk!)
  (r/render [shared-state]
            (js/document.getElementById "app")))