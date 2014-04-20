(ns rx.lang.clojure.core
  (:refer-clojure :exclude [concat cons count cycle
                            distinct do drop drop-while
                            empty every?
                            filter first future
                            group-by
                            interleave interpose into iterate
                            keep keep-indexed
                            map mapcat map-indexed
                            merge next nth partition-all
                            range reduce reductions
                            rest seq some sort sort-by split-with
                            take take-while throw])
  (:require [rx.lang.clojure.interop :as iop]
            [rx.lang.clojure.graph :as graph]
            [rx.lang.clojure.realized :as realized])
  (:import [rx
            Observable
            Observer Observable$Operator Observable$OnSubscribe
            Subscriber Subscription]
           [rx.observables
            BlockingObservable
            GroupedObservable]
           [rx.subscriptions Subscriptions]
           [rx.functions Action0 Action1 Func0 Func1 Func2]))

(set! *warn-on-reflection* true)

(declare concat* concat map* map map-indexed reduce take take-while)

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
  "Call onNext on the given observer and return o."
  [^Observer o value]
  (.onNext o value)
  o)

(defn on-completed
  "Call onCompleted on the given observer and return o."
  [^Observer o]
  (.onCompleted o)
  o)

(defn on-error
  "Call onError on the given observer and return o."
  [^Observer o e]
  (.onError o e)
  o)

