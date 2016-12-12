(ns re-frisk.core
  (:require [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [re-frisk.data :refer [re-frame-events re-frame-data initialized deb-data]]
            [re-frisk.devtool :as d]
            [re-frisk.ui :as ui]
            [re-frisk.help :as h]
            [re-frame.registrar :refer [kind->id->handler]]
            [re-frame.core :refer [reg-sub reg-event-db subscribe] :as rfr]))

(defn post-event-callback [value]
  (let [cntx ((first value) (:contexts @deb-data))]
    (swap! re-frame-events conj
           (if cntx
             (assoc cntx :event value)
             value))))

(defn- render-re-frisk [params]
  (let [div (js/document.createElement "div")]
    (js/document.body.appendChild div)
    (set! js/window.onbeforeunload #(when (:win @deb-data) (.alert (:win @deb-data) "Application has been closed or refreshed. Debugger has been stoped!")))
    (r/render [ui/re-frisk-shell re-frame-data (merge {:on-click d/open-debugger-window} params)] div)))

(defn enable-re-frisk! [& params]
  (when-not @initialized
    (let [prefs (first params)
          event (reaction (into {} (map (fn [a]
                                          (hash-map (first a)
                                                    (let [intc (map #(merge {(:id %1) (cond (and (= %2 0) (= (:id %1) :coeffects)) (:coeffects h/intrcp)
                                                                                            (and (= %2 1) (= (:id %1) :do-fx)) (:do-fx h/intrcp)
                                                                                            (= (:id %1) :db-handler) (:db-handler h/intrcp)
                                                                                            (= (:id %1) :fx-handler) (:fx-handler h/intrcp)
                                                                                            :else "id")}
                                                                            (when (:before %1) {:before (cond (and (= %2 0) (= (:id %1) :coeffects)) (:coeffects h/intrcp-fn)
                                                                                                              (= (:id %1) :db-handler) (:db-handler h/intrcp-fn)
                                                                                                              (= (:id %1) :fx-handler) (:fx-handler h/intrcp-fn)
                                                                                                                :else "fn")})
                                                                            (when (:after %1) {:after (cond (and (= %2 1) (= (:id %1) :do-fx)) (:do-fx h/intrcp-fn)
                                                                                                             :else "fn")}))
                                                                    (second a)
                                                                    (range (count (second a))))]
                                                      (hash-map (str (count intc) " interceptors")
                                                                intc))))
                                        (filter #(not= (key %) :re-frisk/update-db) @(reaction (:event @kind->id->handler))))))
          sub (reaction (into {} (map #(let [k (first %)]
                                         (hash-map k (subscribe [k])))
                                      (filter #(not= (first %) ::db) @(reaction (:sub @kind->id->handler))))))]
      (reg-sub ::db (fn [db _] db))
      (reset! re-frame-data {:handlers {:event {(count @event) event}
                                        :sub {(count @sub) sub}
                                        :fx (reaction (map #(if (% h/fx) {% (% h/fx)} %) (keys (:fx @kind->id->handler))))
                                        :cofx (reaction (map #(if (% h/cofx) {% (% h/cofx)} %) (keys (:cofx @kind->id->handler))))}
                             :app-db (subscribe [::db])})
      (reset! initialized true)
      (swap! deb-data assoc :prefs prefs)
      (when-not (= (:events? prefs) false)
        (rfr/add-post-event-callback post-event-callback))
      (js/setTimeout render-re-frisk 100 prefs))))

(defn enable-frisk! [& params]
  (when-not @initialized
    (do
      (reset! initialized true)
      (js/setTimeout render-re-frisk 100 (first params)))))

(defn add-data [key data]
    (swap! re-frame-data assoc key data))

(defn add-in-data [keys data]
    (swap! re-frame-data assoc-in keys data))

(def watch-context
  (re-frame.core/->interceptor
    :id      :re-frisk-watch-context
    :before  (fn [context]
               (swap! deb-data assoc-in [:contexts (-> context :coeffects :event first) :before] context)
               context)))

(defn reg-view [view subs events]
  (when (:app-db @re-frame-data)
    (do
      (swap! re-frame-data assoc-in [:views view :events] (set events))
      (swap! re-frame-data assoc-in [:views view :subs] (into {} (map #(hash-map % (subscribe [%])) subs)))
      (doseq [s subs]
        (swap! re-frame-data assoc-in [:subs s] (subscribe [s]))))))

(defn unmount-view [view]
  (when (:app-db @re-frame-data)
    (swap! re-frame-data update-in [:views] dissoc view)))

(reg-event-db :re-frisk/update-db (fn [db [_ value]] value))

