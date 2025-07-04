(ns re-frisk.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frisk.utils :as utils]
            [re-frisk.ui.style :as style]
            [reagent.dom.client :as rdom]
            [re-frisk.ui.components.drag :as drag]
            [re-frisk.ui.external-hml :as external-hml]
            [re-frisk.db :as db]
            [re-frisk.subs-graph :as subs-graph]
            [re-frisk.ui.views :as ui.views]
            [goog.object :as gobj]))

(defn on-external-window-unload [root]
  (fn []
    (rdom/unmount root)
    (swap! db/tool-state assoc :ext-win-opened? false)))

(defn mount-external [window doc re-frame-data]
  (let [root (rdom/create-root (.getElementById doc "re-frisk-debugger-div"))]
    (gobj/set window "onunload" (on-external-window-unload root))
    (swap! db/tool-state assoc :ext-win-opened? true :doc doc)
    (subs-graph/init window doc)
    (rdom/render root [:div {:style {:height "100%"}}
                       [ui.views/main-view re-frame-data db/tool-state doc]])))

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
      (gobj/set win "onload" #(mount-external win doc re-frame-data))
      (.close doc))))

(defn on-iframe-load [re-frame-data]
  (fn []
    (let [iframe (.getElementById js/document "re-frisk-iframe")
          doc (.-contentDocument iframe)
          win (.-contentWindow iframe)]
      (swap! db/tool-state assoc :doc doc)
      (subs-graph/init win doc)
      (rdom/render
       (rdom/create-root (.getElementById doc "re-frisk-debugger-div"))
       [:div {:style {:height "100%"}}
        [ui.views/main-view re-frame-data db/tool-state doc]]))))

(defn handle-toggle []
  (let [width (or (utils/normalize-draggable (:width @drag/draggable))
                  0)]
    (when-not (utils/closed? width)
      (swap! db/tool-state assoc :latest-width width))
    (swap! drag/draggable assoc :width (if (utils/closed? width)
                                         (:latest-width @db/tool-state)
                                         0))))

(defn handle-keydown [e]
  (let [input-elements #{"INPUT" "SELECT" "TEXTAREA"}
        input-focused? (contains? input-elements  (.-tagName (.-target e)))]
    (when (and (not input-focused?)
               (= (.-key e) "h")
               (.-ctrlKey e))
      (.preventDefault e)
      (handle-toggle))))

(defonce listener (js/window.addEventListener "keydown" handle-keydown))

(defn inner-view [re-frame-data]
  (let [ext-opened? (reaction (:ext-win-opened? @db/tool-state))
        hidden (get-in  @db/tool-state [:opts :hidden])]
    (fn []
      (when-not @ext-opened?
        (let [width (or (utils/normalize-draggable (:width @drag/draggable))
                        0)]
          [:div {:style (style/inner-view-container width (:offset @drag/draggable))}
           (when-not (and hidden (utils/closed? width))
             [:div {:style {:display :flex :flex-direction :column :opacity 0.3}}
              [:div {:style    style/external-button
                     :on-click (open-debugger-window re-frame-data)}
               "\u2197"]
              [:div {:style {:display :flex :flex 1 :justify-content :center :flex-direction :column}}
               [:div {:style    style/external-button
                      :on-click handle-toggle}
                (if (utils/closed? width) "\u2b60" "\u2b62")]
               [:div {:style         style/dragg-button
                      :on-mouse-down drag/mouse-down-handler}]]])
           (when-not (utils/closed? width)
             [:div {:style {:display :flex :flex 1 :width "100%" :height "100%"}}
              [:iframe {:id      "re-frisk-iframe" :src-doc external-hml/html-doc :width "100%" :height "100%"
                        :style   (if (:offset @drag/draggable) {:pointer-events :none} {:pointer-events :all})
                        :on-load (on-iframe-load re-frame-data)}]])])))))

(defn mount-internal [re-frame-data]
  (let [div (.createElement js/document "div")]
    (gobj/set div "style"
              (str "position:fixed; top:0; left:0; bottom:0; right:0; width:100%; height:100%; border:none;"
                   "margin:0; padding:0; z-index:999999999;pointer-events: none;"))
    (.appendChild ^js (.-body js/document) div)
    (rdom/render (rdom/create-root div) [inner-view re-frame-data])))
