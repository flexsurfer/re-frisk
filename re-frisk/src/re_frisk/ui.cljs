(ns re-frisk.ui
  (:require-macros [re-frisk.inlined-deps.reagent.v1v0v0.reagent.ratom :refer [reaction]])
  (:require [re-frisk.utils :as utils]
            [re-frisk.ui.style :as style]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.dom :as rdom]
            [re-frisk.ui.components.drag :as drag]
            [re-frisk.ui.external-hml :as external-hml]
            [re-frisk.db :as db]
            [re-frisk.subs-graph :as subs-graph]
            [re-frisk.ui.views :as ui.views]))

(defn on-external-window-unload [app]
  (fn []
    (rdom/unmount-component-at-node app)
    (swap! db/tool-state assoc :ext-win-opened? false)))

(defn mount-external [window doc re-frame-data]
  (let [app (.getElementById doc "re-frisk-debugger-div")]
    (goog.object/set window "onunload" (on-external-window-unload app))
    (swap! db/tool-state assoc :ext-win-opened? true :doc doc)
    (subs-graph/init window doc)
    (rdom/render
     [:div {:style {:height "100%"}}
      [ui.views/main-view re-frame-data db/tool-state doc]]
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
    (let [iframe (.getElementById js/document "re-frisk-iframe")
          doc (.-contentDocument iframe)
          win (.-contentWindow iframe)]
      (swap! db/tool-state assoc :doc doc)
      (subs-graph/init win doc)
      (rdom/render
       [:div {:style {:height "100%"}}
        [ui.views/main-view re-frame-data db/tool-state doc]]
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
             [:div {:style    style/external-button
                    :on-click #(let []
                                 (when-not (utils/closed? left)
                                   (swap! db/tool-state assoc :latest-left (- js/window.innerWidth left)))
                                 (swap! drag/draggable assoc :x (- js/window.innerWidth
                                                                   (if (utils/closed? left) @latest-left 30))))}
              (if (utils/closed? left) "\u2b60" "\u2b62")]
             [:div {:style         style/dragg-button
                    :on-mouse-down drag/mouse-down-handler}]]]
           (when-not (utils/closed? left)
             [:div {:style {:display :flex :flex 1 :width "100%" :height "100%"}}
              [:iframe {:id      "re-frisk-iframe" :src-doc external-hml/html-doc :width "100%" :height "100%"
                        :style   (if (:offset @drag/draggable) {:pointer-events :none} {:pointer-events :all})
                        :on-load (on-iframe-load re-frame-data)}]])])))))

(defn mount-internal [re-frame-data]
  (let [div (.createElement js/document "div")]
    (goog.object/set div "style"
                     (str "position:fixed; top:0; left:0; bottom:0; right:0; width:100%; height:100%; border:none;"
                          "margin:0; padding:0; z-index:999999999;pointer-events: none;"))
    (.appendChild (.-body js/document) div)
    (rdom/render [inner-view re-frame-data] div)))