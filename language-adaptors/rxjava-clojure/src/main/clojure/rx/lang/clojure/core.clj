(ns rx.lang.clojure.core
  (:refer-clojure :exclude [concat cons do drop drop-while empty
                            filter future
                            interpose into keep keep-indexed
                            map mapcat map-indexed
                            merge next partition reduce reductions
                            rest seq some sort sort-by split-with
                            take take-while throw])
  (:require [rx.lang.clojure.interop :as iop]
            [rx.lang.clojure.base :as base]
            [rx.lang.clojure.graph :as graph]
            [rx.lang.clojure.realized :as realized])
  (:import [rx Observable Observer Subscription]
           [rx.observables BlockingObservable]
           [rx.subscriptions Subscriptions]
           [rx.util.functions Action0 Action1 Func0 Func1 Func2]))

(set! *warn-on-reflection* true)

(declare map map-indexed reduce take take-while)

(defn ^Func1 fn->predicate
  "Turn f into a predicate that returns true/false like Rx predicates should"
  [f]
  (iop/fn* (comp boolean f)))

;################################################################################

(defn observable?
  "Returns true if o is an rx.Observable"
  [o]
  (instance? Observable o))

;################################################################################

(defn on-next
  "Call onNext on the given observer."
  [^Observer o value]
  (.onNext o value))

(defn on-completed
  "Call onCompleted on the given observer."
  [^Observer o]
  (.onCompleted o))

(defn on-error
  "Call onError on the given observer."
  [^Observer o e]
  (.onError o e))

(defn on-error-return
  [^Observable o f]
  (.onErrorReturn o f))

;################################################################################

(defn ^Subscription subscribe
  ([^Observable o on-next-action]
    (.subscribe o ^Action1 (iop/action* on-next-action)))
  ([^Observable o on-next-action on-error-action]
    (.subscribe o ^Action1 (iop/action* on-next-action) ^Action1 (iop/action* on-error-action)))
  ([^Observable o on-next-action on-error-action on-completed-action]
    (.subscribe o ^Action1 (iop/action* on-next-action) ^Action1 (iop/action* on-error-action) ^Action0 (iop/action* on-completed-action))))

