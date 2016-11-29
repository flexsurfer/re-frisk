(ns re-frisk.ui
  (:require [reagent.core :as r]
            [goog.events :as e]
            [datafrisk.core :as f]
            [re-frisk.data :refer [deb-data]])
  (:import [goog.events EventType]))

(def frisk-style
  {:backgroundColor "#FAFAFA"
   :fontFamily "Consolas,Monaco,Courier New,monospace"
   :fontSize "12px"
   :height "100%"
   :overflow "auto"
   :width "100%"})

(def re-frisk-button-style
  {:fontFamily "Consolas,Monaco,Courier New,monospace"
   :fontSize "12px"
   :display "inline-block"
   :background-color "#CCCCCC"
   :cursor "move"
   :padding "6px"
   :text-align "left"
   :border-radius "2px"
   :border-bottom-left-radius "0px"
   :border-bottom-right-radius "0px"
   :padding-left "2rem"})

(def arrow-style
  {:margin-left "5px"
   :display "inline-block"
   :padding "3px"
   :width "15px"
   :text-align "center"
   :background-color "#CCCCCC"
   :cursor "pointer"
   :border-radius "2px"
   :border-bottom-left-radius "0px"
   :border-bottom-right-radius "0px"
   :padding-left "2rem"})

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

(defn visibility-button
  [visible? update-fn]
  [:button {:style {:border 0
                    :backgroundColor "transparent" :width "20px" :height "10px"}
            :onClick update-fn}
   [:svg {:viewBox "0 0 100 100"
          :width "100%" :height "100%"
          :style {:transition "all 0.2s ease"
                  :transform (when visible? "rotate(90deg)")}}
    [:polygon {:points "0,0 0,100 100,50" :stroke "black"}]]])

