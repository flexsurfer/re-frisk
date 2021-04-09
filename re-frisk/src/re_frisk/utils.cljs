(ns re-frisk.utils
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [re-frame.subs :as subs]))

(defn sort-map [value checkbox-val checkbox]
  (if (and checkbox-val (map? value))
    (try
      (into (sorted-map) value)
      (catch :default e
        (do
          (reset! checkbox false)
          value)))
    value))

(defn sort-map-by [value comp]
  (if (map? value)
    (try
      (into (sorted-map-by comp) value)
      (catch :default e
        value))
    value))

(defn on-change-sort [value checkbox-val key]
  (fn [val]
    (reset! checkbox-val val)
    (swap! value assoc key true)
    (js/setTimeout #(swap! value dissoc key) 100)))

(defn filter-event [text]
  (fn [item]
    (or (:trace? item)
        (let [name (string/lower-case (name (first (:event item))))
              text (string/lower-case text)]
          (not= (string/index-of name text) nil)))))

(defn truncate-name [event]
  (let [namespace (string/split (string/replace event #":" "") #"/")]
    (if (> (count namespace) 1)
      (str ":" (string/join "." (mapv first (string/split (first namespace) #"\.")))
           "/" (last namespace))
      event)))

(defn normalize-draggable [x]
  (when x
    (cond (< x 80) 80
          (> x (- js/window.innerWidth 30)) (- js/window.innerWidth 30)
          :else x)))

(defn closed? [left]
  (= left (- js/window.innerWidth 30)))

(defn str-ms [value]
  (when-not (string/blank? value)
    (str (gstring/format "%.2f" value) " ms")))

(declare call-timeout)
(defonce call-state (atom nil))

(defn call-and-chill [handler time]
  (if @call-state
    (reset! call-state :call)
    (do
      (reset! call-state :chill)
      (js/setTimeout call-timeout time handler time)
      (handler))))

(defn- call-timeout [handler time]
  (let [state @call-state]
    (reset! call-state nil)
    (when (= state :call)
      (call-and-chill handler time))))

(defn scroll-timeline-event-item [doc indx]
  (when-let [elem (.getElementById doc (str "timeline-event-item" indx))]
    (.scrollIntoView elem #js {:inline "center"})))

(defn scroll-event-list-item [doc indx]
  (when-let [elem (.getElementById doc (str "events-list-item" indx))]
    (.scrollIntoView elem #js {:block "center"})))

(defn get-subs []
  (reduce-kv #(assoc %1 %2 (deref %3)) {} @subs/query->reaction))