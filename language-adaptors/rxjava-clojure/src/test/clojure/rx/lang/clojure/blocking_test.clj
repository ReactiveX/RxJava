(ns rx.lang.clojure.blocking-test
  (:require [rx.lang.clojure.blocking :as b]
            [rx.lang.clojure.core :as rx]
            [clojure.test :refer [deftest testing is]]))

(deftest test-->blocking
  (testing "returns a BlockingObservable from an Observable"
    (is (instance? rx.observables.BlockingObservable (b/->blocking (rx/return 0)))))
  
  (testing "is idempotent"
    (is (instance? rx.observables.BlockingObservable (b/->blocking (b/->blocking (rx/return 0)))))))


(deftest test-o->seq
  (is (= [1 2 3] (b/o->seq (rx/seq->o [1 2 3])))))

(deftest test-first
  (testing "returns first element of observable"
    (is (= 1 (b/first (rx/seq->o [1 2 3])))))
  (testing "returns nil for empty observable"
    (is (nil? (b/first (rx/empty)))))
  (testing "rethrows errors"
    (is (thrown? java.io.FileNotFoundException
                 (b/first (rx/throw (java.io.FileNotFoundException. "boo")))))))

(deftest test-last
  (testing "returns last element of observable"
    (is (= 3 (b/last (rx/seq->o [1 2 3])))))
  (testing "returns nil for empty observable"
    (is (nil? (b/last (rx/empty)))))
  (testing "rethrows errors"
    (is (thrown? java.io.FileNotFoundException
                 (b/last (rx/throw (java.io.FileNotFoundException. "boo")))))))

(deftest test-single
  (testing "returns one element"
    (is (= 1 (b/single (rx/return 1)))))
  (testing "throw if empty"
    (is (thrown? java.util.NoSuchElementException (b/single (rx/empty)))))
  (testing "throw if many"
    (is (thrown? java.lang.IllegalArgumentException (b/single (rx/seq->o [1 2])))))
  (testing "rethrows errors"
    (is (thrown? java.io.FileNotFoundException
                 (b/single (rx/throw (java.io.FileNotFoundException. "boo"))))))) 

(deftest test-into
  (is (= [1 2 3]
         (b/into [1] (rx/seq->o [2 3]))))
  (testing "rethrows errors"
    (is (thrown? java.io.FileNotFoundException
                 (b/into #{} (rx/throw (java.io.FileNotFoundException. "boo")))))))

(deftest test-doseq
  (is (= (range 3)
         (let [capture (atom [])]
           (b/doseq [{:keys [value]} (rx/seq->o (map #(hash-map :value %) (range 3)))]
             (println value)
             (swap! capture conj value))
           @capture)))

  (testing "rethrows errors"
    (is (thrown? java.io.FileNotFoundException
                 (b/doseq [i (rx/seq->o (range 3))]
                   (throw (java.io.FileNotFoundException. "boo")))))))

