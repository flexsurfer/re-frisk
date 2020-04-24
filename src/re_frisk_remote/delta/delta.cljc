(ns re-frisk-remote.delta.delta
  (:require [clojure.set :as set])
  (:refer-clojure :exclude [apply]))

;; diff description:
;; [:replace x] -- replace current node with x
;; [:set #{1 2} #{3 4}] -- add 1 2 and to the current node, remove 3 4
;; [:map {1 2} #{3 4} {5 Z}] -- add (1 2) pair to current node, remove 3 4,
;;   apply patch Z to the value of key 5
;; [:seq 0 {6 Z}] -- apply patch Z to the value of key 6
;; [:seq [5 6] {6 Z}] -- add values 5 and 6
;; [:seq 3 {6 Z}] -- pop the last 3 values,
;;   apply patch Z to the value of key 6

(declare delta)

;; Don't pass empty collections around, replace them with nulls
(defn nullify [a]
  (if (empty? a) nil a))

(defn- delta-set [a b]
  [:set (nullify (set/difference b a)) (nullify (set/difference a b))])

(defn- ff [a b k]
  (when (not= (a k) (b k))
    [k (delta (a k) (b k))]))

(defn- delta-map-vals [a b ks]
  (nullify (into {} (filter some? (map #(ff a b %) ks)))))

(defn- delta-map [a b]
  (let [akeys (set (keys a))
        bkeys (set (keys b))
        common (set/intersection akeys bkeys)
        add (set/difference bkeys akeys)
        remove (nullify (set/difference akeys bkeys))]
    [:map (nullify (select-keys b add)) remove (delta-map-vals a b common)]))

(defn- delta-seq-vals [n a b]
  (let [items (map vector (range) a b)]
    (->> items
         (map (fn [[i a b]] (when (not= a b) [i (delta a b)])))
         (filter some?)
         (into {}))))

(defn- delta-vec [a b]
  (let [ca (count a)
        cb (count b)
        mc (min ca cb)
        tail (if (<= cb ca) (- ca cb) (subvec b ca))]
    [:vec tail (nullify (delta-seq-vals mc a b))]))

(defn delta [a b]
  (cond
    (= a b) nil
    (and (map? a) (map? b)) (delta-map a b)
    (and (set? a) (set? b)) (delta-set a b)
    (and (list? a) (list? b)) [:replace b]
    (and (vector? a) (vector? b)) (delta-vec a b)
    :else [:replace b]))

(declare apply)

(defn- apply-set [a [add remove]]
  (set/difference (set/union a add) remove))

;; [1 2 3] {1 [:set 7]} -> [1 7 3]
(defn- apply-vec-changes [a changes]
  (reduce (fn [a [k ch]] (update-in a [k] #(apply % ch))) a changes))

(defn- apply-vec [a [tail changes]]
  (let [h (apply-vec-changes a changes)]
    (cond
      (= 0 tail) h
      (integer? tail) (subvec h 0 (- (count h) tail))
      :else (vec (concat h tail)))))

;; {1 2 3 4} {3 [:set 6]} -> {1 2 3 6}
(defn apply-map-changes [a changes]
  (reduce (fn [a [k ch]] (update-in a [k] #(apply % ch))) a changes))

(defn- apply-map [a [keys-add keys-remove changes]]
  (as-> a A
      (apply-map-changes A changes)
      (merge keys-add A)
      (clojure.core/apply dissoc A keys-remove)))

(defn apply [a patch]
  (case (first patch)
    nil a
    :replace (second patch)
    :set (apply-set a (rest patch))
    :vec (apply-vec a (rest patch))
    :map (apply-map a (rest patch))))
