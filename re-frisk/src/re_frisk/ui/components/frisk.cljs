(ns re-frisk.ui.components.frisk
  (:require [clojure.set :as set]
            [re-frisk.filter.filter-parser :as filter-parser]
            [re-frisk.filter.filter-matcher :as filter-matcher]
            [re-frisk.ui.components.components :as components]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]
            [re-frisk.clipboard :as clipboard]
            cljs.pprint))

;;original idea Odin Hole Standal https://github.com/Odinodin/data-frisk-reagent
(declare DataFrisk)

(def debounce-pending (atom {}))
(defn debounce [key delay f]
  (let [old-timeout (get @debounce-pending key)
        new-timeout (js/setTimeout f delay)]
    (swap! debounce-pending assoc key new-timeout)
    (js/clearTimeout old-timeout)))

(defn ExpandButton [{:keys [expanded? path emit-fn]}]
  [:button {:style    {:border          0
                       :backgroundColor "transparent" :width "20px" :height "20px"}
            :on-click #(emit-fn (if expanded? :contract :expand) path)}
   [:svg {:viewBox "0 0 100 100"
          :width   "100%" :height "100%"
          :style   {:transition "all 0.2s ease"
                    :transform  (when expanded? "rotate(90deg)")}}
    [:polygon {:points "0,0 0,100 100,50" :stroke "gray" :color "gray"}]]])

(def styles
  {:shell                {:backgroundColor "#FAFAFA"
                          :fontFamily      "Consolas,Monaco,Courier New,monospace"
                          :fontSize        "12px"
                          :z-index         9999}
   :strings              {:color "#4Ebb4E"}
   :keywords             {:color "purple"}
   :numbers              {:color "blue"}
   :nil                  {:color "red"}
   :shell-visible-button {:backgroundColor "#4EE24E"}})

(defn ExpandAllButton [emit-fn data]
  [:button {:on-click #(emit-fn :expand-all data)}
   "exp"])

(defn CollapseAllButton [emit-fn data]
  [:button {:on-click #(emit-fn :collapse-all)}
   "coll"])

(defn button [label emit-fn]
  [:button {:on-click emit-fn
            :style    {:paddingLeft             "5px"
                       :paddingRight            "5px"
                       :marginLeft              "5px"}}
   label])

(defn FilterEditBox [emit-fn inp-val]
  [:input {:type        "text"
           :value       @inp-val
           :style       {:flex 1 :margin-left 5}
           :placeholder "Type here to find keys..."
           :on-change   #(let [val (.. % -target -value)]
                           (reset! inp-val val)
                           (emit-fn :filter-change val))}])

(defn FilterReset [emit-fn inp-val]
  [:button {:style    {:margin-right 5 :width 25}
            :on-click #(do
                         (reset! inp-val "")
                         (emit-fn :filter-change "" 0))} "X"])

(defn node-clicked [{:keys [event emit-fn path]}]
  (.stopPropagation event)
  (emit-fn :filter-change-exp (str path) 0))

(defn NilText []
  [:span {:style (:nil styles)} (pr-str nil)])

(defn StringText [data]
  [:span {:style (:strings styles)} (pr-str data)])

(defn KeywordText [data]
  [:span {:style (:keywords styles)} (str data)])

(defn NumberText [data]
  [:span {:style (:numbers styles)} data])

(defn is-prefix [needle haystack]
  (and (< (count needle) (count haystack))
       (= needle (subvec haystack 0 (count needle)))))

(defn Node [{:keys [data path emit-fn swappable node matching-paths]}]
  [:span {:style {:padding-top "5px"}}
   (when node
     [:span {:style {:padding-left "20px"}}
      [Node node]])
   [:span
    (merge
     {:on-click #(node-clicked {:event % :emit-fn emit-fn :path path})
      :style    (merge (when node {:padding-left "10px"})
                       (when (get matching-paths path)
                         {:background-color "#fff9db"}))}
     (when (get matching-paths path)
       {:id  (str path)
        :ref #(emit-fn :filter-ref path %)}))
    (cond
      (nil? data)
      [NilText]

      (string? data)
      (if swappable
        [:input {:type          "text"
                 :default-value (str data)
                 :on-change     (fn string-changed [e] (emit-fn :changed path (.. e -target -value)))}]
        [StringText data])

      (keyword? data)
      (if swappable
        [:input {:type          "text"
                 :default-value (name data)
                 :on-change     (fn keyword-changed [e] (emit-fn :changed path (keyword (.. e -target -value))))}]
        [KeywordText data])

      (object? data)
      "Object"

      (number? data)
      (if swappable
        [:input {:type          "number"
                 :default-value data
                 :on-change     (fn number-changed [e] (emit-fn :changed path (js/Number (.. e -target -value))))}]
        [NumberText data])
      :else
      (str data))]])

;; A path is expanded if it is explicitly expanded or if it is a part of
;; current selection
(defn is-expanded [expanded-paths expanded-matching-paths path]
  (or (get expanded-paths path)
      (get expanded-matching-paths path)))

(defn KeyValNode [{[k v] :data :keys [path expanded-paths matching-paths expanded-matching-paths emit-fn swappable]}]
  [:div {:style {:display "flex"}}
   [DataFrisk {:node                    {:data           k
                                         :emit-fn        emit-fn
                                         :path           (conj path k)
                                         :matching-paths matching-paths}
               :data                    v
               :swappable               swappable
               :path                    (conj path k)
               :expanded-paths          expanded-paths
               :matching-paths          matching-paths
               :expanded-matching-paths expanded-matching-paths
               :emit-fn                 emit-fn}]])

(defn copy [_]
  (let [ show-copied (reagent/atom nil)]
    (fn [data]
      [:span {:on-click (fn []
                          (reset! show-copied true)
                          (js/setTimeout #(reset! show-copied false) 2000)
                          (clipboard/copy-to-clip (with-out-str (cljs.pprint/pprint data))))
              :style {:cursor :pointer}}
       " ⎘"
       (when @show-copied
         [:span {:style {:background-color :white :border-radius 4 :margin-left 5}} "copied"])])))

(defn MapNode [{:keys [data path expanded-paths expanded-matching-paths emit-fn node] :as all}]
  (let [expanded? (is-expanded expanded-paths expanded-matching-paths path)]
    [:div {:style {:display "flex" :padding-top "3px"}}
     [:div {:style {:flex "0 1 auto"}}
      (if (empty? data)
        [:div {:style {:width "20px"}}]
        [ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn}])]
     [:div {:style {:flex 1}}
      (when node
        [Node node])
      [:span " {"]
      [:span (str (count (keys data)) " keys")]
      [:span "}"]
      [copy data]
      (when expanded?
        (map-indexed (fn [i x] ^{:key i}
                       [:div {:style {:flex 1}}
                        [KeyValNode (assoc all :data x)]])
                     data))]]))

(defn ListVecNode [{:keys [data path expanded-paths matching-paths expanded-matching-paths emit-fn swappable node]}]
  (let [expanded? (is-expanded expanded-paths expanded-matching-paths path)]
    [:div {:style {:display "flex" :padding-top "3px"}}
     [:div {:style {:flex "0 1 auto"}}
      (if (empty? data)
        [:div {:style {:width "20px"}}]
        [ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn}])]
     [:div {:style {:flex 1}}
      (when node
        [Node node])
      [:span (if (vector? data) " [" " (")
       (str (count data) " items")]
      [:span (if (vector? data) "]" ")")]
      [copy data]
      (when expanded?
        (map-indexed (fn [i x] ^{:key i} [:div {:style {:flex 1}}
                                          [DataFrisk {:data                    x
                                                      :swappable               swappable
                                                      :path                    (conj path i)
                                                      :expanded-paths          expanded-paths
                                                      :matching-paths          matching-paths
                                                      :expanded-matching-paths expanded-matching-paths
                                                      :emit-fn                 emit-fn}]]) data))]]))

(defn SetNode [{:keys [data path expanded-paths matching-paths expanded-matching-paths emit-fn swappable node]}]
  (let [expanded? (is-expanded expanded-paths expanded-matching-paths path)]
    [:div {:style {:display "flex" :padding-top "3px"}}
     [:div {:style {:flex "0 1 auto"}}
      (if (empty? data)
        [:div {:style {:width "20px"}}]
        [ExpandButton {:expanded? expanded?
                       :path      path
                       :emit-fn   emit-fn}])]
     [:div {:style {:flex 1}}
      (when node
        [Node node])
      [:span " #{"
       (str (count data) " items")]
      [:span "}"]
      [copy data]
      (when expanded?
        (map-indexed (fn [i x] ^{:key i} [:div {:style {:flex 1}}
                                          [DataFrisk {:data                    x
                                                      :swappable               swappable
                                                      :path                    (conj path x)
                                                      :expanded-paths          expanded-paths
                                                      :matching-paths          matching-paths
                                                      :expanded-matching-paths expanded-matching-paths
                                                      :emit-fn                 emit-fn}]]) data))]]))

(defn DataFrisk [{:keys [data] :as all}]
  (cond (map? data) [MapNode all]
        (set? data) [SetNode all]
        (or (seq? data) (vector? data)) [ListVecNode all]
        (satisfies? IDeref data) [DataFrisk (assoc all :data @data)]
        :else [Node all]))

(defn conj-to-set [coll x]
  (conj (or coll #{}) x))

(defn expand-all-paths [root-value]
  (loop [remaining [{:path [] :node root-value}]
         expanded-paths #{}]
    (if (seq remaining)
      (let [[current & rest] remaining
            current-node (if (satisfies? IDeref (:node current)) @(:node current) (:node current))]
        (cond (map? current-node)
              (recur
               (concat rest (map (fn [[k v]] {:path (conj (:path current) k)
                                              :node v})
                                 current-node))
               (conj expanded-paths (:path current)))
              (or (seq? current-node) (vector? current-node))
              (recur
               (concat rest (map-indexed (fn [i node] {:path (conj (:path current) i)
                                                       :node node})
                                         current-node))
               (conj expanded-paths (:path current)))
              :else
              (recur
               rest
               (if (coll? current-node)
                 (conj expanded-paths (:path current))
                 expanded-paths))))
      expanded-paths)))

(defn apply-filter [state id value]
  (let [filter (filter-parser/parse value)]
    (assoc-in state [:data-frisk id :filter] filter)))

(defn emit-fn-factory [state-atom id swappable filter-refs inp-val]
  (fn [event & args]
    (case event
      :expand (swap! state-atom update-in [:data-frisk id :expanded-paths] conj-to-set (first args))
      :expand-all (swap! state-atom assoc-in [:data-frisk id :expanded-paths] (expand-all-paths (first args)))
      :contract (swap! state-atom update-in [:data-frisk id :expanded-paths] disj (first args))
      :collapse-all (swap! state-atom assoc-in [:data-frisk id :expanded-paths] #{})
      :filter-ref (swap! filter-refs #(if (second args)
                                        (assoc % (first args) (second args))
                                        (dissoc % (first args))))
      :filter-change-exp
      (do
        (reset! inp-val (first args))
        (swap! state-atom apply-filter id (first args)))
      :filter-change
      (do
        (reset! inp-val (first args))
        (debounce :filter-change 400 #(swap! state-atom apply-filter id (first args))))
      :changed (let [[path value] args]
                 (if (seq path)
                   (swap! swappable assoc-in path value)
                   (reset! swappable value))))))

(defn emit-fn-factory-simple [state-atom id]
  (fn [event & args]
    (case event
      :expand (swap! state-atom update-in [:data-frisk id :expanded-paths] conj-to-set (first args))
      :contract (swap! state-atom update-in [:data-frisk id :expanded-paths] disj (first args))
      nil)))

(defn walk-paths
  ([data]
   (walk-paths [] data))
  ([prefix data]
   (conj
    (cond (map? data)
          (apply set/union
                 (map (fn [[k v]] (walk-paths (conj prefix k) v)) data))
          (set? data)
          (apply set/union
                 (map (fn [v] (walk-paths (conj prefix v) v)) data))
          (or (seq? data) (vector? data))
          (apply set/union
                 (map-indexed
                  (fn [i v] (walk-paths (conj prefix i) v)) data))
          (satisfies? IDeref data) (walk-paths prefix @data)
          :else #{})
    prefix)))

(defn matching-paths [data filter']
  (set (filter #(filter-matcher/match filter' %) (walk-paths data))))

(defn prefixes [path]
  (set (reductions conj [] path)))

;; Any node which is a prefix of a matched path needs to be expnaded
(defn expanded-matching-paths [paths]
  (apply set/union (map prefixes paths)))

(defn scroll-frisk-list-item [filter-ref current-search-index dec?]
  (let [filter-ref @filter-ref
        len (count filter-ref)
        indx @current-search-index]
    (when (> len 0)
      (let [matching (vec (sort-by :y (map #(hash-map
                                             :path (first %)
                                             :y (.-top (.getBoundingClientRect (second %)))
                                             :el (second %))
                                           filter-ref)))]
        (if dec?
          (if (or (zero? indx)
                  (>= (dec indx) len))
            (reset! current-search-index (dec len))
            (swap! current-search-index dec))
          (if (>= (inc indx) len)
            (reset! current-search-index 0)
            (swap! current-search-index inc)))
        (when-let [path (get matching indx)]
          (when-let [elem (:el path)]
            (.scrollIntoView elem #js {:block "center"})))))))

(defn Root [_ _ _]
  (let [filter-refs (atom {})
        current-search-index (atom 0)
        inp-val (reagent/atom "")]
    (fn [data id state-atom]
      (let [data-frisk (:data-frisk @state-atom)
            swappable (when (satisfies? IAtom data)
                        data)
            filter (or (get-in data-frisk [id :filter]) [])
            matching (matching-paths data filter)
            expanded-matching (expanded-matching-paths matching)
            emit-fn (emit-fn-factory state-atom id swappable filter-refs inp-val)]
        [:div {:style {:background-color "#f3f3f3" :color "#444444" :flex 1 :display :flex :flex-direction :column}}
         [:div {:style {:padding "4px 2px" :display :flex}}
          [ExpandAllButton emit-fn data]
          [CollapseAllButton emit-fn]
          [:div {:style {:padding "2px" :margin-left "4px" :background-color "#fff9db"}} (count matching)]
          [button "↑" #(scroll-frisk-list-item filter-refs current-search-index true)]
          [button "↓" #(scroll-frisk-list-item filter-refs current-search-index false)]
          [FilterEditBox emit-fn inp-val]
          [FilterReset emit-fn inp-val]]
         [components/scroller
          [DataFrisk {:data                    data
                      :swappable               swappable
                      :path                    []
                      :expanded-paths          (get-in data-frisk [id :expanded-paths])
                      :matching-paths          matching
                      :expanded-matching-paths expanded-matching
                      :emit-fn                 emit-fn}]]]))))

(def expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1)))

(defn Root-Simple [_ _ _]
  (let [state-atom (reagent/atom expand-by-default)]
    (fn [data]
      [DataFrisk {:data                    data
                  :path                    []
                  :expanded-paths          (get-in @state-atom [:data-frisk 0 :expanded-paths])
                  :emit-fn                 (emit-fn-factory-simple state-atom 0)}])))