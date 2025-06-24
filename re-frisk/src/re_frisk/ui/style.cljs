(ns re-frisk.ui.style
  (:require [re-frisk.utils :as utils]))

(defn inner-view-container [width dragging?]
  (merge {:position       :fixed
          :top            0
          :bottom         0
          :right          0
          :display        :flex
          :pointer-events :all
          :flex-direction :row
          :flex           1
          :width          (str width "px")}
         (when (and (not dragging?) (not (utils/closed? width)))
           {:transition "width 0.5s"})))

(def external-button
  {:margin-left -30
   :width                     30
   :height                    30
   :background-color          "#df691a"
   :color                     :white
   :border-bottom-left-radius 8
   :border-top-left-radius    8
   :cursor                    :pointer
   :display                   :flex
   :align-items               :center
   :justify-content           :center})

(def dragg-button
  {:margin-left -30
   :width                     30
   :height                    60
   :background-color          "#df691a"
   :display                   :flex
   :border-bottom-left-radius 8
   :border-top-left-radius    8
   :align-items               :center
   :justify-content           :center
   :cursor                    :grabbing
   :margin-top                10})
