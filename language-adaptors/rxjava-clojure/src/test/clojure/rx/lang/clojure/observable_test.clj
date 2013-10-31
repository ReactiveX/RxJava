(ns rx.lang.clojure.observable-test
  (:require [rx.lang.clojure.observable :as rx]
            [clojure.test :refer [deftest testing is]]))

(deftest test-exercise-generated-wrapper
  (is (= 12 (-> (rx/just 6)
                (rx/map #(* 2 %))
                rx.observables.BlockingObservable/single)))

  (is (= (+ 1 4 9 16 25)
         (-> (rx/from-& [1 2 3 4 5])
             (rx/map (fn [v] (* v v)))
             (rx/reduce +)
             rx.observables.BlockingObservable/single)))

  (is (= [1 2 3]
         (-> (rx/from-& [3 2 1])
             rx/to-sorted-list
             rx.observables.BlockingObservable/single)))

  (is (= [1 2 3 4 5 6]
         (-> (rx/concat-& [(rx/from-& [1 2 3]) (rx/from-& [4 5 6])])
             rx/to-list
             rx.observables.BlockingObservable/single)))

  (is (= [{:a 1 :b 2 :c 3}  {:a 4 :b 5 :c 6} {:a 7 :b 8 :c 9}]
         (-> (rx/zip (rx/from-& [1 4 7]) (rx/from-& [2 5 8]) (rx/from-& [3 6 9])
                    (fn [a b c] {:a a :b b :c c}))
                    rx/to-list
                    rx.observables.BlockingObservable/single))))
