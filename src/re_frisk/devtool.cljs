(ns re-frisk.devtool
  (:require [hiccups.runtime :as h]
            [re-frisk.data :refer [re-frame-events re-frame-data]]
            [reagent.core :as r]
            [datafrisk.core :as f]
            [re-frame.core :refer [dispatch]]
            [cognitect.transit :as t])
  (:require-macros [hiccups.core :refer [html]]
                   [reagent.ratom :refer [reaction]]))

(defonce deb-data (r/atom {:w-c true
                           :event-data ""}))

(defn export-json [data]
  (js/window.saveAs (js/Blob. (js/Array. data) {:type "application/json"}) "app-db.json"))

(defn debugger-shell [data]
  (let [expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        expand-by-default2 (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range 1))
        get-app-data (get data 2)
        state-atom (r/atom expand-by-default)
        state-atom2 (r/atom expand-by-default2)
        ;; i have two issues with the cljs data structures after passing it between windows
        ;; first problem frisk library doesn't colored keywords right
        ;; second - reagent doesn't see ratom changes, this hack to avoid second issue
        _ (js/setInterval #(swap! state-atom assoc :t (rand)) 100)]
    (fn []
      (let [_ @state-atom] ;; this hack to avoid second issue
        [:div.debugger
         [:div.debugger-sidebar
          [:div.debugger-sidebar-messages
           (for [item @(second data)]
             ^{:key item}
             [:div.messages-entry {:on-click #(swap! deb-data assoc :event-data (second item))}
              [:span.messages-entry-content (str (first item) " " (second item))]])]
          [:div.debugger-sidebar-controls
            [:div.debugger-sidebar-controls-import-export
             [:span {:style {:cursor "pointer"}
                     :on-click #(.click (js/document.getElementById "json-file-field"))}
                    "Import"]
             " / "
             [:span {:style {:cursor "pointer"}
                     :on-click #(export-json (get-app-data))} "Export"]]]]
         [:div.values [:div {:style {:backgroundColor "#FAFAFA"
                                     :fontFamily "Consolas,Monaco,Courier New,monospace"
                                     :fontSize "12px"
                                     :height "50%"
                                     :width "100%"}}
                       [:div
                        (map-indexed (fn [id x]
                                       ^{:key id} [f/Root x id state-atom]) [@(first data)])]]
                      [:div {:style {:backgroundColor "#FAFAFA"
                                     :fontFamily "Consolas,Monaco,Courier New,monospace"
                                     :fontSize "12px"
                                     :height "50%"
                                     :width "100%"}}
                       [:div
                        (map-indexed (fn [id x]
                                       ^{:key id} [f/Root x id state-atom2]) [(:event-data @deb-data)])]]]]))))

(defn json-on-readed [event]
   ((:update-db-handler @deb-data) (.-result (.-target event))))

(defn json-on-change [event]
  (let [rdr (js/FileReader.)]
    (set! (.-onload rdr) json-on-readed)
    (.readAsText rdr (aget (.-files (.-target event)) 0))))

(defn run [data]
  (when-not (:rendered @deb-data)
    (let [div (js/document.getElementById "app")]
      (swap! deb-data assoc :rendered true)
      (println (count data) (get data 3))
      (swap! deb-data assoc :update-db-handler (get data 3))
      (r/render [:div
                 [:input {:type "file" :id "json-file-field" :on-change json-on-change :style {:display "none"}}]
                 [debugger-shell data]]
                div))))
;;^^
;;|| DEBUGGER PAGE
;;======================================================================================================================
;;|| APP PAGE
;;

(defn debugger-page [src]
  [:html
   [:head
    [:title "re-frisk debugger"]
    [:meta {:charset "UTF-8"}]
    [:meta
     {:content "width=device-width, initial-scale=1", :name "viewport"}]]
   [:body {:style "margin:0px;padding:0px"}
    [:script "var saveAs=saveAs||function(e){\"use strict\";if(typeof e===\"undefined\"||typeof navigator!==\"undefined\"&&/MSIE [1-9]\\./.test(navigator.userAgent)){return}var t=e.document,n=function(){return e.URL||e.webkitURL||e},r=t.createElementNS(\"http://www.w3.org/1999/xhtml\",\"a\"),o=\"download\"in r,a=function(e){var t=new MouseEvent(\"click\");e.dispatchEvent(t)},i=/constructor/i.test(e.HTMLElement)||e.safari,f=/CriOS\\/[\\d]+/.test(navigator.userAgent),u=function(t){(e.setImmediate||e.setTimeout)(function(){throw t},0)},s=\"application/octet-stream\",d=1e3*40,c=function(e){var t=function(){if(typeof e===\"string\"){n().revokeObjectURL(e)}else{e.remove()}};setTimeout(t,d)},l=function(e,t,n){t=[].concat(t);var r=t.length;while(r--){var o=e[\"on\"+t[r]];if(typeof o===\"function\"){try{o.call(e,n||e)}catch(a){u(a)}}}},p=function(e){if(/^\\s*(?:text\\/\\S*|application\\/xml|\\S*\\/\\S*\\+xml)\\s*;.*charset\\s*=\\s*utf-8/i.test(e.type)){return new Blob([String.fromCharCode(65279),e],{type:e.type})}return e},v=function(t,u,d){if(!d){t=p(t)}var v=this,w=t.type,m=w===s,y,h=function(){l(v,\"writestart progress write writeend\".split(\" \"))},S=function(){if((f||m&&i)&&e.FileReader){var r=new FileReader;r.onloadend=function(){var t=f?r.result:r.result.replace(/^data:[^;]*;/,\"data:attachment/file;\");var n=e.open(t,\"_blank\");if(!n)e.location.href=t;t=undefined;v.readyState=v.DONE;h()};r.readAsDataURL(t);v.readyState=v.INIT;return}if(!y){y=n().createObjectURL(t)}if(m){e.location.href=y}else{var o=e.open(y,\"_blank\");if(!o){e.location.href=y}}v.readyState=v.DONE;h();c(y)};v.readyState=v.INIT;if(o){y=n().createObjectURL(t);setTimeout(function(){r.href=y;r.download=u;a(r);h();c(y);v.readyState=v.DONE});return}S()},w=v.prototype,m=function(e,t,n){return new v(e,t||e.name||\"download\",n)};if(typeof navigator!==\"undefined\"&&navigator.msSaveOrOpenBlob){return function(e,t,n){t=t||e.name||\"download\";if(!n){e=p(e)}return navigator.msSaveOrOpenBlob(e,t)}}w.abort=function(){};w.readyState=w.INIT=0;w.WRITING=1;w.DONE=2;w.error=w.onwritestart=w.onprogress=w.onwrite=w.onabort=w.onerror=w.onwriteend=null;return m}(typeof self!==\"undefined\"&&self||typeof window!==\"undefined\"&&window||this.content);if(typeof module!==\"undefined\"&&module.exports){module.exports.saveAs=saveAs}else if(typeof define!==\"undefined\"&&define!==null&&define.amd!==null){define(\"FileSaver.js\",function(){return saveAs})}"]
    [:style "\n\nhtml {\n    overflow: hidden;\n    height: 100%;\n}\n\nbody {\n    height: 100%;\n    overflow: auto;\n}\n\n#debugger {\n  width: 100%\n  height: 100%;\n  font-family: monospace;\n}\n\n#values {\n  display: block;\n  float: left;\n  height: 100%;\n  width: calc(100% - 30ch);\n  margin: 0;\n  overflow: auto;\n  cursor: default;\n}\n\n.debugger-sidebar {\n  display: block;\n  float: left;\n  width: 30ch;\n  height: 100%;\n  color: white;\n  background-color: rgb(61, 61, 61);\n}\n\n.debugger-sidebar-controls {\n  width: 100%;\n  text-align: center;\n  background-color: rgb(50, 50, 50);\n}\n\n.debugger-sidebar-controls-import-export {\n  width: 100%;\n  height: 24px;\n  line-height: 24px;\n  font-size: 12px;\n}\n\n.debugger-sidebar-controls-resume {\n  width: 100%;\n  height: 30px;\n  line-height: 30px;\n  cursor: pointer;\n}\n\n.debugger-sidebar-controls-resume:hover {\n  background-color: rgb(41, 41, 41);\n}\n\n.debugger-sidebar-messages {\n  width: 100%;\n  overflow-y: auto;\n  height: calc(100% - 24px);\n}\n\n.debugger-sidebar-messages-paused {\n  width: 100%;\n  overflow-y: auto;\n  height: calc(100% - 54px);\n}\n\n.messages-entry {\n  cursor: pointer;\n  width: 100%;\n}\n\n.messages-entry:hover {\n  background-color: rgb(41, 41, 41);\n}\n\n.messages-entry-selected, .messages-entry-selected:hover {\n  background-color: rgb(10, 10, 10);\n}\n\n.messages-entry-content {\n  width: 23ch;\n  padding-top: 4px;\n  padding-bottom: 4px;\n  padding-left: 1ch;\n  text-overflow: ellipsis;\n  white-space: nowrap;\n  overflow: hidden;\n  display: inline-block;\n}\n\n.messages-entry-index {\n  color: #666;\n  width: 5ch;\n  padding-top: 4px;\n  padding-bottom: 4px;\n  padding-right: 1ch;\n  text-align: right;\n  display: block;\n  float: right;\n}\n\n"]
    [:div#app {:style "height:100%;width:100%"}
     [:h2 "re-frisk debugger"]
     [:p "ENJOY!"]]]
   [:script {:type "text/javascript", :src src}]])

(defn get-app-db []
  (t/write (t/writer :json) @(:app-db @re-frame-data)))

(defn update-db [value]
  (dispatch [:re-frisk/update-db (t/read (t/reader :json) value)]))

(defn open-debugger-window []
  (let [w (js/window.open "" "Debugger" "width=800,height=400,resizable=yes,scrollbars=yes,status=no,directories=no,toolbar=no,menubar=no")
        d (.-document w)]
    (swap! deb-data assoc :w w)
    (.open d)
    (.write d (html (debugger-page (:p @deb-data))))
    (aset w "onload" #(do
                       (swap! deb-data assoc :w-c false)
                       ((:f @deb-data) (:w @deb-data) [re-frame-data re-frame-events get-app-db update-db])))
    (aset w "onunload" #(swap! deb-data assoc :w-c true))
    (.close d)))

(defn register [p f]
  (swap! deb-data assoc :p p)
  (swap! deb-data assoc :f f))

