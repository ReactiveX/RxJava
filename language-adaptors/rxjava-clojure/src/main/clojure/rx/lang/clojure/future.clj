(ns rx.lang.clojure.future
  (:refer-clojure :exclude [future])
  (:require [rx.lang.clojure.interop :as iop]
            [rx.lang.clojure.core :as rx :refer [fn->o fn->subscription]]
            [rx.lang.clojure.base :as base]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn default-runner
  "Default runner creator function. Creates futures on Clojure's default future thread pool."
  [f]
  (future-call f))

(defn future-generator*
  "Same as rx/generator* except f is invoked in a separate thread.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  subscribe will not block.

  See:
    rx.lang.clojure.core/generator*
    rx.lang.clojure.future/future-generator
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (fn->o (fn [observer]
           (let [wrapped (-> (fn [o]
                               (apply f o args))
                             base/wrap-on-completed
                             base/wrap-on-error)
                 fu      (runner #(wrapped observer))]
             (fn->subscription #(future-cancel fu))))))

(defmacro future-generator
  "Same as rx/generator macro except body is invoked in a separate thread.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  subscribe will not block.

  See:
    rx.lang.clojure.core/generator*
    rx.lang.clojure.future/future-generator
  "
  [runner bindings & body]
  `(future-generator* ~runner (fn ~bindings ~@body)))

(defn future*
  "Execute (f & args) in a separate thread and pass the result to onNext.
  If an exception is thrown, onError is called with the exception.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  Returns an Observable.
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (fn->o (fn [observer]
           (let [wrapped (-> #(rx/on-next % (apply f args))
                             base/wrap-on-completed
                             base/wrap-on-error)
                 fu      (runner #(wrapped observer))]
             (fn->subscription #(future-cancel fu))))))

(defmacro future
  "Executes body in a separate thread and passes the single result to onNext.
  If an exception occurs, onError is called.

  Returns an Observable

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  Examples:

    (subscribe (rx/future rx/default-runner
                  (slurp \"input.txt\"))
               (fn [v] (println \"Got: \" v)))
    ; eventually outputs content of input.txt
  "
  [runner & body]
  `(future* ~runner (fn [] ~@body)))

