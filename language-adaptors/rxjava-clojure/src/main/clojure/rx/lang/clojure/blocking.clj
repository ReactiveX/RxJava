(ns rx.lang.clojure.blocking
  "Blocking operators and functions. These should never be used in
  production code except at the end of an async chain to convert from
  rx land back to sync land. For example, to produce a servlet response."
  (:refer-clojure :exclude [first into])
  (:require [rx.lang.clojure.core :as rx])
  (:import [rx Observable]
           [rx.observables BlockingObservable]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn ^BlockingObservable ->blocking
  "Convert an Observable to a BlockingObservable"
  [^Observable o]
  (.toBlockingObservable o))

(defn first
  "*Blocks* and waits for the first value emitted by the given observable.

  If an error is produced it is thrown.

  See:
    clojure.core/first
    rx/first
  "
  [observable]
  (let [result (clojure.core/promise)]
    (rx/subscribe (->> observable (rx/take 1))
                #(clojure.core/deliver result [:value %])
                #(clojure.core/deliver result [:error %])
                #(clojure.core/deliver result nil))
    (if-let [[type v] @result]
      (case type
        :value v
        :error (throw v)))))

(defn single
  "*Blocks* and waits for the first value emitted by the given observable.

   An error is thrown if more then one value is produced.
  "
  [observable]
  (.single (->blocking observable)))

(defn into
  "*Blocks* and pours the elements emitted by the given observables into
  to.

  If an error is produced it is thrown.

  See:
    clojure.core/into
    rx/into
  "
  [to from-observable]
  (first (rx/into to from-observable)))
