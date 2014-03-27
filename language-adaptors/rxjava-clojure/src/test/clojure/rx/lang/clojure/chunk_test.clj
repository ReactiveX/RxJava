(ns rx.lang.clojure.chunk-test
  (:require [rx.lang.clojure.chunk :as rx-chunk]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.future :as rx-future]
            [rx.lang.clojure.blocking :as rx-blocking]
            [clojure.test :refer [deftest testing is]]))


(deftest test-chunk
  (let [n          20
        chunk-size 10
        factory (rx-future/future-generator*
                  future-call
                  (fn[o]
                    (doseq [i (range n)]
                      (Thread/sleep (rand-int 50))
                      (rx/on-next o (rx-future/future*
                                      future-call
                                      #(let [t (rand-int 500)]
                                         (Thread/sleep t)
                                         i))))))]
    (is (= (range n)
           (sort (rx-blocking/into []
                                   (rx-chunk/chunk chunk-size {:debug true} factory)))))))

(deftest test-chunk-with-error
  (testing "error from source is propagated"
    (let [n          20
          chunk-size 4
          factory (rx-future/future-generator*
                    future-call
                    (fn [o]
                      (doseq [i (range n)]
                        (Thread/sleep (rand-int 50))
                        (rx/on-next o (rx-future/future*
                                        future-call
                                        #(let [t (rand-int 1000)]
                                           (Thread/sleep t)
                                           i))))
                      (throw (IllegalArgumentException. "hi"))))]
      (is (thrown-with-msg? IllegalArgumentException #"hi"
                            (rx-blocking/into []
                                              (rx-chunk/chunk chunk-size {:debug true} factory))))))

  (testing "error from single observable is propagated"
    (let [n          20
          chunk-size 4
          factory (rx-future/future-generator*
                    future-call
                    (fn [o]
                      (doseq [i (range n)]
                        (Thread/sleep (rand-int 50))
                        (rx/on-next o (rx-future/future*
                                        future-call
                                        #(let [t (rand-int 1000)]
                                           (throw (IllegalArgumentException. "byebye"))
                                           (Thread/sleep t)
                                           i))))))]
      (is (thrown? rx.exceptions.CompositeException
                   (rx-blocking/into []
                                     (rx-chunk/chunk chunk-size
                                                 {:debug true
                                                  :delay-error? true }
                                                 factory)))))))

