(ns re-frisk.demo
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :as re-frame])
  (:require-macros [reagent.ratom :refer [reaction]]))

(enable-console-print!)

(def time-interval (atom nil))

(re-frame/reg-fx
 ::start-time
 (fn []
   (when @time-interval (js/clearInterval @time-interval))
   (reset! time-interval (js/setInterval #(re-frame/dispatch [::timer-db (js/Date.) "test"]) 1000))))

(re-frame/reg-fx
 ::stop-time
 (fn []
   (when @time-interval (js/clearInterval @time-interval))))

(def initial-state
 {:timer (js/Date.)
  :form1 true
  :jsobj js/setInterval
  :time-color "#f88"
  :clock? true})

;; -- Event Handlers ----------------------------------------------------------

(re-frame/reg-event-db                 ;; setup initial state
 :initialize-db                     ;; usage:  (dispatch [:initialize])
 (fn
  [db _]
  (merge db initial-state)))    ;; what it returns becomes the new state


(re-frame/reg-event-db
 :time-color                     ;; usage:  (dispatch [:time-color 34562])
 (re-frame/path [:time-color])            ;; this is middleware
 (fn
  [time-color [_ value]]        ;; path middleware adjusts the first parameter
  value))

(re-frame/reg-event-db
 ::timer-db
 (fn
  ;; the first item in the second argument is :timer the second is the
  ;; new value
  [db [_ value]]
  (assoc db :timer value)))    ;; return the new version of db


(re-frame/reg-event-db
 ::set-trace
 (fn
   ;; the first item in the second argument is :timer the second is the
   ;; new value
   [db [_ value]]
   (update db :traces conj value)))    ;; return the new version of db

(re-frame/reg-event-fx
  :clock?-db
  (fn
    ;; the first item in the second argument is :timer the second is the
    ;; new value
    [{db :db} [_ value]]
    {:db (assoc db :clock? value)}))

(re-frame/reg-event-db
  ::change-form
  (fn [db _]
    (update db :form1 not)))

(re-frame/reg-event-fx
 ::start-time
 (fn [_ _]
   {::start-time nil}))

(re-frame/reg-event-fx
 ::stop-time
 (fn [_ _]
   {::stop-time nil}))

(re-frame/reg-event-fx
 ::change-db1
 (fn [{db :db} _]
   {:db (assoc db :change-db 1)}))

(re-frame/reg-event-fx
 ::change-db2
 (fn [{db :db} _]
   {:db (assoc db :change-db 2)}))

(re-frame/reg-event-fx
 ::change-db3
 (fn [{db :db} _]
   {:db (assoc db :change-db 3)}))

(re-frame/reg-event-fx
 ::do-nothing1
 (fn [_ _]
   nil))

(re-frame/reg-event-fx
 ::do-nothing2
 (fn [_ _]
   nil))

(re-frame/reg-event-fx
 ::do-nothing3
 (fn [_ _]
   nil))

; -- Subscription Handlers ---------------------------------------------------


(re-frame/reg-sub
 :timer
 (fn
  [db _]             ;; db is the value currently in the app-db atom
  (:timer db)))

(re-frame/reg-sub
  :form1?
  (fn
    [db _]             ;; db is the value currently in the app-db atom
    (:form1 db)))


(re-frame/reg-sub
 :time-color
 (fn
  [db _]
  (:time-color db)))


(re-frame/reg-sub
  :clock?
  (fn
    [db _]
    (:clock? db)))

;; -- View Components ---------------------------------------------------------

(defn greeting
 [message]
 [:h1 message])

(defn clock
 []
 (let [time-color (re-frame/subscribe [:time-color])
       timer (re-frame/subscribe [:timer])]
  (fn clock-render
   []
   (let [time-str (-> @timer
                      .toTimeString
                      (clojure.string/split " ")
                      first)
         style {:style {:color @time-color}}]
    [:div.example-clock style time-str]))))

(defn color-input
 []
 (let [time-color (re-frame/subscribe [:time-color])]
  (fn color-input-render
   []
   [:div.color-input
    "Time color:"
    [:input {:type "text"
             :value @time-color
             :on-change #(re-frame/dispatch [:time-color (-> % .-target .-value)])}]])))

(defn form1 []
  (fn []
    (let [clock? (re-frame/subscribe [:clock?])]
      [:div
       [greeting "Hello world, it is now"]
       (when @clock? [clock])
       [color-input]])))

(defn form2 []
  (fn []
    [:div "form2"]))

(defn simple-example
 []
 (reagent/create-class
   {
    :reagent-render (fn []
                      (let [form1? (re-frame/subscribe [:form1?])]
                        [:div
                         (if @form1?
                           [form1]
                           [form2])
                         [:div]
                         [:div {:style {:background-color "#CCCCCC" :width 150 :margin-top 10}
                                :on-click #(re-frame/dispatch [::start-time])}
                          "start time"]
                         [:div {:style {:background-color "#CCCCCC" :width 150 :margin-top 10}
                                :on-click #(re-frame/dispatch [::stop-time])}
                          "stop time"]
                         [:div {:style {:background-color "#CCCCCC" :width 150 :margin-top 10}
                                :on-click #(re-frame/dispatch [::change-form])}
                          "change form"]
                         [:div {:style {:background-color "#CCCCCC" :width 150 :margin-top 10}
                                :on-click #(do (re-frame/dispatch [::change-db1])
                                               (re-frame/dispatch [::change-db2])
                                               (re-frame/dispatch [::change-db3]))}
                          "dispatch 3 events change app db"]
                         [:div {:style {:background-color "#CCCCCC" :width 150 :margin-top 10}
                                :on-click #(do (re-frame/dispatch [::do-nothing1])
                                               (re-frame/dispatch [::do-nothing2])
                                               (re-frame/dispatch [::do-nothing3]))}
                          "dispatch 3 events doing nothing"]]))}))

(defn mount []
  (rdom/render [simple-example] (js/document.getElementById "app")))

(defn on-js-reload []
  (mount))

;; -- Entry Point -------------------------------------------------------------

(defn ^:export run
 []
 (re-frame/dispatch-sync [:initialize-db])
 (mount))