(ns rx.lang.clojure.chunk
  (:refer-clojure :exclude [chunk])
  (:require [rx.lang.clojure.core :as rx]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn chunk
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  TODO RxJava's much bigger since this was written. Is there something built in?

  Same as rx.Observable.merge(Observable<Observable<T>>) but the input Observables
  are \"chunked\" so that at most chunk-size of them are \"in flight\" at any given
  time.

  The order of the input Observables is not preserved.

  The main purpose here is to allow a large number of Hystrix observables to
  be processed in a controlled way so that the Hystrix execution queues aren't
  overwhelmed.

  Example:

    (->> users
         (rx/map #(-> (GetUserCommand. %) .toObservable))
         (chunk 10))

  See:
    http://netflix.github.io/RxJava/javadoc/rx/Observable.html#merge(rx.Observable)
    http://netflix.github.io/RxJava/javadoc/rx/Observable.html#mergeDelayError(rx.Observable)
  "
  ([chunk-size observable-source] (chunk chunk-size {} observable-source))
  ([chunk-size options observable-source]
  (let [new-state-atom #(atom {:in-flight #{}     ; observables currently in-flight
                               :buffered  []      ; observables waiting to be emitted
                               :complete  false   ; true if observable-source is complete
                               :observer  % })    ; the observer
        ps   #(do (printf "%s/%d/%d%n"
                          (:complete %)
                          (-> % :buffered count)
                          (-> % :in-flight count))
                (flush))

        ; Given the current state, returns [action new-state]. action is the
        ; next Observable or Throwable to emit, or :complete if we're done.
        next-state (fn [{:keys [complete buffered in-flight] :as old}]
                     (cond
                       (empty? buffered)                [complete old]

                       (< (count in-flight) chunk-size) (let [next-o (first buffered)]
                                                          [next-o
                                                           (-> old
                                                               (update-in [:buffered] next)
                                                               (update-in [:in-flight] conj next-o))])

                       :else                            [nil old]))

        ; Advance the state, performing side-effects as necessary
        advance! (fn advance! [state-atom]
                   (let [old-state        @state-atom
                         [action new-state] (next-state old-state)]
                     (if (compare-and-set! state-atom old-state new-state)
                       (let [observer (:observer new-state)]
                         (if (:debug options) (ps new-state))
                         (cond
                          (= :complete action)
                            (rx/on-completed observer)

                          (instance? Throwable action)
                            (rx/on-error observer action)

                          (instance? rx.Observable action)
                            (rx/on-next observer
                                        (.finallyDo ^rx.Observable action
                                                    (reify rx.functions.Action0
                                                              (call [this]
                                                                (swap! state-atom update-in [:in-flight] disj action)
                                                                (advance! state-atom)))))))
                       (recur state-atom))))

        subscribe (fn [state-atom]
                    (rx/subscribe observable-source
                                  (fn [o]
                                    (swap! state-atom update-in [:buffered] conj o)
                                    (advance! state-atom))

                                  (fn [e]
                                    (swap! state-atom assoc :complete e)
                                    (advance! state-atom))

                                  (fn []
                                    (swap! state-atom assoc :complete :complete)
                                    (advance! state-atom))))
        observable (rx/observable* (fn [observer]
                                     (subscribe (new-state-atom observer)))) ]
    (if (:delay-error? options)
      (rx.Observable/mergeDelayError observable)
      (rx.Observable/merge observable)))))

