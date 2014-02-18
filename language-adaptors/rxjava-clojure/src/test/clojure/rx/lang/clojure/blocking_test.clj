(ns rx.lang.clojure.blocking-test
  (:require [rx.lang.clojure.blocking :as b]
            [rx.lang.clojure.core :as rx]
            [clojure.test :refer [deftest testing is]]))

(deftest test-first
  (testing "returns first element of observable"
    (is (= 1 (b/first (rx/return 1)))))
  (testing "returns nil for empty observable"
    (is (nil? (b/first (rx/empty))))))

(deftest test-single
  (testing "returns one element"
    (is (= 1 (b/single (rx/return 1)))))
  (testing "throw if empty"
    (is (thrown? java.lang.IllegalArgumentException (b/single (rx/empty)))))
  (testing "throw if many"
    (is (thrown? java.lang.IllegalArgumentException (b/single (rx/seq->o [1 2]))))))
