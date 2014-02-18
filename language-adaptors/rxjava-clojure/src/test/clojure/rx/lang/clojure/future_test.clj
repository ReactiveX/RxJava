(ns rx.lang.clojure.future-test
  (:require [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.blocking :as blocking]
            [rx.lang.clojure.future :as f])
  (:require [clojure.test :refer [deftest testing is]]))

(deftest test-future-generator
  (is (not= [(.getId (Thread/currentThread))]
            (blocking/into []
                      (f/future-generator f/default-runner
                        [observer]
                        (rx/on-next observer (.getId (Thread/currentThread))))))))

(deftest test-future
  (is (= [15] (blocking/into [] (f/future* f/default-runner + 1 2 3 4 5))))
  (is (= [15] (blocking/into [] (f/future f/default-runner (println "HI") (+ 1 2 3 4 5))))) )


(comment (rx/subscribe (f/future* f/default-runner + 1 2 3 4 5)
                        (fn [v] (println "RESULT: " v))
                        (fn [e] (println "ERROR: " e))
                        #(println "COMPLETED")))

(comment (rx/subscribe (f/future f/default-runner
                          (Thread/sleep 5000)
                          (+ 100 200))
                        (fn [v] (println "RESULT: " v))
                        (fn [e] (println "ERROR: " e))
                        #(println "COMPLETED")))

(comment (rx/subscribe (f/future f/default-runner
                          (Thread/sleep 2000)
                          (throw (Exception. "Failed future")))
                        (fn [v] (println "RESULT: " v))
                        (fn [e] (println "ERROR: " e))
                        #(println "COMPLETED")))

