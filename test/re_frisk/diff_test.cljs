(ns re-frisk.diff-test
  (:require [cljs.test :refer [deftest is]]
            [re-frisk.diff :as sut]))

(deftest test-empty
  (is (= nil (sut/diff 1 1))))

(deftest test-change-scalar
  (is (= {:was 1 :now 2} (sut/diff 1 2))))

(deftest test-change-type
  (is (= {:was 1 :now [42]} (sut/diff 1 [42]))))

(deftest test-change-vector
  (is (= {2 2}
         (sut/diff [0 1] [0 1 2])))
  (is (= {2 {:deleted 2}}
         (sut/diff [0 1 2] [0 1])))
  (is (= {2 {:was 2 :now 3}}
         (sut/diff [0 1 2] [0 1 3]))))

(deftest test-change-set
  (is (= {:deleted #{2}
          :added #{3}}
         (sut/diff #{1 2} #{1 3}))))

(deftest test-change-map
  (is (= {:a {:deleted 1}
          :b {:was 2 :now -2}
          :c 3}
         (sut/diff {:a 1 :b 2} {:b -2 :c 3}))))

(deftest test-change-deep
  (is (= {:a {2 {:deleted 2}}}
         (sut/diff {:a [0 1 2]} {:a [0 1]}))))
