(ns re-frisk.subs-graph
  (:require [re-frisk.ui.components.colors :as colors]
            [re-frisk.inlined-deps.reagent.v1v0v0.reagent.core :as reagent]))

(defonce network (atom nil))
(defonce reaction->operation (reagent/atom {}))
(defonce view->reactions (reagent/atom {}))
(defonce vis (atom nil))
(defonce doc (atom nil))
(defonce nodes (atom {}))
(defonce edges (atom {}))
(defonce options (clj->js {:physics
                           {:solver        "forceAtlas2Based"
                            :maxVelocity   30
                            :minVelocity   10
                            :stabilization {:iterations 30}}}))

(defn init [win document]
  (reset! vis (.-vis ^js win))
  (reset! doc document))

(defn set-root-node [reaction]
  (when-not (get @nodes reaction)
    (let [data {:id "app-db" :label "app-db" :color {:background :yellow}}]
      (swap! nodes assoc reaction data)
      (swap! reaction->operation assoc reaction "app-db")
      (when @network
        (.add (.-nodes ^js (:data @network)) (clj->js data))))))

(defn destroy []
  (when-let [network-js (:network @network)]
    (.destroy network-js)
    (reset! network nil)))

(defn create []
  (destroy)
  (when (and @vis @doc)
    (let [Network  (.-Network ^js @vis)
          DataSet  (.-DataSet ^js @vis)
          nodes-ds (DataSet. (clj->js (vals @nodes)))
          edges-ds (DataSet. (clj->js (vals @edges)))
          data     #js {:nodes nodes-ds
                        :edges edges-ds}]
      (when-let [container (.getElementById @doc "global-subs-graph-container")]
        (reset! network {:data    data
                         :network (Network. container data options)})))))

(defn update-subs [traces]
  (when-let [app-db-reaction (:app-db-reaction (first traces))]
    (set-root-node app-db-reaction))
  (doseq [{:keys [subs]} traces]
    (doseq [{:keys [operation reaction]} subs]
      (when reaction
        (swap! reaction->operation assoc reaction operation))))
  (let [new-nodes (atom {})]
    (doseq [{:keys [subs]} traces]
      (doseq [{:keys [op-type input-signals operation reaction]} subs]
        (when (not= op-type :create-class)
          (when (and (= op-type :render) input-signals)
            (swap! view->reactions assoc operation input-signals))
          (let [operation (str operation)]
            (when reaction
              (if-let [old-reaction (get @nodes operation)]
                (when (not= op-type (:op-type old-reaction))
                  (let [updated-node (assoc old-reaction
                                       :op-type op-type
                                       :color {:background (get colors/sub-colors op-type)})]
                    (swap! nodes assoc operation updated-node)
                    (when @network
                      (if (get @new-nodes operation)
                        (swap! new-nodes assoc operation updated-node)
                        (.update (.-nodes ^js (:data @network)) (clj->js [updated-node]))))))
                (let [data {:id      operation :label operation :color {:background (get colors/sub-colors op-type)}
                            :font {:color :white}
                            :op-type op-type}]
                  (swap! nodes assoc operation data)
                  (swap! new-nodes assoc operation data))))
            (when input-signals
              (doseq [input-reaction input-signals]
                (let [input-operation (str (get @reaction->operation input-reaction))
                      reaction-path (str input-operation "-" operation)]
                  (if-let [old-edge (get @edges reaction-path)]
                    (let [updated-edge (update old-edge :value inc)]
                      (swap! edges assoc reaction-path updated-edge)
                      (when @network
                        (.update (.-edges ^js (:data @network)) (clj->js [updated-edge]))))
                    (let [data {:id reaction-path :from input-operation :to operation :value 1}]
                      (swap! edges assoc reaction-path data)
                      (when @network
                        (.add (.-edges ^js (:data @network)) (clj->js data))))))))))))
    (when @network
      (if (> (count @new-nodes) 20)
        (create)
        (doseq [data (vals @new-nodes)]
          (.add (.-nodes ^js (:data @network)) (clj->js data)))))))

(defonce event-network (atom nil))

(defn create-event-subs [{:keys [app-db-reaction subs]}]
  (when @event-network
    (.destroy @event-network)
    (reset! event-network nil))
  (when (and @vis @doc)
    (when-let [container (.getElementById @doc "event-subs-graph-container")]
      (let [Network  (.-Network ^js @vis)
            DataSet  (.-DataSet ^js @vis)
            nodes (atom {app-db-reaction {:id app-db-reaction :label "app-db" :color {:background :yellow}}})
            edges (atom {})]
        (doseq [{:keys [op-type reaction input-signals operation]} subs]
          (if-let [old-reaction (get @nodes reaction)]
            (when (not= op-type (:op-type old-reaction))
              (let [updated-node (assoc old-reaction
                                   :op-type op-type
                                   :color {:background (get colors/sub-colors op-type)})]
                (swap! nodes assoc reaction updated-node)))
            (let [data {:id      reaction :label operation :color {:background (get colors/sub-colors op-type)}
                        :font {:color :white}
                        :op-type op-type}]
              (swap! nodes assoc reaction data)))
          (when input-signals
            (doseq [input-reaction input-signals]
              (when-not (get @edges (str input-reaction "-" reaction))
                (swap! edges assoc (str input-reaction "-" reaction) {:from input-reaction :to reaction})))))
        (reset! event-network (Network. container #js {:nodes (DataSet. (clj->js (vals @nodes)))
                                                       :edges (DataSet. (clj->js (vals @edges)))} options))))))
