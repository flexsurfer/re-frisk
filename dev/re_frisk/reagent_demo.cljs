(ns re-frisk.reagent-demo
  (:require [reagent.core :as r]
            [re-frisk.core :refer [add-data add-in-data]]))

(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(let [v (-> % .-target .-value)]
                        (add-in-data [:my-log :on-change] v)
                        (reset! value v))}])

(defn shared-state []
  (let [val (r/atom "foo")
        _ (add-in-data [:my-log :input-val] val)
        _ (add-data :test "test")]
    (fn []
      [:div
       [:p "The value is now: " @val]
       [:p "Change it here: " [atom-input val]]])))

(defn ^:export run
  []
  (r/render [shared-state]
            (js/document.getElementById "app")))
