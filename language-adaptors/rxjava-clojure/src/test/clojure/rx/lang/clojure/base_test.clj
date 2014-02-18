(ns rx.lang.clojure.base-test
  (:require [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.base :as b]
            [rx.lang.clojure.blocking :as blocking]
            [rx.lang.clojure.future :as f]
            )
  (:require [clojure.test :refer [deftest testing is]]))

(deftest test-zip
  (testing "is happy with less than 4 args"
    (is (= [[1 2 3]] (blocking/into [] (b/zip vector
                                        (rx/seq->o [1]) (rx/seq->o [2]) (rx/seq->o [3]))))))
  (testing "is happy with more than 4 args"
    (is (= [[1 2 3 4 5 6 7 8]]
           (blocking/into [] (b/zip vector
                              (rx/seq->o [1])
                              (rx/seq->o [2])
                              (rx/seq->o [3])
                              (rx/seq->o [4])
                              (rx/seq->o [5])
                              (rx/seq->o [6])
                              (rx/seq->o [7])
                              (rx/seq->o [8])))))))

(deftest test-merge
  (is (= [[1 3 5] [2 4 6]]
         (let [r (blocking/into []
                   (b/merge [(f/future-generator f/default-runner [o]
                                                   (doseq [x [1 3 5]]
                                                     (Thread/sleep 10)
                                                     (rx/on-next o x)))
                              (f/future-generator f/default-runner [o]
                                                   (doseq [x [2 4 6]]
                                                     (Thread/sleep 10)
                                                     (rx/on-next o x)))]))]
           ; make sure each sequence maintained original order
           [(keep #{1 3 5} r)
            (keep #{2 4 6} r) ]))))
