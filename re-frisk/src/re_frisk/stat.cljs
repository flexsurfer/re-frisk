(ns re-frisk.stat
  (:require [re-frame.registrar :as reg]))

(defn assoc-map [acc key]
  (assoc acc key {:cnt 0 :ms 0}))

(defn get-re-frame-handlers []
  {:fx    (reduce assoc-map {} (keys (dissoc (:fx @reg/kind->id->handler)
                                             :dispatch-later
                                             :fx
                                             :dispatch
                                             :dispatch-n
                                             :deregister-event-handler
                                             :db)))
   :cofx  (reduce assoc-map {} (keys (dissoc (:cofx @reg/kind->id->handler) :db)))
   :event (reduce assoc-map {} (keys (:event @reg/kind->id->handler)))
   :sub   (reduce assoc-map {} (keys (:sub @reg/kind->id->handler)))})

(defn init-stat [re-frame-data]
  (when (empty? @(:stat re-frame-data))
    (reset! (:stat re-frame-data) (get-re-frame-handlers))))

(defn update-trace-stat [re-frame-data traces]
  (doseq [{:keys [event subs duration effects coeffects]} traces]
    (when event
      (swap! (:stat re-frame-data) update-in [:event (first event) :cnt] inc)
      (swap! (:stat re-frame-data) update-in [:event (first event) :ms] + duration)
      (when (pos? (count effects))
        (doseq [key (keys effects)]
          (swap! (:stat re-frame-data) update-in [:fx key :cnt] inc)))
      (when (pos? (count coeffects))
        (doseq [key (keys coeffects)]
          (swap! (:stat re-frame-data) update-in [:cofx key :cnt] inc))))
    (when (seq subs)
      (doseq [{:keys [op-type operation duration]} subs]
        (when (= op-type :sub/run)
          (swap! (:stat re-frame-data) update-in [:sub operation :cnt] inc)
          (swap! (:stat re-frame-data) update-in [:sub operation :ms] + duration))))))

(defn update-event-stat [re-frame-data event]
  (swap! (:stat re-frame-data) update-in [:event event :cnt] inc))