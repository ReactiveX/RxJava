(ns rx.lang.clojure.interop
  "Functions an macros for instantiating rx Func* and Action* interfaces."
  (:refer-clojure :exclude [fn]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defmacro ^:private reify-callable
  "Reify a bunch of Callable-like interfaces

    prefix  fully qualified interface name. numbers will be appended
    arities vector of desired arities
    f       the function to execute

  "
  [prefix arities f]
  (let [f-name (gensym "rc")]
    `(let [~f-name ~f]
       (reify
         ; If they want Func1, give them subscribe as well so Observable/create can be
         ; used seemlessly with rx/fn.
         ~@(if (and (= prefix "rx.util.functions.Func")
                    (some #{1} arities))
             `(rx.IObservable
                (~'subscribe [~'this observer#]
                  (~f-name observer#))))

         ~@(mapcat (clojure.core/fn [n]
                     (let [ifc-sym  (symbol (str prefix n))
                           arg-syms (map #(symbol (str "v" %)) (range n))]
                       `(~ifc-sym
                          (~'call ~(vec (cons 'this arg-syms))
                                  ~(cons f-name arg-syms)))))
             arities) ))))

(defn fn*
  "Given function f, returns an object that implements rx.util.functions.Func0-9
  by delegating the call() method to the given function.

  If the f has the wrong arity, an ArityException will be thrown at runtime.

  This will also implement rx.IObservable.subscribe for use with
  Observable/create. In this case, the function must take an Observable as its single
  argument and return a subscription object.

  Example:

    (.reduce my-numbers (rx/fn* +))

  See:
    http://netflix.github.io/RxJava/javadoc/rx/util/functions/Func0.html
  "
  [f]
  (reify-callable "rx.util.functions.Func" [0 1 2 3 4 5 6 7 8 9] f))

(defn fnN*
  "Given function f, returns an object that implements rx.util.functions.FuncN
  by delegating to the given function.

  Unfortunately, this can't be included in fn* because of ambiguities between
  the single arg call() method and the var args call method.

  See:
    http://netflix.github.io/RxJava/javadoc/rx/util/functions/FuncN.html
  "
  [f]
  (reify rx.util.functions.FuncN
    (call [this objects]
      (apply f objects))))

(defmacro fn
  "Like clojure.core/fn, but returns the appropriate rx.util.functions.Func*
  interface.

  Example:

    (.map my-observable (rx/fn [a] (* 2 a)))

    or, to create an Observable:

    (Observable/create (rx/fn [observer]
                         (.onNext observer 10)
                         (.onCompleted observer)
                         (Subscriptions/empty)))

  See:
    rx.lang.clojure.interop/fn*
  "
  [& fn-form]
  ; preserve metadata so type hints work
  ; have to qualify fn*. Otherwise bad things happen with the fn* special form in clojure
  (with-meta `(rx.lang.clojure.interop/fn* (clojure.core/fn ~@fn-form))
             (meta &form)))

(defn action*
  "Given function f, returns an object that implements rx.util.functions.Action0-3
  by delegating to the given function.

  Example:

    (.subscribe my-observable (rx/action* println))

  See:
    http://netflix.github.io/RxJava/javadoc/rx/util/functions/Action0.html
  "
  [f]
  (reify-callable "rx.util.functions.Action" [0 1 2 3] f))

(defmacro action
  "Like clojure.core/fn, but returns the appropriate rx.util.functions.Action*
  interface.

  Example:

    (.finallyDo my-observable (rx/action [] (println \"Finally!\")))

  "
  [& fn-form]
  ; preserve metadata so type hints work
  (with-meta `(action* (clojure.core/fn ~@fn-form))
             (meta &form)))

;################################################################################
