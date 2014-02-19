(ns rx.lang.clojure.future
  "Functions and macros for making rx-ified futures. That is, run some code in some
   other thread and return an Observable of its result.
  "
  (:refer-clojure :exclude [future])
  (:require [rx.lang.clojure.interop :as iop]
            [rx.lang.clojure.core :as rx]))

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

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel

  See:
    rx.lang.clojure.core/generator*
    rx.lang.clojure.future/future-generator
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (rx/fn->o (fn [^rx.Subscriber observer]
              (let [wrapped (-> (fn [o]
                                  (apply f o args))
                                rx/wrap-on-completed
                                rx/wrap-on-error)
                    fu      (runner #(wrapped observer))]
              (.add observer
                    (rx/fn->subscription #(future-cancel fu)))))))

(defmacro future-generator
  "Same as rx/generator macro except body is invoked in a separate thread.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel

  Example:

    (future-generator default-runner
      [o]
      (rx/on-next o 1)
      (Thread/sleep 1000)
      (rx/on-next o 2))

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

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (rx/fn->o (fn [^rx.Subscriber observer]
           (let [wrapped (-> #(rx/on-next % (apply f args))
                             rx/wrap-on-completed
                             rx/wrap-on-error)
                 fu      (runner #(wrapped observer))]
             (.add observer
                   (rx/fn->subscription #(future-cancel fu)))))))

(defmacro future
  "Executes body in a separate thread and passes the single result to onNext.
  If an exception occurs, onError is called.

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel

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

