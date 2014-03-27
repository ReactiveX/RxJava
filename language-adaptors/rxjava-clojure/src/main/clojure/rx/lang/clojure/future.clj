(ns rx.lang.clojure.future
  "Functions and macros for making rx-ified futures. That is, run some code in some
   other thread and return an Observable of its result.
  "
  (:require [rx.lang.clojure.interop :as iop]
            [rx.lang.clojure.core :as rx]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn future*
  "Exerimental/Possibly a bad idea

  Execute (f & args) in a separate thread and pass the result to onNext.
  If an exception is thrown, onError is called with the exception.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel

  Examples:

    (subscribe (rx/future future-call
                  #(slurp \"input.txt\"))
               (fn [v] (println \"Got: \" v)))
    ; eventually outputs content of input.txt
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (rx/observable* (fn [^rx.Subscriber observer]
                    (let [wrapped (-> #(rx/on-next % (apply f args))
                                      rx/wrap-on-completed
                                      rx/wrap-on-error)
                          fu      (runner #(wrapped observer))]
                      (.add observer
                            (rx/subscription #(future-cancel fu)))))))

(defn future-generator*
  "Exerimental/Possibly a bad idea

  Same as rx/generator* except f is invoked in a separate thread.

  runner is a function that takes a no-arg function argument and returns a future
  representing the execution of that function.

  Returns an Observable. If the subscriber unsubscribes, the future will be canceled
  with clojure.core/future-cancel

  Example:

    (future-generator* future-call
      (fn [o]
        (rx/on-next o 1)
        (Thread/sleep 1000)
        (rx/on-next o 2)))

  See:
    rx.lang.clojure.core/generator*
  "
  [runner f & args]
  {:pre [(ifn? runner) (ifn? f)]}
  (rx/observable* (fn [^rx.Subscriber observer]
                    (let [wrapped (-> (fn [o]
                                        (apply f o args))
                                      rx/wrap-on-completed
                                      rx/wrap-on-error)
                          fu      (runner #(wrapped observer))]
                      (.add observer
                            (rx/subscription #(future-cancel fu)))))))
