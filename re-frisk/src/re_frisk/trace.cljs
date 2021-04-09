(ns re-frisk.trace
  (:require [re-frisk.diff.diff :as diff]
            [re-frisk.utils :as utils]
            [re-frame.trace]
            [re-frame.interop :as interop]
            [re-frame.db :as db]))

(defonce mounted-views (atom {}))

(declare call-timeout)
(defonce call-state (atom nil))

(defn call-and-chill [handler]
  (if @call-state
    (reset! call-state :call)
    (do
      (reset! call-state :chill)
      (js/setTimeout call-timeout 500 handler)
      (js/setTimeout handler))))

(defn- call-timeout [handler]
  (let [state @call-state]
    (reset! call-state nil)
    (when (= state :call)
      (call-and-chill handler))))

(defn update-views-and-get-traces [send-views traces]
  (call-and-chill #(send-views @mounted-views))
  (reduce (fn [items {:keys [op-type operation tags] :as item}]
            (let [op-type (if (= (namespace op-type) "sub") :sub op-type)]
              (if (#{:event :event/handler :event/do-fx :sub :render :force-update :create-class :raf
                     :raf-end :should-upd} op-type)
                (conj items item)
                (do
                  (when (#{:componentWillUnmount :componentDidMount} op-type)
                    (if (= op-type :componentDidMount)
                        (swap! mounted-views assoc operation {:name operation :order (:order tags)})
                        (swap! mounted-views dissoc operation)))
                  items))))
          []
          traces))

(defn normalize-traces [traces ignore-events]
  (reduce (fn [items {:keys [op-type tags duration id] :as trace}]
            (let [op-type (if (= (namespace op-type) "sub") :sub op-type)
                  item    {:indx id :trace? true}]
              (case op-type
                ;:re-frame.router/fsm-trigger
                #_(conj items (merge item
                                     (select-keys trace [:id :op-type :operation :start :end])))
                :event
                (when (or (not ignore-events) (not (get ignore-events (first (:event tags)))))
                  (conj items (merge (dissoc item :trace?)
                                     (assoc (select-keys trace [:id :op-type :operation :duration
                                                                :start :end :effects :coeffects])
                                       :event (:event tags)
                                       :effects (cond-> (:effects tags)
                                                        (:db (:effects tags))
                                                        (assoc :db {}))
                                       :coeffects (dissoc (:coeffects tags) :db :event :original-event)
                                       :truncated-name (utils/truncate-name (str (first (:event tags))))
                                       :app-db-diff (diff/diff (:app-db-before tags) (:app-db-after tags))))))
                :event/handler
                (let [prev (peek items)]
                  (when (= (:op-type prev) :event)
                    (conj (pop items) (assoc prev :handler-duration duration))
                    #_(conj items (merge item
                                         (select-keys trace [:id :op-type :operation :duration])))))
                :event/do-fx
                (let [prev (peek items)]
                  (when (= (:op-type prev) :event)
                    (conj (pop items) (assoc prev :fx-duration duration))
                    #_(conj items (merge item
                                         (select-keys trace [:id :op-type :duration])))))
                (:sub :render :force-update :create-class :should-upd)
                (let [prev  (peek items)
                      trace (select-keys trace [:id :op-type :operation :duration :start :end])
                      trace (assoc trace :duration-ms (utils/str-ms (:duration trace))
                                         :reaction (:reaction tags)
                                         :cached? (:cached? tags)
                                         :input-signals (:input-signals tags))]
                  (if (and (:subs? prev) (not (:raf-end prev)))
                    (conj (pop items) (update prev :subs conj trace))
                    (conj items (merge item
                                       {:op-type         :subs
                                        :subs?           true
                                        :subs            [trace]
                                        :app-db-reaction (interop/reagent-id db/app-db)
                                        :start           (:start trace)}))))

                :raf
                (conj items (merge item
                                   {:op-type         :subs
                                    :subs?           true
                                    :subs            []
                                    :app-db-reaction (interop/reagent-id db/app-db)
                                    :start           (:start trace)
                                    :raf (select-keys trace [:sart :end :duration])}))

                :raf-end
                (let [prev (peek items)]
                  (if (empty? (:subs prev))
                    (pop items)
                    (conj (pop items) (assoc prev :raf-end true))))

                items)))
   []
   (sort-by :id traces)))

