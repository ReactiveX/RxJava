(ns rx.lang.clojure.base
  "Generic, low-level rx helpers."
  (:refer-clojure :exclude [merge])
  (:require [rx.lang.clojure.interop :as iop])
  (:import [rx Observable Observer Subscription]
           [rx.observables BlockingObservable]
           [rx.subscriptions Subscriptions]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn wrap-on-completed
  "Wrap handler with code that automaticaly calls rx.Observable.onCompleted."
  [handler]
  (fn [^Observer observer]
    (handler observer)
    (.onCompleted observer)))

(defn wrap-on-error
  "Wrap handler with code that automaticaly calls (on-error) if an exception is thrown"
  [handler]
  (fn [^Observer observer]
    (try
      (handler observer)
      (catch Exception e
        (.onError observer e)))))

(defn ^Observable merge
  "Observable.merge, renamed because merge means something else in Clojure

  os is one of:

    * An Iterable of Observables to merge
    * An Observable<Observable<T>> to merge
  "
  [os]
  (cond
    (instance? Iterable os)
      (Observable/merge (Observable/from ^Iterable os))
    (instance? Observable os)
      (Observable/merge ^Observable os)
    :else
      (throw (IllegalArgumentException. (str "Don't know how to merge " (type os))))))

(defn ^Observable merge-delay-error
  "Observable.mergeDelayError, renamed because merge means something else in Clojure"
  [os]
  (cond
    (instance? java.util.List os)
      (Observable/mergeDelayError ^java.util.List os)
    (instance? Observable os)
      (Observable/mergeDelayError ^Observable os)
    :else
      (throw (IllegalArgumentException. (str "Don't know how to merge " (type os))))))

(defn ^Observable zip
  "Observable.zip. You want map."
  ([f ^Observable a ^Observable b] (Observable/zip a b (iop/fn* f)))
  ([f ^Observable a ^Observable b ^Observable c] (Observable/zip a b c (iop/fn* f)))
  ([f ^Observable a ^Observable b ^Observable c ^Observable d] (Observable/zip a b c d (iop/fn* f)))
  ([f a b c d & more]
    ; recurse on more and then pull everything together with 4 parameter version
   (zip (fn [a b c more-value]
          (apply f a b c more-value))
        a
        b
        c
        (apply zip vector d more))))

(defmacro zip-let
  [bindings & body]
  (let [pairs  (clojure.core/partition 2 bindings)
        names  (clojure.core/mapv clojure.core/first pairs)
        values (clojure.core/map second pairs)]
    `(zip (fn ~names ~@body) ~@values)))
