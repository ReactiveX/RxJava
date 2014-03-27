(ns rx.lang.clojure.blocking
  "Blocking operators and functions. These should never be used in
  production code except at the end of an async chain to convert from
  rx land back to sync land. For example, to produce a servlet response.

  If you use these, you're a bad person.
  "
  (:refer-clojure :exclude [first into doseq last])
  (:require [rx.lang.clojure.interop :as iop] [rx.lang.clojure.core :as rx])
  (:import [rx Observable]
           [rx.observables BlockingObservable]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defmacro ^:private with-ex-unwrap
  "The blocking ops wrap errors stuff in RuntimeException because of stupid Java.
  This tries to unwrap them so callers get the exceptions they expect."
  [& body]
  `(try
    ~@body
    (catch RuntimeException e#
      (throw (or
               (and (identical? RuntimeException (class e#))
                    (.getCause e#))
               e#)))))

(defn ^BlockingObservable ->blocking
  "Convert an Observable to a BlockingObservable.

  If o is already a BlockingObservable it's returned unchanged.
  "
  [o]
  (if (instance? BlockingObservable o)
    o
    (.toBlockingObservable ^Observable o)))

(defn o->seq
  "Returns a lazy sequence of the items emitted by o

  See:
    rx.observables.BlockingObservable/getIterator
    rx.lang.clojure.core/seq->o
  "
  [o]
  (-> (->blocking o)
      (.getIterator)
      (iterator-seq)))

(defn first
  "*Blocks* and waits for the first value emitted by the given observable.

  If the Observable is empty, returns nil

  If an error is produced it is thrown.

  See:
    clojure.core/first
    rx/first
    rx.observables.BlockingObservable/first
  "
  [observable]
  (with-ex-unwrap
    (.firstOrDefault (->blocking observable) nil)))

(defn last
  "*Blocks* and waits for the last value emitted by the given observable.

  If the Observable is empty, returns nil

  If an error is produced it is thrown.

  See:
    clojure.core/last
    rx/last
    rx.observable.BlockingObservable/last
  "
  [observable]
  (with-ex-unwrap
    (.lastOrDefault (->blocking observable) nil)))

(defn single
  "*Blocks* and waits for the first value emitted by the given observable.

   An error is thrown if zero or more then one value is produced.
  "
  [observable]
  (with-ex-unwrap
    (.single (->blocking observable))))

(defn into
  "*Blocks* and pours the elements emitted by the given observables into
  to.

  If an error is produced it is thrown.

  See:
    clojure.core/into
    rx/into
  "
  [to from-observable]
  (with-ex-unwrap
    (clojure.core/into to (o->seq from-observable))))

(defn doseq*
  "*Blocks* and executes (f x) for each x emitted by xs

  Returns nil.

  See:
    doseq
    clojure.core/doseq
  "
  [xs f]
  (with-ex-unwrap
    (-> (->blocking xs)
        (.forEach (rx.lang.clojure.interop/action* f)))))

(defmacro doseq
  "Like clojure.core/doseq except iterates over an observable in a blocking manner.

  Unlike clojure.core/doseq, only supports a single binding

  Returns nil.

  Example:

    (rx-blocking/doseq [{:keys [name]} users-observable]
      (println \"User:\" name))

  See:
    doseq*
    clojure.core/doseq
  "
  [bindings & body]
  (when (not= (count bindings) 2)
    (throw (IllegalArgumentException. (str "sorry, rx/doseq only supports one binding"))))
  (let [[k v] bindings]
    `(doseq* ~v (fn [~k] ~@body))))

