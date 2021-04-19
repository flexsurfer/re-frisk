(ns re-frisk.ui.components.splits
  (:require-macros [re-frisk.ui.components.recom :refer [handler-fn]])
  (:require [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [clojure.string :as string]))

;;TODO copy https://github.com/Day8/re-com/blob/master/src/re_com/splits.cljs

;; fixed :on-mouse-leave https://github.com/Day8/re-com/issues/158
;; (.getElementById document container-id), document provided from outside, because re-frisk window is not a root window

(defn sum-scroll-offsets
  "Given a DOM node, I traverse through all ascendant nodes (until I reach body), summing any scrollLeft and scrollTop values
   and return these sums in a map"
  [node]
  (loop [current-node    (.-parentNode node) ;; Begin at parent
         sum-scroll-left 0
         sum-scroll-top  0]
    (if (not= (.-tagName current-node) "BODY")
      (recur (.-parentNode current-node)
             (+ sum-scroll-left (.-scrollLeft current-node))
             (+ sum-scroll-top  (.-scrollTop  current-node)))
      {:left sum-scroll-left
       :top  sum-scroll-top})))

(defn flex-child-style
  [size]
  (let [split-size      (string/split (string/trim size) #"\s+")                  ;; Split into words separated by whitespace
        split-count     (count split-size)
        _               (assert (contains? #{1 3} split-count) "Must pass either 1 or 3 words to flex-child-style")
        size-only       (when (= split-count 1) (first split-size))         ;; Contains value when only one word passed (e.g. auto, 60px)
        split-size-only (when size-only (string/split size-only #"(\d+)(.*)")) ;; Split into number + string
        [_ num units]   (when size-only split-size-only)                    ;; grab number and units
        pass-through?   (nil? num)                                          ;; If we can't split, then we'll pass this straign through
        grow-ratio?     (or (= units "%") (= units "") (nil? units))        ;; Determine case for using grow ratio
        grow            (if grow-ratio? num "0")                            ;; Set grow based on percent or integer, otherwise no grow
        shrink          (if grow-ratio? "1" "0")                            ;; If grow set, then set shrink to even shrinkage as well
        basis           (if grow-ratio? "0px" size)                         ;; If grow set, then even growing, otherwise set basis size to the passed in size (e.g. 100px, 5em)
        flex            (if (and size-only (not pass-through?))
                          (str grow " " shrink " " basis)
                          size)]
    {:-webkit-flex flex
     :flex flex}))

(defn flex-flow-style
  "A cross-browser helper function to output flex-flow with all it's potential browser prefixes"
  [flex-flow]
  {:-webkit-flex-flow flex-flow
   :flex-flow flex-flow})

(defn drag-handle
  "Return a drag handle to go into a vertical or horizontal splitter bar:
    orientation: Can be :horizonal or :vertical
    over?:       When true, the mouse is assumed to be over the splitter so show a bolder color"
  [orientation over?]
  (let [vertical? (= orientation :vertical)
        length    "20px"
        width     "8px"
        pos1      "3px"
        pos2      "3px"
        color     (if over? "#999" "#ccc")
        border    (str "solid 1px " color)
        flex-flow (str (if vertical? "row" "column") " nowrap")]
    [:div {:class "display-flex"
           :style (merge (flex-flow-style flex-flow)
                         {:width  (if vertical? width length)
                          :height (if vertical? length width)
                          :margin "auto"})}
     [:div {:style (if vertical?
                     {:width pos1 :height length :border-right border}
                     {:width length :height pos1 :border-bottom border})}]
     [:div {:style (if vertical?
                     {:width pos2 :height length :border-right border}
                     {:width length :height pos2 :border-bottom border})}]]))

(defn h-split
  "Returns markup for a horizontal layout component"
  [& {:keys [size width height on-split-change initial-split splitter-size margin document]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px" document js/document}
      :as   args}]
  (let [container-id         (gensym "h-split-")
        split-perc           (reagent/atom (js/parseInt initial-split)) ;; splitter position as a percentage of width
        dragging?            (reagent/atom false)           ;; is the user dragging the splitter (mouse is down)?
        over?                (reagent/atom false)           ;; is the mouse over the splitter, if so, highlight it

        stop-drag            (fn []
                               (when on-split-change (on-split-change @split-perc))
                               (reset! dragging? false))

        calc-perc            (fn [mouse-x]                  ;; turn a mouse y coordinate into a percentage position
                               (let [container  (.getElementById document container-id) ;; the outside container
                                     offsets    (sum-scroll-offsets container) ;; take any scrolling into account
                                     c-width    (.-clientWidth container) ;; the container's width
                                     c-left-x   (.-offsetLeft container) ;; the container's left X
                                     relative-x (+ (- mouse-x c-left-x) (:left offsets))] ;; the X of the mouse, relative to container
                                 (* 100.0 (/ relative-x c-width)))) ;; do the percentage calculation

        mousemove            (fn [event]
                               (reset! split-perc (calc-perc (.-clientX event))))

        mousedown            (fn [event]
                               (.preventDefault event)      ;; stop selection of text during drag
                               (reset! dragging? true))

        mouseover-split      #(reset! over? true)           ;; true CANCELs mouse-over (false cancels all others)
        mouseout-split       #(reset! over? false)

        make-container-attrs (fn [class style attr in-drag?]
                               (merge {:class (str "rc-h-split display-flex " class)
                                       :id    container-id
                                       :style (merge (flex-child-style size)
                                                     (flex-flow-style "row nowrap")
                                                     {:margin margin
                                                      :width  width
                                                      :height height}
                                                     style)}
                                      (when in-drag?        ;; only listen when we are dragging
                                        {:on-mouse-up    (handler-fn (stop-drag))
                                         :on-mouse-leave (handler-fn (stop-drag))
                                         :on-mouse-move  (handler-fn (mousemove event))})
                                      attr))

        make-panel-attrs     (fn [class in-drag? percentage]
                               {:class (str "display-flex " class)
                                :style (merge (flex-child-style (str percentage " 1 0px"))
                                              {:overflow "hidden"} ;; TODO: Shouldn't have this...test removing it
                                              (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs  (fn [class]
                               {:class         (str "display-flex " class)
                                :on-mouse-down (handler-fn (mousedown event))
                                :on-mouse-over (handler-fn (mouseover-split))
                                :on-mouse-out  (handler-fn (mouseout-split))
                                :style         (merge (flex-child-style (str "0 0 " splitter-size))
                                                      {:cursor "col-resize"}
                                                      (when @over? {:background-color "#f8f8f8"}))})]

    (fn
      [& {:keys [panel-1 panel-2 _size _width _height _on-split-change _initial-split _splitter-size _margin class style attr]}]
      [:div (make-container-attrs class style attr @dragging?)
       [:div (make-panel-attrs "rc-h-split-top" @dragging? @split-perc)
        panel-1]
       [:div (make-splitter-attrs "rc-h-split-splitter")
        [drag-handle :vertical @over?]]
       [:div (make-panel-attrs "rc-h-split-bottom" @dragging? (- 100 @split-perc))
        panel-2]])))


;; ------------------------------------------------------------------------------------
;;  Component: v-split
;; ------------------------------------------------------------------------------------

(defn v-split
  "Returns markup for a vertical layout component"
  [& {:keys [size width height on-split-change initial-split splitter-size margin document]
      :or   {size "auto" initial-split 50 splitter-size "8px" margin "8px" document js/document}}]
  (let [container-id         (gensym "v-split-")
        split-perc           (reagent/atom (js/parseInt initial-split)) ;; splitter position as a percentage of height
        dragging?            (reagent/atom false)           ;; is the user dragging the splitter (mouse is down)?
        over?                (reagent/atom false)           ;; is the mouse over the splitter, if so, highlight it

        stop-drag            (fn []
                               (when on-split-change (on-split-change @split-perc))
                               (reset! dragging? false))

        calc-perc            (fn [mouse-y]                  ;; turn a mouse y coordinate into a percentage position
                               (let [container  (.getElementById document container-id) ;; the outside container
                                     offsets    (sum-scroll-offsets container) ;; take any scrolling into account
                                     c-height   (.-clientHeight container) ;; the container's height
                                     c-top-y    (.-offsetTop container) ;; the container's top Y
                                     relative-y (+ (- mouse-y c-top-y) (:top offsets))] ;; the Y of the mouse, relative to container
                                 (* 100.0 (/ relative-y c-height)))) ;; do the percentage calculation

        mousemove            (fn [event]
                               (reset! split-perc (calc-perc (.-clientY event))))

        mousedown            (fn [event]
                               (.preventDefault event)      ;; stop selection of text during drag
                               (reset! dragging? true))

        mouseover-split      #(reset! over? true)
        mouseout-split       #(reset! over? false)

        make-container-attrs (fn [class style attr in-drag?]
                               (merge {:class (str "rc-v-split display-flex " class)
                                       :id    container-id
                                       :style (merge (flex-child-style size)
                                                     (flex-flow-style "column nowrap")
                                                     {:margin margin
                                                      :width  width
                                                      :height height}
                                                     style)}
                                      (when in-drag?        ;; only listen when we are dragging
                                        {:on-mouse-up    (handler-fn (stop-drag))
                                         :on-mouse-move  (handler-fn (mousemove event))
                                         :on-mouse-leave (handler-fn (stop-drag))})
                                      attr))

        make-panel-attrs     (fn [class in-drag? percentage]
                               {:class (str "display-flex " class)
                                :style (merge (flex-child-style (str percentage " 1 0px"))
                                              {:overflow "hidden"} ;; TODO: Shouldn't have this...test removing it
                                              (when in-drag? {:pointer-events "none"}))})

        make-splitter-attrs  (fn [class]
                               {:class         (str "display-flex " class)
                                :on-mouse-down (handler-fn (mousedown event))
                                :on-mouse-over (handler-fn (mouseover-split))
                                :on-mouse-out  (handler-fn (mouseout-split))
                                :style         (merge (flex-child-style (str "0 0 " splitter-size))
                                                      {:cursor "row-resize"}
                                                      (when @over? {:background-color "#f8f8f8"}))})]

    (fn
      [& {:keys [panel-1 panel-2 _size _width _height _on-split-change _initial-split _splitter-size _margin class style attr
                 open-bottom-split? close-bottom-split?]}]
      (let [perc (if close-bottom-split?
                   "0"
                   (if (and (= @split-perc (js/parseInt initial-split)) open-bottom-split?)
                     "70"
                     @split-perc))]
        [:div (make-container-attrs class style attr @dragging?)
         [:div (make-panel-attrs "re-v-split-top" @dragging? perc)
          panel-1]
         (when-not close-bottom-split?
           [:div (make-splitter-attrs "re-v-split-splitter")
            [drag-handle :horizontal @over?]])
         [:div (make-panel-attrs "re-v-split-bottom" @dragging? (- 100 perc))
          panel-2]]))))