(defmacro catch-error-value
  "Experimental

  TODO: Better name, better abstraction.

  Evaluate body and return its value.  If an exception e is thrown, inject the
  given value into the exception's cause and call (on-error error-observer e),
  returning e.

  This is meant to facilitate implementing Observers that call user-supplied code
  safely. The general pattern is something like:

    (fn [o v]
      (rx/catch-error-value o v
        (rx/on-next o (some-func v))))

  If (some-func v) throws an exception, it is caught, v is injected into the
  exception's cause (with OnErrorThrowable/addValueAsLastCause) and
  (rx/on-error o e) is invoked.

  See:
    rx.exceptions.OnErrorThrowable/addValueAsLastCause
  "
  [error-observer value & body]
  `(try
    ~@body
    (catch Throwable e#
      (on-error ~error-observer
                (rx.exceptions.OnErrorThrowable/addValueAsLastCause e# ~value))
      e#)))

;################################################################################
; Tools for creating new operators and observables

(declare unsubscribed?)

(defn ^Subscriber subscriber
  "Experimental, subject to change or deletion."
  ([o on-next-action] (subscriber o on-next-action nil nil))
  ([o on-next-action on-error-action] (subscriber o on-next-action on-error-action nil))
  ([^Subscriber o on-next-action on-error-action on-completed-action]
   (proxy [Subscriber] [o]
     (onCompleted []
       (if on-completed-action
         (on-completed-action o)
         (on-completed o)))
     (onError [e]
       (if on-error-action
         (on-error-action o e)
         (on-error o e)))
     (onNext [t]
       (if on-next-action
         (on-next-action o t)
         (on-next o t))))))

(defn ^Subscription subscription
  "Create a new subscription that calls the given no-arg handler function when
  unsubscribe is called

  See:
    rx.subscriptions.Subscriptions/create
  "
  [handler]
  (Subscriptions/create ^Action0 (iop/action* handler)))

(defn ^Observable$Operator operator*
  "Experimental, subject to change or deletion.

  Returns a new implementation of rx.Observable$Operator that calls the given
  function with a rx.Subscriber. The function should return a rx.Subscriber.

  See:
    lift
    rx.Observable$Operator
  "
  [f]
  {:pre [(fn? f)]}
  (reify Observable$Operator
    (call [this o]
      (f o))))

(defn ^Observable observable*
  "Create an Observable from the given function.

  When subscribed to, (f subscriber) is called at which point, f can start emitting values, etc.
  The passed subscriber is of type rx.Subscriber.

  See:
    rx.Subscriber
    rx.Observable/create
  "
  [f]
  (Observable/create ^Observable$OnSubscribe (iop/action* f)))

(defn wrap-on-completed
  "Wrap handler with code that automaticaly calls rx.Observable.onCompleted."
  [handler]
  (fn [^Observer observer]
    (handler observer)
    (when-not (unsubscribed? observer)
      (.onCompleted observer))))

(defn wrap-on-error
  "Wrap handler with code that automaticaly calls (on-error) if an exception is thrown"
  [handler]
  (fn [^Observer observer]
    (try
      (handler observer)
      (catch Throwable e
        (when-not (unsubscribed? observer)
          (.onError observer e))))))

(defn lift
  "Lift the Operator op over the given Observable xs

  Example:

    (->> my-observable
         (rx/lift (rx/operator ...))
         ...)

  See:
    rx.Observable/lift
    operator
  "
  [^Observable$Operator op ^Observable xs]
  (.lift xs op))

;################################################################################

(defn ^Subscription subscribe
  "Subscribe to the given observable.

  on-X-action is a normal clojure function.

  See:
    rx.Observable/subscribe
  "

  ([^Observable o on-next-action]
    (.subscribe o
                ^Action1 (iop/action* on-next-action)))

  ([^Observable o on-next-action on-error-action]
    (.subscribe o
                ^Action1 (iop/action* on-next-action)
                ^Action1 (iop/action* on-error-action)))

  ([^Observable o on-next-action on-error-action on-completed-action]
    (.subscribe o
                ^Action1 (iop/action* on-next-action)
                ^Action1 (iop/action* on-error-action)
                ^Action0 (iop/action* on-completed-action))))

(defn unsubscribe
  "Unsubscribe from Subscription s and return it."
  [^Subscription s]
  (.unsubscribe s)
  s)

(defn subscribe-on
  "Cause subscriptions to the given observable to happen on the given scheduler.

  Returns a new Observable.

  See:
    rx.Observable/subscribeOn
  "
  [^rx.Scheduler s ^Observable xs]
  (.subscribeOn xs s))

(defn unsubscribe-on
  "Cause unsubscriptions from the given observable to happen on the given scheduler.

  Returns a new Observable.

  See:
    rx.Observable/unsubscribeOn
  "
  [^rx.Scheduler s ^Observable xs]
  (.unsubscribeOn xs s))

(defn unsubscribed?
  "Returns true if the given Subscription (or Subscriber) is unsubscribed.

  See:
    rx.Observable/create
    observable*
  "
  [^Subscription s]
  (.isUnsubscribed s))

;################################################################################
; Functions for creating Observables

(defn ^Observable never
  "Returns an Observable that never emits any values and never completes.

  See:
    rx.Observable/never
  "
  []
  (Observable/never))

(defn ^Observable empty
  "Returns an Observable that completes immediately without emitting any values.

  See:
    rx.Observable/empty
  "
  []
  (Observable/empty))

(defn ^Observable return
  "Returns an observable that emits a single value.

  See:
    rx.Observable/just
  "
  [value]
  (Observable/just value))

(defn ^Observable seq->o
  "Make an observable out of some seq-able thing. The rx equivalent of clojure.core/seq."
  [xs]
  (if-let [s (clojure.core/seq xs)]
    (Observable/from ^Iterable s)
    (empty)))

;################################################################################
; Operators

(defn serialize
  "Serialize execution.

  See:
    rx.Observable/serialize
  "
  ([^Observable xs]
  (.serialize xs)))

(defn merge*
  "Merge an Observable of Observables into a single Observable

  If you want clojure.core/merge, it's just this:

    (rx/reduce clojure.core/merge {} maps)

  See:
    merge
    merge-delay-error*
    rx.Observable/merge
  "
  [^Observable xs]
  (Observable/merge xs))

(defn ^Observable merge
  "Merge one or more Observables into a single observable.

  If you want clojure.core/merge, it's just this:

    (rx/reduce clojure.core/merge {} maps)

  See:
    merge*
    merge-delay-error
    rx.Observable/merge
  "
  [& os]
  (merge* (seq->o os)))

(defn ^Observable merge-delay-error*
  "Same as merge*, but all values are emitted before errors are propagated"
  [^Observable xs]
  (Observable/mergeDelayError xs))

(defn ^Observable merge-delay-error
  "Same as merge, but all values are emitted before errors are propagated"
  [& os]
  (merge-delay-error* (seq->o os)))

(defn cache
  "caches the observable value so that multiple subscribers don't re-evaluate it.

  See:
    rx.Observable/cache"
  [^Observable xs]
  (.cache xs))

(defn cons
  "cons x to the beginning of xs"
  [x xs]
  (concat (return x) xs))

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

(defn count
  "Returns an Observable that emits the number of items is xs as a long.

  See:
    rx.Observable/longCount
  "
  [^Observable xs]
  (.longCount xs))

(defn cycle
  "Returns an Observable that emits the items of xs repeatedly, forever.

  TODO: Other sigs.

  See:
    rx.Observable/repeat
    clojure.core/cycle
  "
  [^Observable xs]
  (.repeat xs))

(defn distinct
  "Returns an Observable of the elements of Observable xs with duplicates
  removed. key-fn, if provided, is a one arg function that determines the
  key used to determined duplicates. key-fn defaults to identity.

  This implementation doesn't use rx.Observable/distinct because it doesn't
  honor Clojure's equality semantics.

  See:
    clojure.core/distinct
  "
  ([xs] (distinct identity xs))
  ([key-fn ^Observable xs]
   (let [op (operator* (fn [o]
                         (let [seen (atom #{})]
                           (subscriber o
                                       (fn [o v]
                                         (let [key (key-fn v)]
                                           (when-not (contains? @seen key)
                                             (swap! seen conj key)
                                             (on-next o v))))))))]
    (lift op xs))))

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

  See:
    rx.Observable/doOnNext
  "
  [do-fn ^Observable xs]
  (.doOnNext xs (iop/action* do-fn)))

(defn ^Observable drop
  [n ^Observable xs]
  (.skip xs n))

(defn ^Observable drop-while
  [p ^Observable xs]
  (.skipWhile xs (fn->predicate p)))

(defn ^Observable every?
  "Returns an Observable that emits a single true value if (p x) is true for
  all x in xs. Otherwise emits false.

  See:
    clojure.core/every?
    rx.Observable/all
  "
  [p ^Observable xs]
  (.all xs (fn->predicate p)))

(defn ^Observable filter
  [p ^Observable xs]
  (.filter xs (fn->predicate p)))

(defn ^Observable first
  "Returns an Observable that emits the first item emitted by xs, or an
  empty Observable if xs is empty.

  See:
    rx.Observable/take(1)
  "
  [^Observable xs]
  (.take xs 1))

(defn ^Observable group-by
  "Returns an Observable of clojure.lang.MapEntry where the key is the result of
  (key-fn x) and the val is an Observable of x for each key.

  This returns a clojure.lang.MapEntry rather than rx.observables.GroupedObservable
  for some vague consistency with clojure.core/group-by and so that clojure.core/key,
  clojure.core/val and destructuring will work as expected.

  See:
    clojure.core/group-by
    rx.Observable/groupBy
    rx.observables.GroupedObservable
  "
  ([key-fn ^Observable xs]
   (->> (.groupBy xs (iop/fn* key-fn))
        (map (fn [^GroupedObservable go]
               (clojure.lang.MapEntry. (.getKey go) go))))))

(defn interleave*
  "Returns an Observable of the first item in each Observable emitted by observables, then
  the second etc.

  observables is an Observable of Observables

  See:
    interleave
    clojure.core/interleave
  "
  [observables]
  (->> (map* #(seq->o %&) observables)
       (concat*)))

(defn interleave
  "Returns an Observable of the first item in each Observable, then the second etc.

  Each argument is an individual Observable

  See:
    observable*
    clojure.core/interleave
  "
  [o1 & observables]
  (->> (apply map #(seq->o %&) o1 observables)
       (concat*)))

(defn interpose
  "Returns an Observable of the elements of xs separated by sep

  See:
    clojure.core/interpose
  "
  [sep xs]
  (let [op (operator* (fn [o]
                        (let [first? (atom true)]
                          (subscriber o (fn [o v]
                                          (if-not (compare-and-set! first? true false)
                                            (on-next o sep))
                                          (on-next o v))))))]
    (lift op xs)))

(defn into
  "Returns an observable that emits a single value which is all of the
  values of from-observable conjoined onto to

  See:
    clojure.core/into
    rx.Observable/toList
  "
  [to ^Observable from]
  ; clojure.core/into uses transients if to is IEditableCollection
  ; I don't think we have any guarantee that all on-next calls will be on the
  ; same thread, so we can't do that here.
  (reduce conj to from))

(defn iterate
  "Returns an Observable of x, (f x), (f (f x)) etc. f must be free of side-effects

  See:
    clojure.core/iterate
  "
  [f x]
  (observable* (fn [s]
                 (loop [x x]
                   (when-not (unsubscribed? s)
                     (on-next s x)
                     (recur (f x)))))))

(defn keep
  [f xs]
  (filter (complement nil?) (map f xs)))

(defn keep-indexed
  [f xs]
  (filter (complement nil?) (map-indexed f xs)))

(defn ^Observable map*
  "Map a function over an Observable of Observables.

  Each item from the first emitted Observable is the first arg, each
  item from the second emitted Observable is the second arg, and so on.

  See:
    map
    clojure.core/map
    rx.Observable/zip
  "
  [f ^Observable observable]
  (Observable/zip observable
                  ^rx.functions.FuncN (iop/fnN* f)))

(defn ^Observable map
  "Map a function over one or more observable sequences.

  Each item from the first Observable is the first arg, each item
  from the second Observable is the second arg, and so on.

  See:
    clojure.core/map
    rx.Observable/zip
  "
  [f & observables]
  (Observable/zip ^Iterable observables
                  ^rx.functions.FuncN (iop/fnN* f)))

(defn ^Observable mapcat*
  "Same as multi-arg mapcat, but input is an Observable of Observables.

  See:
    mapcat
    clojure.core/mapcat
  "
  [f ^Observable xs]
  (->> xs
       (map* f)
       (concat*)))

(defn ^Observable mapcat
  "Returns an observable which, for each value x in xs, calls (f x), which must
  return an Observable. The resulting observables are concatentated together
  into one observable.

  If multiple Observables are given, the arguments to f are the first item from
  each observable, then the second item, etc.

  See:
    clojure.core/mapcat
    rx.Observable/flatMap
  "
  [f & xs]
  (if (clojure.core/next xs)
    (mapcat* f (seq->o xs))
    ; use built-in flatMap for single-arg case
    (.flatMap ^Observable (clojure.core/first xs) (iop/fn* f))))

(defn map-indexed
  "Returns an observable that invokes (f index value) for each value of the input
  observable. index starts at 0.

  See:
    clojure.core/map-indexed
  "
  [f xs]
  (let [op (operator* (fn [o]
                        (let [n (atom -1)]
                          (subscriber o
                                      (fn [o v]
                                        (catch-error-value o v
                                          (on-next o (f (swap! n inc) v))))))))]
    (lift op xs)))

(def next
  "Returns an observable that emits all but the first element of the input observable.

  See:
    clojure.core/next
  "
  (partial drop 1))

(defn nth
  "Returns an Observable that emits the value at the index in the given
  Observable.  nth throws an IndexOutOfBoundsException unless not-found
  is supplied.

  Note that the Observable is the *first* arg!
  "
  ([^Observable xs index]
   (.elementAt xs index))
  ([^Observable xs index not-found]
   (.elementAtOrDefault xs index not-found)))

(defn ^Observable partition-all
  "Returns an Observable of Observables of n items each, at offsets step
  apart. If step is not supplied, defaults to n, i.e. the partitions
  do not overlap. May include partitions with fewer than n items at the end.

  See:
    clojure.core/partition-all
    rx.Observable/window
  "
  ([n ^Observable xs] (.window xs (int n)))
  ([n step ^Observable xs] (.window xs (int n) (int step))))

(defn range
  "Returns an Observable nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0, step to 1, and end
  to infinity.

  Note: this is not implemented on rx.Observable/range

  See:
    clojure.core/range
  "
  ([] (range 0 Double/POSITIVE_INFINITY 1))
  ([end] (range 0 end 1))
  ([start end] (range start end 1))
  ([start end step]
   (observable* (fn [s]
                  (let [comp (if (pos? step) < >)]
                    (loop [i start]
                      (if-not (unsubscribed? s)
                        (if (comp i end)
                          (do
                            (on-next s i)
                            (recur (+ i step)))
                          (on-completed s)))))))))

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
  (->> xs
       (map p)
       (filter identity)
       first))

(defn ^:private sorted-list-by
  ([keyfn coll] (sorted-list-by keyfn clojure.core/compare coll))
  ([keyfn comp ^Observable coll]
   (.toSortedList coll (iop/fn [a b]
                         ; force to int so rxjava doesn't have a fit
                         (int (comp (keyfn a) (keyfn b)))))))

(defn sort
  "Returns an observable that emits the items in xs, where the sort order is
  determined by comparing items. If no comparator is supplied, uses compare.
  comparator must implement java.util.Comparator.

  See:
    clojure.core/sort
  "
  ([xs]
   (sort clojure.core/compare xs))
  ([comp xs]
   (->> xs
        (sorted-list-by identity comp)
        (mapcat seq->o))))

(defn sort-by
  "Returns an observable that emits the items in xs, where the sort order is
  determined by comparing (keyfn item). If no comparator is supplied, uses
  compare. comparator must implement java.util.Comparator.

  See:
    clojure.core/sort-by
  "
  ([keyfn xs]
   (sort-by keyfn clojure.core/compare xs))
  ([keyfn comp ^Observable xs]
   (->> xs
        (sorted-list-by keyfn comp)
        (mapcat seq->o))))

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
  "Returns an Observable that emits xs until the first x such that
  (p x) is falsey.

  See:
    clojure.core/take-while
    rx.Observable/takeWhile
  "
  [p ^Observable xs]
  (.takeWhile xs (fn->predicate p)))

;################################################################################;

(defn throw
  "Returns an Observable the simply emits the given exception with on-error

  See:
    rx.Observable/error
  "
  [^Throwable e]
  (Observable/error e))

(defn catch*
  "Returns an observable that, when Observable o triggers an error, e, continues with
  Observable returned by (f e) if (p e) is true. If (p e) returns a Throwable
  that value is passed as e.

  If p is a class object, a normal instance? check is performed rather than calling it
  as a function. If the value returned by (p e) is not true, the error is propagated.

  Examples:

    (->> my-observable

         ; On IllegalArgumentException, just emit 1
         (catch* IllegalArgumentException
                 (fn [e] (rx/return 1)))

         ; If exception message contains \"WAT\", emit [\\W \\A \\T]
         (catch* (fn [e] (-> e .getMessage (.contains \"WAT\")))
                 (fn [e] (rx/seq->o [\\W \\A \\T]))))

  See:
    rx.Observable/onErrorResumeNext
    http://netflix.github.io/RxJava/javadoc/rx/Observable.html#onErrorResumeNext(rx.functions.Func1)
  "
  [p f ^Observable o]
  (let [p (if (class? p)
            (fn [e] (.isInstance ^Class p e))
            p)]
    (.onErrorResumeNext o
                        ^Func1 (iop/fn [e]
                                 (if-let [maybe-e (p e)]
                                   (f (if (instance? Throwable maybe-e)
                                        maybe-e
                                        e))
                                   (rx.lang.clojure.core/throw e))))))

(defmacro catch
  "Macro version of catch*.

  The body of the catch is wrapped in an implicit (do). It must evaluate to an Observable.

  Note that the source observable is the last argument so this works with ->> but may look
  slightly odd when used standalone.

  Example:

    (->> my-observable
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
  {:arglists '([p binding & body observable])}
  [p binding & body]
  (let [o    (last body)
        body (butlast body)]
    `(catch* ~p
             (fn [~binding] ~@body)
             ~o)))