(defn normalize-durations [first-event]
  (fn [{:keys [subs? subs op-type handler-duration fx-duration raf]
        :as   trace}]
    (let [{:keys [duration handler-duration fx-duration start created-duration-cached shoud-update-duration
                  run-duration created-duration disposed-duration render-duration create-class-duration force-duration]
           :as   trace}
          (cond
            subs?
            (merge trace
                   (reduce (fn [acc {:keys [duration op-type end cached?]}]
                             (cond-> (update acc :duration + duration)
                                     :always
                                     (assoc :end end)
                                     (= op-type :sub/run)
                                     (-> (update :run-count inc)
                                         (update :run-duration + duration))
                                     (and (= op-type :sub/create) (not cached?))
                                     (-> (update :created-count inc)
                                         (update :created-duration + duration))
                                     (and (= op-type :sub/create) cached?)
                                     (-> (update :created-count-cached inc)
                                         (update :created-duration-cached + duration))
                                     (= op-type :sub/dispose)
                                     (-> (update :disposed-count inc)
                                         (update :disposed-duration + duration))
                                     (= op-type :render)
                                     (-> (update :render-count inc)
                                         (update :render-duration + duration))
                                     (= op-type :force-update)
                                     (-> (update :force-count inc)
                                         (update :force-duration + duration))
                                     (= op-type :create-class)
                                     (-> (update :create-class-count inc)
                                         (update :create-class-duration + duration))
                                     (= op-type :should-upd)
                                     (-> (update :shoud-update-count inc)
                                         (update :shoud-update-duration + duration))))
                           {:duration                0
                            :run-count               0
                            :run-duration            0
                            :render-count            0
                            :render-duration         0
                            :force-count             0
                            :force-duration          0
                            :created-count           0
                            :created-duration        0
                            :disposed-count          0
                            :disposed-duration       0
                            :created-count-cached    0
                            :created-duration-cached 0
                            :create-class-count      0
                            :create-class-duration   0
                            :shoud-update-count      0
                            :shoud-update-duration   0}
                           subs)
                   (when raf
                     raf))
            (= op-type :event)
            (let [handler-fx-duration (+ handler-duration fx-duration)]
              (assoc trace :handler-fx-duration handler-fx-duration
                           :handler-fx-duration-ms (utils/str-ms handler-fx-duration)))
            :else
            trace)]
      (cond-> trace
              duration
              (assoc :duration-ms (utils/str-ms duration))
              handler-duration
              (assoc :handler-duration-ms (utils/str-ms handler-duration))
              fx-duration
              (assoc :fx-duration-ms (utils/str-ms fx-duration))
              run-duration
              (assoc :run-duration-ms (utils/str-ms run-duration))
              created-duration
              (assoc :created-duration-ms (utils/str-ms created-duration))
              created-duration-cached
              (assoc :created-duration-cached-ms (utils/str-ms created-duration-cached))
              disposed-duration
              (assoc :disposed-duration-ms (utils/str-ms disposed-duration))
              render-duration
              (assoc :render-duration-ms (utils/str-ms render-duration))
              create-class-duration
              (assoc :create-class-duration-ms (utils/str-ms create-class-duration))
              shoud-update-duration
              (assoc :shoud-update-duration-ms (utils/str-ms shoud-update-duration))
              force-duration
              (assoc :force-duration-ms (utils/str-ms force-duration))
              :always
              (assoc :position (- start (:start first-event)))))))