(defn chain
  "Like subscribe, but any omitted handlers pass-through to the next observable."
  ([from to]
    (chain from to #(on-next to %)))
  ([from to on-next-action]
    (chain from to on-next-action #(on-error to %)))
  ([from to on-next-action on-error-action]
    (chain from to on-next-action on-error-action #(on-completed to)))
  ([from to on-next-action on-error-action on-completed-action]
    (subscribe from on-next-action on-error-action on-completed-action)))

(defn unsubscribe
  "Unsubscribe from Subscription s and return it."
  [^Subscription s]
  (.unsubscribe s)
  s)

(defn ^Subscription fn->subscription
  "Create a new subscription that calls the given no-arg handler function when
  unsubscribe is called

  See:
    rx.subscriptions.Subscriptions/create
  "
  [handler]
  (Subscriptions/create ^Action0 (iop/action* handler)))

;################################################################################

(defn ^Observable never [] (Observable/never))
(defn ^Observable empty [] (Observable/empty))

(defn ^Observable return
  "Returns an observable that emits a single value.

  See:
    Observable/just
  "
  [value]
  (Observable/just value))

(defn ^Observable fn->o
  "Create an observable from the given handler. When subscribed to, (f observer)
  is called at which point, f can start emitting values, etc."
  [f]
  (Observable/create (iop/fn* f)))

(defn ^Observable seq->o
  "Make an observable out of some seq-able thing. The rx equivalent of clojure.core/seq."
  [xs]
  (if xs
    (Observable/from ^Iterable xs)
    (empty)))

;################################################################################

(defn cache
  "caches the observable value so that multiple subscribers don't re-evaluate it"
  [^Observable xs]
  (.cache xs))

(defn cons
  "cons x to the beginning of xs"
  [x xs]
  (fn->o (fn [target]
           (on-next target x)
           (chain xs target))))

(defn ^Observable concat
  "Concatenate the given Observables one after the another.

  Note that xs is separate Observables which are concatentated. To concatenate an
  Observable of Observables, use concat*

  See:
    rx.Observable/concat
    concat*
  "
  [& xs]
  (Observable/concat (seq->o xs)))

(defn ^Observable concat*
  "Concatenate the given Observable of Observables one after another.

  See:
    rx.Observable/concat
    concat
  "
  [^Observable os]
  (Observable/concat os))

(defn ^Observable do
  "Returns a new Observable that, for each x in Observable xs, executes (do-fn x),
  presumably for its side effects, and then passes x along unchanged.

  If do-fn throws an exception, that exception is emitted via onError and the sequence
  is finished.

  Example:

    (->> (rx/seq->o [1 2 3])
         (rx/do println)
        ...)

    Will print 1, 2, 3.
  "
  ([do-fn xs]
   (fn->o (fn [target]
            (let [state (atom {:sub   nil
                               :error nil })
                  on-next-fn (fn [v]
                               ; since we may not be able to unsubscribe, drop
                               ; anything after an error
                               (let [{:keys [sub error]} @state]
                                 (if-not error
                                   (try
                                     (do-fn v)
                                     (on-next target v)
                                     (catch Throwable e
                                       (reset! state {:error e :sub nil})
                                       (if sub
                                         (unsubscribe sub))
                                       (on-error target e))))))]
              (let [sub (chain xs target on-next-fn)]
                ; dependening on xs, this may not be reached until after the sequence
                ; is complete.
                (swap! state update-in [:sub] (constantly sub))
                sub))))))

(defn ^Observable drop
  [n ^Observable xs]
  (.skip xs n))

(defn ^Observable drop-while
  [p xs]
  (fn->o (fn [target]
           (let [dropping (atom true)]
             (chain xs
                    target
                    (fn [v]
                      (when (or (not @dropping)
                                (not (reset! dropping (p v))))
                        (on-next target v))))))))

(defn ^Observable filter
  [p ^Observable xs]
  (.filter xs (fn->predicate p)))

(defn interpose
  [sep xs]
  (fn->o (fn [target]
           (let [first? (atom true)]
             (chain xs
                    target
                    (fn [v]
                      (if-not (compare-and-set! first? true false)
                        (on-next target sep))
                      (on-next target v)))))))

(defn into
  "Returns an observable that emits a single value which is all of the
  values of from-observable conjoined onto to

  See:
    clojure.core/into
    rx.Observable/toList
  "
  [to ^Observable from-observable]
  (->> from-observable
   .toList
   (map (partial clojure.core/into to))))

(defn keep
  [f xs]
  (filter (complement nil?) (map xs f)))

(defn keep
  [f xs]
  (filter (complement nil?) (map f xs)))

(defn keep-indexed
  [f xs]
  (filter (complement nil?) (map-indexed f xs)))

(defn ^Observable map
  "Map a function over an observable sequence. Unlike clojure.core/map, only supports up
  to 4 simultaneous source sequences at the moment."
  ([f ^Observable xs] (.map xs (iop/fn* f)))
  ([f xs & observables] (apply base/zip f xs observables)))

(defn ^Observable mapcat
  "Returns an observable which, for each value x in xs, calls (f x), which must
  return an Observable. The resulting observables are concatentated together
  into one observable.

  See:
    clojure.core/mapcat
    rx.Observable/mapMany
  "
  ([f ^Observable xs] (.mapMany xs (iop/fn* f)))
  ; TODO multi-arg version
  )

(defn map-indexed
  "Returns an observable that invokes (f index value) for each value of the input
  observable. index starts at 0.

  See:
    clojure.core/map-indexed
  "
  [f xs]
  (fn->o (fn [target]
           (let [n (atom -1)]
             (chain xs
                    target
                    (fn [v] (on-next target (f (swap! n inc) v))))))))

(defn merge
  "
  Returns an observable that emits a single map that consists of the rest of the
  maps emitted by the input observable conj-ed onto the first.  If a key occurs
  in more than one map, the mapping from the latter (left-to-right) will be the
  mapping in the result.

  NOTE: This is very different from rx.Observable/merge. See rx.base/merge for that
  one.

  See:
    clojure.core/merge
  "
  [maps]
  (reduce clojure.core/merge {} maps))

(def next
  "Returns an observable that emits all of the first element of the input observable.

  See:
    clojure.core/next
  "
  (partial drop 1))

; TODO partition. Use Buffer whenever it's implemented.

(defn ^Observable reduce
  ([f ^Observable xs] (.reduce xs (iop/fn* f)))
  ([f val ^Observable xs] (.reduce xs val (iop/fn* f))))

(defn ^Observable reductions
  ([f ^Observable xs] (.scan xs (iop/fn* f)))
  ([f val ^Observable xs] (.scan xs val (iop/fn* f))))

(def rest
  "Same as rx/next"
  next)

(defn some
  "Returns an observable that emits the first logical true value of (pred x) for
  any x in xs, else completes immediately.

  See:
    clojure.core/some
  "
  [p ^Observable xs]
  (fn->o (fn [target]
           (chain xs
                  target
                  (fn [v]
                    (when-let [result (p v)]
                      (on-next target result)
                      (on-completed target)))))))

(defn sort
  "Returns an observable that emits a single value which is a sorted sequence
  of the items in coll, where the sort order is determined by comparing
  items.  If no comparator is supplied, uses compare. comparator must
  implement java.util.Comparator.

  See:
    clojure.core/sort
  "
  ([coll] (sort clojure.core/compare coll))
  ([comp ^Observable coll]
   (.toSortedList coll (iop/fn [a b]
                         ; force to int so rxjava doesn't have a fit
                         (int (comp a b))))))

(defn sort-by
  "Returns an observable that emits a single value which is a sorted sequence
  of the items in coll, where the sort order is determined by comparing
  (keyfn item).  If no comparator is supplied, uses compare. comparator must
  implement java.util.Comparator.

  See:
    clojure.core/sort-by
  "
  ([keyfn coll] (sort-by keyfn clojure.core/compare coll))
  ([keyfn comp ^Observable coll]
   (.toSortedList coll (iop/fn [a b]
                         ; force to int so rxjava doesn't have a fit
                         (int (comp (keyfn a) (keyfn b)))))))

(defn split-with
  "Returns an observable that emits a pair of observables

    [(take-while p xs) (drop-while p xs)]

  See:
    rx.lang.clojure/take-while
    rx.lang.clojure/drop-while
    clojure.core/split-with
  "
  [p xs]
  (return [(take-while p xs) (drop-while p xs)]))

(defn ^Observable take
  "Returns an observable that emits the first n elements of xs.

  See:
    clojure.core/take
  "
  [n ^Observable xs]
  {:pre [(>= n 0)]}
  (.take xs n))

(defn take-while
  [p xs]
  (fn->o (fn [target]
           (chain xs
                  target
                  (fn [v]
                    (if (p v)
                      (on-next target v)
                      (on-completed target)))))))

;################################################################################;

(defn throw
  "Returns an Observable the simply emits the given exception with on-error

  See:
    http://netflix.github.io/RxJava/javadoc/rx/Observable.html#error(java.lang.Exception)
  "
  [^Exception e]
  (Observable/error e))

(defn catch*
  "Returns an observable that, when Observable o triggers an error, e, continues with
  Observable returned by (apply f e args) if (p e) is true. If (p e) returns a Throwable
  that value is passed as e.

  If p is a class object, a normal instance? check is performed rather than calling it
  as a function. If the value returned by (p e) is not true, the error is propagated.

  Examples:

    (-> my-observable

        ; On IllegalArgumentException, just emit 1
        (catch* IllegalArgumentException (fn [e] (rx/return 1)))

        ; If exception message contains \"WAT\", emit [\\W \\A \\T]
        (catch* #(-> % .getMessage (.contains \"WAT\")) (rx/seq->o [\\W \\A \\T])))

  See:

    http://netflix.github.io/RxJava/javadoc/rx/Observable.html#onErrorResumeNext(rx.util.functions.Func1)
  "
  [^Observable o p f & args]
  (let [p (if (class? p)
            (fn [e] (.isInstance ^Class p e))
            p)]
    (.onErrorResumeNext o
                        ^Func1 (iop/fn [e]
                                 (if-let [maybe-e (p e)]
                                   (apply f (if (instance? Throwable maybe-e) maybe-e e) args)
                                   (rx.lang.clojure.core/throw e))))))

(defmacro catch
  "Macro version of catch*.

  The body of the catch is wrapped in an implicit (do). It must evaluate to an Observable.

  Example:

    (-> my-observable
        ; just emit 0 on IllegalArgumentException
        (catch IllegalArgumentException e
          (rx/return 0))

        (catch DependencyException e
          (if (.isMinor e)
            (rx/return 0)
            (rx/throw (WebException. 503)))))

  See:
    catch*
  "
  [o p binding & body]
  `(catch* ~o ~p (fn [~binding] ~@body)))

(defn finally*
  "Returns an Observable that, as a side-effect, executes (apply f args) when the given
  Observable completes regardless of success or failure.

  Example:

    (-> my-observable
        (finally* (fn [] (println \"Done\"))))

  "
  [^Observable o f & args]
  (.finallyDo o ^Action0 (iop/action [] (apply f args))))

(defmacro finally
  "Macro version of finally*.

  Example:

    (-> my-observable
        (finally (println \"Done\")))

  See:
    finally*
  "
  [o & body]
  `(finally* ~o (fn [] ~@body)))

;################################################################################;

(defn generator*
  "Creates an observable that calls (f observable & args) which should emit a sequence.

  Automatically calls on-completed on return, or on-error if any exception is thrown.

  Subscribers will block.

  Examples:

    ; An observable that emits just 99
    (generator* on-next 99)
  "
  [f & args]
  (fn->o (-> (fn [observer]
               (apply f observer args)
               (Subscriptions/empty))
             base/wrap-on-completed
             base/wrap-on-error)))

(defmacro generator
  "Create an observable that executes body which should emit a sequence. bindings
  should be a single [observer] argument.

  Automatically calls on-completed on return, or on-error if any exception is thrown.

  Subscribe will block.

  Examples:

    ; make an observer that emits [0 1 2 3 4]
    (generator [observer]
      (dotimes [i 5]
        (on-next observer i)))

  "
  [bindings & body]
  `(generator* (fn ~bindings ~@body)))

;################################################################################;

; Import public graph symbols here. I want them in this namespace, but implementing
; them here with all the clojure.core symbols excluded is a pain.
(intern *ns* (with-meta 'let-o* (meta #'graph/let-o*)) @#'graph/let-o*)
(intern *ns* (with-meta 'let-o (meta #'graph/let-o)) @#'graph/let-o)

;################################################################################;

; Import some public realized symbols here. I want them in this namespace, but implementing
; them here with all the clojure.core symbols excluded is a pain.
(intern *ns* (with-meta 'let-realized (meta #'realized/let-realized)) @#'realized/let-realized)