(defn finally*
  "Returns an Observable that, as a side-effect, executes (f) when the given
  Observable completes regardless of success or failure.

  Example:

    (->> my-observable
         (finally* (fn [] (println \"Done\"))))

  "
  [f ^Observable o]
  (.finallyDo o ^Action0 (iop/action* f)))

(defmacro finally
  "Macro version of finally*.

  Note that the source observable is the last argument so this works with ->> but may look
  slightly odd when used standalone.

  Example:

    (->> my-observable
         (finally (println \"Done\")))

  See:
    finally*
  "
  {:arglists '([& body observable])}
  [& body]
  (let [o    (last body)
        body (butlast body)]
    `(finally* (fn [] ~@body) ~o)))

;################################################################################;

(defn generator*
  "Creates an observable that calls (f observer & args) which should emit values
  with (rx/on-next observer value).

  Automatically calls on-completed on return, or on-error if any exception is thrown.

  f should exit early if (rx/unsubscribed? observable) returns true

  Examples:

    ; An observable that emits just 99
    (rx/generator* on-next 99)
  "
  [f & args]
  (observable* (-> #(apply f % args)
                   wrap-on-completed
                   wrap-on-error)))

(defmacro generator
  "Create an observable that executes body which should emit values with
  (rx/on-next observer value) where observer comes from bindings.

  Automatically calls on-completed on return, or on-error if any exception is thrown.

  The body should exit early if (rx/unsubscribed? observable) returns true

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

