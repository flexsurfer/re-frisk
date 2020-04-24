(ns re-frisk.delta-test
  (:require [clojure.test.check.clojure-test :as ct :include-macros true]
            [clojure.test.check.generators :as gen]
            [clojure.test.check]
            [clojure.test.check.properties :as prop :include-macros true]
            [re-frisk-remote.delta.delta :as delta]))

;; NaNs make everything hard, and break clojure.core/=
(defn nonans [a]
  (cond
    (set? a) (every? nonans a)
    (sequential? a) (every? nonans a)
    (associative? a) (and (every? nonans (keys a)) (every? nonans (vals a)))
    :else (not (js/Number.isNaN a))))

(def any-no-nan (gen/such-that nonans gen/any))

(ct/defspec delta-patch-is-identity
  {:num-tests 10000 :max-size 15}
  (prop/for-all [a any-no-nan b any-no-nan]
                (= (delta/apply a (delta/delta a b)) b)))
