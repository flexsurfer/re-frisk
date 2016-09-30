(ns re-frisk.demo
  (:require [re-frisk.core :refer [enable-re-frisk! enable-frisk! add-data] :refer-macros [def-view]]
            [reagent.core :as reagent]
            [re-frame.core :as rf :refer [reg-event-db
                                          path
                                          reg-sub
                                          dispatch
                                          dispatch-sync
                                          subscribe]]
            [reagent.interop :refer-macros [$ $!]])
  (:require-macros [reagent.ratom :refer [reaction]]))

(enable-console-print!)

;; trigger a dispatch every second
(defonce time-updater (js/setInterval
                       #(dispatch [:timer (js/Date.)]) 1000))

(def initial-state
 {:timer (js/Date.)
  :time-color "#f88"
  :clock? true})


;; -- Event Handlers ----------------------------------------------------------


(reg-event-db                 ;; setup initial state
 :initialize                     ;; usage:  (dispatch [:initialize])
 (fn
  [db _]
  (merge db initial-state)))    ;; what it returns becomes the new state


(reg-event-db
 :time-color                     ;; usage:  (dispatch [:time-color 34562])
 (path [:time-color])            ;; this is middleware
 (fn
  [time-color [_ value]]        ;; path middleware adjusts the first parameter
  value))


(reg-event-db
 :timer
 (fn
  ;; the first item in the second argument is :timer the second is the
  ;; new value
  [db [_ value]]
  (assoc db :timer value)))    ;; return the new version of db


(reg-event-db
  :clock?
  (fn
    ;; the first item in the second argument is :timer the second is the
    ;; new value
    [db [_ value]]
    (assoc db :clock? value)))
;; -- Subscription Handlers ---------------------------------------------------


(reg-sub
 :timer
 (fn
  [db _]             ;; db is the value currently in the app-db atom
  (:timer db)))


(reg-sub
 :time-color
 (fn
  [db _]
  (:time-color db)))


(reg-sub
  :clock?
  (fn
    [db _]
    (:clock? db)))
;; -- View Components ---------------------------------------------------------

(def-view greeting
 [message]
 [:h1 message])


(def-view clock
 []
 (let [time-color (rf/subscribe [:time-color])
       timer (rf/subscribe [:timer])
       clock? (rf/subscribe [:clock?])
       _ (add-data :timer timer)]
  (fn clock-render
   []
   (let [time-str (-> @timer
                      .toTimeString
                      (clojure.string/split " ")
                      first)
         style {:style {:color @time-color}}]
    [:div.example-clock style time-str]))))


(def-view color-input
 []
 (let [time-color (subscribe [:time-color])]
  (fn color-input-render
   []
   [:div.color-input
    "Time color: "
    [:input {:type "text"
             :value @time-color
             :on-change #(dispatch [:time-color (-> % .-target .-value)])}]])))

(def-view simple-example
 []
 (reagent/create-class
   {
    :reagent-render (fn []
                      (let [clock? (rf/subscribe [:clock?])]
                        [:div
                         [greeting "Hello world, it is now"]
                         (when @clock? [clock])
                         [color-input]]))}))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
 []
 (dispatch-sync [:initialize])
 (enable-re-frisk!)
 (reagent/render [simple-example]
                 (js/document.getElementById "app")))
