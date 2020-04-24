(ns re-frisk.ui.views
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require ;[reagent.dom :as rdom]
            [reagent.core :as reagent]
            [re-com.core :as re-com]
            [re-frisk.db :as db]
            [re-frisk.ui.events :as events]
            [re-frisk.ui.components.splits :as splits]
            [re-frisk.ui.components.frisk :as frisk]
            [re-frisk.ui.style :as style]
            [re-frisk.ui.components.drag :as drag]
            [re-frisk.ui.external-hml :as external-hml]
            [re-frisk.utils :as utils]))

(defn subs-view [subs checkbox-sorted-val]
  (let [state-atom (reagent/atom frisk/expand-by-default)]
    (fn [_]
      [frisk/Root (utils/sort-map @subs @checkbox-sorted-val checkbox-sorted-val) 0 state-atom])))

(defn app-db-view [app-db tool-state]
  (let [state-atom          (reagent/atom frisk/expand-by-default)
        checkbox-sorted-val (reagent/atom true)]
    (fn [_]
      [re-com/v-box :size "1"
       :children
       [[re-com/h-box
         :children
         [[re-com/label :label "app-db"]
          [re-com/gap :size "20px"]
          [re-com/checkbox
           :model checkbox-sorted-val
           :on-change (utils/on-change-sort app-db checkbox-sorted-val :re-frisk-sorted)
           :label "sort"]
          (when (:app-db-delta-error @tool-state)
            [re-com/label :label "update error" :style {:margin-left "4px" :color "#df691a"}])]]
        [frisk/Root (utils/sort-map @app-db @checkbox-sorted-val checkbox-sorted-val) 0 state-atom]]])))

(defn main-view [re-frame-data tool-state doc]
  (let [subs-checkbox-sorted-val (reagent/atom true)
        open-event-split?        (reaction (boolean (get @tool-state :selected-event)))]
    (fn [_ _ _]
      [re-com/v-box :size "1" :style {:padding "0"}
       :children [[re-com/h-box :style {:background-color "#4e5d6c"}
                   :children
                   [[re-com/label :label "Active subscriptions"]
                    [re-com/gap :size "20px"]
                    [re-com/checkbox
                     :model subs-checkbox-sorted-val
                     :on-change (utils/on-change-sort (:subs re-frame-data) subs-checkbox-sorted-val [[:re-frisk-sorted] []])
                     :label "sort"]
                    (when (:subs-delta-error @tool-state)
                      [re-com/label :label "update error" :style {:margin-left "4px" :color "#df691a"}])]]
                  [splits/v-split :size "1" :initial-split "0" :style {:padding "0" :margin "0"} :document doc
                   ;; SUBS
                   :panel-1 [subs-view (:subs re-frame-data) subs-checkbox-sorted-val]
                   :panel-2 [splits/v-split :size "1" :initial-split "100" :style {:padding "0" :margin "0"}
                             :document doc :open-bottom-split? open-event-split?
                             :panel-1 [re-com/v-box :size "1" :style {:background-color "#4e5d6c"}
                                       :children
                                       ;; APP-DB
                                       [[app-db-view (:app-db re-frame-data) tool-state]
                                        [events/event-bar tool-state]]]
                             ;; EVENT
                             :panel-2 [events/event-view tool-state]]]]])))

(defn external-main-view [re-frame-data tool-state & [doc]]
  [re-com/v-box :height "100%"
   :children
   [[splits/h-split :size "1" :initial-split "25" :document doc
     ;;EVENTS
     :panel-1 [events/events-view re-frame-data tool-state]
     ;;MAIN (subs, app-db, event)
     :panel-2 [main-view re-frame-data tool-state doc]]]])

(defn on-external-window-unload [app]
  (fn []
    (reagent/unmount-component-at-node app)
    (swap! db/tool-state assoc :ext-win-opened? false)))

(defn mount-external [window doc re-frame-data]
  (let [app (.getElementById doc "re-frisk-debugger-div")]
    (goog.object/set window "onunload" (on-external-window-unload app))
    (swap! db/tool-state assoc :ext-win-opened? true)
    (reagent/render
     [:div {:style {:height "100%"}}
      [external-main-view re-frame-data db/tool-state doc]]
     app)))

(defn open-debugger-window [re-frame-data]
  (fn []
    (let [{:keys [ext_height ext_width]} (:opts @db/tool-state)
          win (js/window.open "" "Debugger" (str "width=" (or ext_width 800)
                                                 ",height=" (or ext_height 800)
                                                 ",resizable=yes,scrollbars=yes,status=no"
                                                 ",directories=no,toolbar=no,menubar=no"))
          doc (.-document win)]
      (.open doc)
      (.write doc external-hml/html-doc)
      (goog.object/set win "onload" #(mount-external win doc re-frame-data))
      (.close doc))))

(defn on-iframe-load [re-frame-data]
  (fn []
    (let [doc (.-contentDocument (.getElementById js/document "re-frisk-iframe"))]
      (reagent/render
       [:div {:style {:height "100%"}}
        [external-main-view re-frame-data db/tool-state doc]]
       (.getElementById doc "re-frisk-debugger-div")))))

(defn inner-view [re-frame-data]
  (let [ext-opened? (reaction (:ext-win-opened? @db/tool-state))
        latest-left (reaction (:latest-left @db/tool-state))]
    (fn []
      (when-not @ext-opened?
        (let [left (or (utils/normalize-draggable (:x @drag/draggable))
                       (- js/window.innerWidth 30))]
          [:div {:style (style/inner-view-container left (:offset @drag/draggable))}
           [:div {:style {:display :flex :flex-direction :column :opacity 0.3}}
            [:div {:style    style/external-button
                   :on-click (open-debugger-window re-frame-data)}
             "\u2197"]
            [:div {:style {:display :flex :flex 1 :justify-content :center :flex-direction :column}}
             [:div {:style style/external-button
                    :on-click #(let []
                                 (when-not (utils/closed? left)
                                   (swap! db/tool-state assoc :latest-left (- js/window.innerWidth left)))
                                 (swap! drag/draggable assoc :x (- js/window.innerWidth
                                                                   (if (utils/closed? left) @latest-left 30))))}
              (if (utils/closed? left) "\u2b60" "\u2b62")]
             [:div {:style style/dragg-button
                    :on-mouse-down drag/mouse-down-handler}]]]
           (when-not (utils/closed? left)
             [:div {:style {:display :flex :flex 1 :width "100%" :height "100%"}}
              [:iframe {:id "re-frisk-iframe" :src-doc external-hml/html-doc :width "100%" :height "100%"
                        :style (if (:offset @drag/draggable) {:pointer-events :none} {:pointer-events :all})
                        :on-load (on-iframe-load re-frame-data)}]])])))))

(defn mount-internal [re-frame-data]
  (let [div (.createElement js/document "div")]
    (goog.object/set div "style"
          (str "position:fixed; top:0; left:0; bottom:0; right:0; width:100%; height:100%; border:none;"
               "margin:0; padding:0; z-index:999999999;pointer-events: none;"))
    (.appendChild (.-body js/document) div)
    (reagent/render [inner-view re-frame-data] div)))