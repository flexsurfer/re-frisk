(ns re-frisk.help
 (:require [reagent.ratom :refer-macros [reaction]]
           [re-frame.core :refer [subscribe]]
           [re-frame.registrar :refer [kind->id->handler]]))

(def fx {:db "reset! app-db with a new value. Expects a map. / re-frame's internal"
         :dispatch "`dispatch` one event. Expects a single vector. / re-frame's internal"
         :dispatch-n  "`dispatch` more than one event. Expects a list or vector of events. Something for which sequential? returns true. / re-frame's internal"
         :deregister-event-handler  "Removes a previously registered event handler. Expects either a single id (typically a keyword), or a seq of ids. / re-frame's internal"
         :dispatch-later "`dispatch` one or more events after given delays. Expects a collection of maps with two keys:  :`ms` and `:dispatch`. / re-frame's internal"})

(def cofx {:db "Adds to coeffects the value in `app-db`, under the key `:db`. / re-frame's internal"})

(def intrcp {:coeffects "An interceptor which injects re-frame :db coeffect. / re-frame's internal"
             :do-fx "An interceptor which actions a `context's` (side) `:effects`. For each key in the `:effects` map, call the `effects handler` previously registered using `reg-fx`. / re-frame's internal"
             :db-handler "An interceptor which wraps the kind of event handler given to `reg-event-db`. These handlers take two arguments;  `db` and `event`, and they return `db`. / re-frame's internal"
             :fx-handler "An interceptor which wraps the kind of event handler given to `reg-event-fx`. These handlers take two arguments;  `coeffects` and `event`, and they return `effects`. / re-frame's internal"})

(def intrcp-fn {:coeffects "Function which adds to coeffects the value in `app-db`, under the key `:db`. / re-frame's internal"
                :do-fx "Function which calls the `effects handler` previously registered using `reg-fx` for each key in the `:effects` map. / re-frame's internal"
                :db-handler "Function which calls the handler given to `reg-event-db`. This handler take two arguments;  `db` and `event`, and returns `db`. / re-frame's internal"
                :fx-handler "Function which calls the handler given to `reg-event-fx`. This handler take two arguments;  `coeffects` and `event`, and returns `effects`. / re-frame's internal"})

;;please do no look at this code, i don't have time to make it more clean :)
(defn re-frame-event []
  (into {} (map (fn [a]
                  (hash-map (first a)
                            (let [intc (map #(merge {(:id %1) (cond (and (= %2 0) (= (:id %1) :coeffects)) (:coeffects intrcp)
                                                                    (and (= %2 1) (= (:id %1) :do-fx)) (:do-fx intrcp)
                                                                    (= (:id %1) :db-handler) (:db-handler intrcp)
                                                                    (= (:id %1) :fx-handler) (:fx-handler intrcp)
                                                                    :else "id")}
                                                    (when (:before %1) {:before (cond (and (= %2 0) (= (:id %1) :coeffects)) (:coeffects intrcp-fn)
                                                                                      (= (:id %1) :db-handler) (:db-handler intrcp-fn)
                                                                                      (= (:id %1) :fx-handler) (:fx-handler intrcp-fn)
                                                                                      :else "fn")})
                                                    (when (:after %1) {:after (cond (and (= %2 1) (= (:id %1) :do-fx)) (:do-fx intrcp-fn)
                                                                                    :else "fn")}))
                                            (second a)
                                            (range (count (second a))))]
                              (hash-map (str (count intc) " interceptors")
                                        intc))))
                (filter #(not= (key %) :re-frisk/update-db) @(reaction (:event @kind->id->handler))))))

(defn re-frame-sub []
  (into {} (map #(let [k (first %)]
                   (hash-map k (subscribe [k])))
                (filter #(not= (first %) ::db) @(reaction (:sub @kind->id->handler))))))

(defn re-frame-fx []
  (map #(if (% fx) {% (% fx)} %) (keys (:fx @kind->id->handler))))

(defn re-frame-cofx []
  (map #(if (% cofx) {% (% cofx)} %) (keys (:cofx @kind->id->handler))))

(defn re-frame-handlers [kind->id->handler?]
  (when (and kind->id->handler? kind->id->handler)
    (let [event (reaction (re-frame-event))
          sub (reaction (re-frame-sub))]
      {:kind->id->handler {:event {(count @event) event}
                           :sub {(count @sub) sub}
                           :fx (reaction (re-frame-fx))
                           :cofx (reaction (re-frame-cofx))}})))
