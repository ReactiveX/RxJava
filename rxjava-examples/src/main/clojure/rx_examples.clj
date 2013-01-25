(ns rx-examples
  (:use clojure.test)
  (import rx.observables.Observable))

(defn hello
  [&rest]
  (-> (Observable/toObservable &rest)
    (.subscribe #(println (str "Hello " % "!")))))
