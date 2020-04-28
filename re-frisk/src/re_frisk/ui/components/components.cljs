(ns re-frisk.ui.components.components)

(defn small-button
  ([label] (small-button {} label))
  ([{:keys [on-click active?] :or {active? true}} label]
   [:a {:class (str "btn btn-xs" (when active? " btn-primary"))
        :on-click on-click} label]))

(defn scroller
  ([div] (scroller {} div))
  ([attr div]
   [:div (merge {:style (merge {:overflow "auto" :height "100%" :flex "1"} (:style attr))}
                (dissoc attr :style))
    div]))