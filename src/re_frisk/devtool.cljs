(ns re-frisk.devtool
  (:require [re-frisk.data :refer [re-frame-events re-frame-data deb-data]]
            [reagent.core :as r]
            [reagent.dom :as rd]
            [datafrisk.core :as f]
            [re-frame.core :refer [dispatch]]
            [cognitect.transit :as t]
            [re-frisk.ui :as ui])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn export-json []
  (if-let [str (try (t/write (t/writer :json) @(:app-db @re-frame-data))
                 (catch js/Object e (.alert (:win @deb-data) e)))]
      (.saveAs (:win @deb-data)
        (js/Blob. (js/Array. str) {:type "application/json"})
        "app-db.json")))

(defn json-on-change [event]
  (let [rdr (js/FileReader.)]
    (set! (.-onload rdr) #(do
                           (reset! re-frame-events [])
                           (dispatch [:re-frisk/update-db (t/read (t/reader :json) (.-result (.-target %)))])))
    (.readAsText rdr (aget (.-files (.-target event)) 0))))

(defn debugger-messages []
  (r/create-class
    {:display-name "debugger-messages"
     :component-did-update
     (fn [this]
       (let [n (r/dom-node this)]
         (when (:scroll-bottom? @deb-data)
           (set! (.-scrollTop n) (.-scrollHeight n)))))
     :reagent-render
     (fn []
       (let [clrs (:evnt-colors @deb-data)]
         [:div.debugger-sidebar-messages {:on-scroll #(let [t (.-target %)]
                                                        (swap! deb-data assoc :scroll-bottom? (= (.-scrollTop t) (- (.-scrollHeight t) (.-offsetHeight t)))))}
          (map-indexed (fn [id item]
                         (let [event (first (if (:event item) (:event item) item))
                               fx? (boolean (re-find #"-fx" (str event)))
                               db? (boolean (re-find #"-db" (str event)))
                               clr (event clrs)]
                           ^{:key id} [:div.messages-entry {:on-click #(swap! deb-data assoc :event-data item)}
                                       [:span {:style {:display "inline-block"
                                                       :background-color (cond clr clr fx? "#FF0000" db? "#00FF00" :else "#3d3d3d")
                                                       :opacity 0.5
                                                       :width "15px"
                                                       :height "15px"
                                                       :overflow "hidden"
                                                       :padding-bottom "4px"}}
                                        (cond fx? "fx" db? "db" :else "  ")]
                                       [:span.messages-entry-content (str event)]])) @re-frame-events)]))}))

(defn event-bar []
  (let [evnt-key (reaction (first (or (:event (:event-data @deb-data)) (:event-data @deb-data))))
        clr (reaction (if @evnt-key (@evnt-key (:evnt-colors @deb-data)) ""))]
    (fn []
      [:div {:style {:width "100%" :height "20px" :background-color "#3d3d3d" :color "#ffffff" :position "relative"}}
       [:div "Event"]
       [:input {:style {:position "absolute" :left "50px" :top "0px" :width "60px"}
                :placeholder "#000000" :type "text" :value @clr :max-length "7"
                :on-change #(swap! deb-data assoc-in [:evnt-colors @evnt-key] (-> % .-target .-value))}]
       [:div {:style {:position "absolute" :right "0px" :top "0px" :width "20px" :cursor "pointer"}
              :on-click #(swap! deb-data assoc :event-data nil)} "X"]])))

(defn debugger-shell []
  (let [expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        expand-by-default2 (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        state-atom (r/atom expand-by-default)
        state-atom2 (r/atom expand-by-default2)]
    (fn []
      [:div#debugger
       [:div.debugger-sidebar
        [debugger-messages]
        [:div.debugger-sidebar-controls
         [:div.debugger-sidebar-controls-import-export
          [:span {:style {:cursor "pointer"}
                  :on-click #(.click (.getElementById (:doc @deb-data) "json-file-field"))}
           "Import"]
          " / "
          [:span {:style {:cursor "pointer"}
                  :on-click export-json} "Export"]]]]
       [:div#values
        [:div {:style (merge ui/frisk-style {:height (if (:event-data @deb-data) "60%" "100%")})}
         [:div
          (map-indexed (fn [id x]
                         ^{:key id} [f/Root x id state-atom]) [@re-frame-data])]]
        [:div {:style (merge ui/frisk-style {:height "40%" :overflow "hidden" :display (if (:event-data @deb-data) "block" "none")})}
         [event-bar]
         [:div {:style {:overflow "auto" :height "100%"}}
          (map-indexed (fn [id x]
                         ^{:key id} [f/Root x id state-atom2]) [(:event-data @deb-data)])
          [:div {:style {:height "20px"}}]]]]])))

(defn reagent-debugger-shell []
  (let [expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        state-atom (r/atom expand-by-default)]
    (fn []
     [:div {:style ui/frisk-style}
      [:div
       (map-indexed (fn [id x]
                      ^{:key id} [f/Root x id state-atom]) [@re-frame-data])]])))

(defn on-window-unload []
  (rd/unmount-component-at-node (:app @deb-data))
  (swap! deb-data assoc :deb-win-closed? true))

(defn mount [w d]
  (let [app (.getElementById d "app")
        re-frame? (:app-db @re-frame-data)]
    (aset w "onunload" on-window-unload)
    (swap! deb-data assoc :deb-win-closed? false :doc d :win w :app app)
    (r/render [:div  {:style {:height "100%"}}
               [:input {:type "file" :id "json-file-field" :on-change json-on-change :style {:display "none"}}]
               (if (and re-frame? (not= (:events? (:prefs @deb-data)) false))
                 [debugger-shell]
                 [reagent-debugger-shell])]
              app)))

(defn open-debugger-window []
  (let [w (js/window.open "" "Debugger" "width=800,height=400,resizable=yes,scrollbars=yes,status=no,directories=no,toolbar=no,menubar=no")
        d (.-document w)]
    (.open d)
    (.write d ui/debugger-page)
    (aset w "onload" #(mount w d))
    (.close d)))