(defn re-frisk-panel [& data]
  (let [expand-by-default (reduce #(assoc-in %1 [:data-frisk %2 :expanded-paths] #{[]}) {} (range (count data)))
        state-atom (r/atom expand-by-default)]
    (fn [& data]
       [:div
        (map-indexed (fn [id x]
                       ^{:key id} [f/Root x id state-atom]) [data])])))

(defn re-frisk-shell [data {:keys [on-click x y width height]}]
  (let [style (merge frisk-style {:resize "both" :width "300px" :height "200px"})
        height (if (and ie? (not height)) 200 height)
        style (merge style (when height {:height height :max-height height :overflow "auto"}))
        style (merge style (when width {:width width :max-width width :overflow "auto"}))]
    (when x (swap! draggable assoc :x x))
    (when y (swap! draggable assoc :y y))
    (fn [data]
      (when (:deb-win-closed? @deb-data)
        [:div {:style (merge {:position "fixed"
                              :left (str (:x @draggable) "px")
                              :top (str (:y @draggable) "px")
                              :z-index 999}
                             (when (or ie? (not (:x @draggable)))
                               {:bottom "0px"
                                :right  "20px"}))}
         [:div {:style re-frisk-button-style
                :on-mouse-down mouse-down-handler}
          [visibility-button (:visible? (:data-frisk @deb-data)) (fn [_] (swap! deb-data assoc-in [:data-frisk :visible?] (not (:visible? (:data-frisk @deb-data)))))]
          "re-frisk"]
         [:div {:style arrow-style
                :on-click on-click}
          "\u2197"]
         (when (:visible? (:data-frisk @deb-data))
           [:div {:style style}
            [re-frisk-panel @data]])]))))

(def debugger-page
  "<!DOCTYPE html>
  <html>\n
    <head>\n
      <title>re-frisk debugger</title>
      <meta charset=\"UTF-8\">\n
      <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n
    </head>\n
    <body style=\"margin:0px;padding:0px\">\n
      <script>var saveAs=saveAs||function(e){\"use strict\";if(typeof e===\"undefined\"||typeof navigator!==\"undefined\"&&/MSIE [1-9]\\./.test(navigator.userAgent)){return}var t=e.document,n=function(){return e.URL||e.webkitURL||e},r=t.createElementNS(\"http://www.w3.org/1999/xhtml\",\"a\"),o=\"download\"in r,a=function(e){var t=new MouseEvent(\"click\");e.dispatchEvent(t)},i=/constructor/i.test(e.HTMLElement)||e.safari,f=/CriOS\\/[\\d]+/.test(navigator.userAgent),u=function(t){(e.setImmediate||e.setTimeout)(function(){throw t},0)},s=\"application/octet-stream\",d=1e3*40,c=function(e){var t=function(){if(typeof e===\"string\"){n().revokeObjectURL(e)}else{e.remove()}};setTimeout(t,d)},l=function(e,t,n){t=[].concat(t);var r=t.length;while(r--){var o=e[\"on\"+t[r]];if(typeof o===\"function\"){try{o.call(e,n||e)}catch(a){u(a)}}}},p=function(e){if(/^\\s*(?:text\\/\\S*|application\\/xml|\\S*\\/\\S*\\+xml)\\s*;.*charset\\s*=\\s*utf-8/i.test(e.type)){return new Blob([String.fromCharCode(65279),e],{type:e.type})}return e},v=function(t,u,d){if(!d){t=p(t)}var v=this,w=t.type,m=w===s,y,h=function(){l(v,\"writestart progress write writeend\".split(\" \"))},S=function(){if((f||m&&i)&&e.FileReader){var r=new FileReader;r.onloadend=function(){var t=f?r.result:r.result.replace(/^data:[^;]*;/,\"data:attachment/file;\");var n=e.open(t,\"_blank\");if(!n)e.location.href=t;t=undefined;v.readyState=v.DONE;h()};r.readAsDataURL(t);v.readyState=v.INIT;return}if(!y){y=n().createObjectURL(t)}if(m){e.location.href=y}else{var o=e.open(y,\"_blank\");if(!o){e.location.href=y}}v.readyState=v.DONE;h();c(y)};v.readyState=v.INIT;if(o){y=n().createObjectURL(t);setTimeout(function(){r.href=y;r.download=u;a(r);h();c(y);v.readyState=v.DONE});return}S()},w=v.prototype,m=function(e,t,n){return new v(e,t||e.name||\"download\",n)};if(typeof navigator!==\"undefined\"&&navigator.msSaveOrOpenBlob){return function(e,t,n){t=t||e.name||\"download\";if(!n){e=p(e)}return navigator.msSaveOrOpenBlob(e,t)}}w.abort=function(){};w.readyState=w.INIT=0;w.WRITING=1;w.DONE=2;w.error=w.onwritestart=w.onprogress=w.onwrite=w.onabort=w.onerror=w.onwriteend=null;return m}(typeof self!==\"undefined\"&&self||typeof window!==\"undefined\"&&window||this.content);if(typeof module!==\"undefined\"&&module.exports){module.exports.saveAs=saveAs}else if(typeof define!==\"undefined\"&&define!==null&&define.amd!==null){define(\"FileSaver.js\",function(){return saveAs})}</script>
      <style>\n\nhtml {\n    overflow: hidden;\n    height: 100%;\n}\n\nbody {\n    height: 100%;\n    overflow: auto;\n}\n\n#debugger {\n  width: 100%;\n  height: 100%;\n  font-family: monospace;\n}\n\n#values {\n  display: block;\n  float: left;\n  height: 100%;\n  width: calc(100% - 30ch);\n  margin: 0;\n  overflow: auto;\n  cursor: default;\n}\n\n.debugger-sidebar {\n  display: block;\n  float: left;\n  width: 30ch;\n  height: 100%;\n  color: white;\n  background-color: rgb(61, 61, 61);\n}\n\n.debugger-sidebar-controls {\n  width: 100%;\n  text-align: center;\n  background-color: rgb(50, 50, 50);\n}\n\n.debugger-sidebar-controls-import-export {\n  width: 100%;\n  height: 24px;\n  line-height: 24px;\n  font-size: 12px;\n}\n\n.debugger-sidebar-controls-resume {\n  width: 100%;\n  height: 30px;\n  line-height: 30px;\n  cursor: pointer;\n}\n\n.debugger-sidebar-controls-resume:hover {\n  background-color: rgb(41, 41, 41);\n}\n\n.debugger-sidebar-messages {\n  width: 100%;\n  overflow-y: auto;\n  height: calc(100% - 24px);\n}\n\n.debugger-sidebar-messages-paused {\n  width: 100%;\n  overflow-y: auto;\n  height: calc(100% - 54px);\n}\n\n.messages-entry {\n  cursor: pointer;\n  width: 100%;\n}\n\n.messages-entry:hover {\n  background-color: rgb(41, 41, 41);\n}\n\n.messages-entry-selected, .messages-entry-selected:hover {\n  background-color: rgb(10, 10, 10);\n}\n\n.messages-entry-content {\n  width: 23ch;\n  padding-top: 4px;\n  padding-bottom: 4px;\n  padding-left: 1ch;\n  text-overflow: ellipsis;\n  white-space: nowrap;\n  overflow: hidden;\n  display: inline-block;\n}\n\n.messages-entry-index {\n  color: #666;\n  width: 5ch;\n  padding-top: 4px;\n  padding-bottom: 4px;\n  padding-right: 1ch;\n  text-align: right;\n  display: block;\n  float: right;\n}\n\n</style>
      <div id=\"app\" style=\"height:100%;width:100%\">\n
        <h2>re-frisk debugger</h2>\n
        <p>ENJOY!</p>\n
      </div>\n
    </body>\n
  </html>")