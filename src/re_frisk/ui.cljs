(ns re-frisk.ui
  (:require [reagent.core :as r]
            [re-frisk.devtool :as d]
            [goog.events :as e])
  (:import [goog.events EventType]))

;reagent d'n'd - https://github.com/borkdude/draggable-button-in-reagent

(defonce draggable (r/atom {}))

(defonce ua js/window.navigator.userAgent)
(defonce ie? (or (re-find #"MSIE " ua) (re-find #"Trident/" ua) (re-find #"Edge/" ua)))

(defn get-client-rect [evt]
  (let [r (.getBoundingClientRect (.-target evt))]
    {:left (.-left r), :top (.-top r)}))

(defn mouse-move-handler [offset]
  (fn [evt]
    (let [x (- (.-clientX evt) (:x offset))
          y (- (.-clientY evt) (:y offset))]
      (reset! draggable {:x x :y y}))))

(defn mouse-up-handler [on-move]
  (fn me [evt]
    (e/unlisten js/window EventType.MOUSEMOVE on-move)))

(defn mouse-down-handler [e]
  (let [{:keys [left top]} (get-client-rect e)
        offset             {:x (- (.-clientX e) left)
                            :y (- (.-clientY e) top)}
        on-move            (mouse-move-handler offset)]
    (e/listen js/window EventType.MOUSEMOVE on-move)
    (e/listen js/window EventType.MOUSEUP (mouse-up-handler on-move))))

(defn re-frisk-shell [frisk {:keys [on-click x y w h]}]
  (let [style {}
        h (when (and ie? (not h)) 200)
        style (merge style (when h {:height h :max-height h :overflow "auto"}))
        style (merge style (when w {:width w :max-width w :overflow "auto"}))]
    (when x (swap! draggable assoc :x x))
    (when y (swap! draggable assoc :y y))
    (fn []
      (when (:w-c @d/deb-data)
        [:div {:style (merge {:position "fixed"
                              :left (str (:x @draggable) "px")
                              :top (str (:y @draggable) "px")
                              :z-index 999}
                             (when (or ie? (not (:x @draggable)))
                               {:bottom  (str (if ie? "-200" "-20") "px")
                                :right "20px"}))}
         [:div {:style {:fontFamily "Consolas,Monaco,Courier New,monospace"
                        :fontSize "12px"
                        :display "inline-block"
                        :background-color "#CCCCCC"
                        :cursor "move"
                        :padding "6px"
                        :text-align "left"
                        :border-radius "2px"
                        :border-bottom-left-radius "0"
                        :border-bottom-right-radius "0"
                        :padding-left "2rem"}
                :on-mouse-down mouse-down-handler}
          "re-frisk"]
         (when (:p @d/deb-data)
           [:div {:style{:margin-left "5px"
                         :display "inline-block"
                         :padding "3px"
                         :width "15px"
                         :text-align "center"
                         :background-color "#CCCCCC"
                         :cursor "pointer"
                         :border-radius "2px"
                         :border-bottom-left-radius "0"
                         :border-bottom-right-radius "0"
                         :padding-left "2rem"}
                  :on-click on-click}
            "\u2197"])
         [:div {:style style}
          frisk]]))))
