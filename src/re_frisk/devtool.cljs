(ns re-frisk.devtool
  (:require [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
            [re-frame.core :refer [dispatch]]
            [cognitect.transit :as transit]
            [re-frisk.data :refer [re-frame-events re-frame-data deb-data]]
            [re-frisk-shell.core :as ui]))

(defn export-json []
  (if-let [str (transit/write
                 (transit/writer :json
                                 {:handlerForForeign
                                  (fn [_ _] (transit/write-handler (fn [_] "ForeignType")
                                                                   (fn [_] "")))})
                 @(:app-db @re-frame-data))]
      (.saveAs (:win @deb-data)
        (js/Blob. (js/Array. str) {:type "application/json"})
        "app-db.json")))

(defn json-on-change [event]
  (let [rdr (js/FileReader.)]
    (set! (.-onload rdr) #(do
                           (reset! re-frame-events [])
                           (dispatch [:re-frisk/update-db (transit/read
                                                            (transit/reader :json)
                                                            (.-result (.-target %)))])))
    (.readAsText rdr (aget (.-files (.-target event)) 0))))

(defn on-window-unload []
  (reagent-dom/unmount-component-at-node (:app @deb-data))
  (swap! deb-data assoc :deb-win-closed? true))

(defn mount [w d]
  (let [app (.getElementById d "app")
        re-frame? (:app-db @re-frame-data)]
    (aset w "onunload" on-window-unload)
    (swap! deb-data assoc :deb-win-closed? false :doc d :win w :app app)
    (reagent/render [:div  {:style {:height "100%"}}
                     [:input {:type "file" :id "json-file-field" :on-change json-on-change :style {:display "none"}}]
                     (if (and re-frame? (not= (:events? (:prefs @deb-data)) false))
                       [ui/debugger-shell
                        re-frame-data re-frame-events deb-data
                        #(.click (.getElementById (:doc @deb-data) "json-file-field"))
                        export-json]
                       [ui/reagent-debugger-shell re-frame-data])]
                    app)))

(defn open-debugger-window []
  (let [w (js/window.open "" "Debugger" "width=800,height=400,resizable=yes,scrollbars=yes,status=no,directories=no,toolbar=no,menubar=no")
        d (.-document w)]
    (.open d)
    (.write d ui/debugger-page)
    (aset w "onload" #(mount w d))
    (.close d)))





