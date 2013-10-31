(ns rx.lang.clojure.interop
  "Functions an macros for instantiating rx Func* and Action* interfaces."
  (:refer-clojure :exclude [fn])
  (:require [clojure.reflect :as r])
  )

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
         ; If they want Func1, give them onSubscribe as well so Observable/create can be
         ; used seemlessly with rx/fn.
         ~@(if (and (= prefix "rx.util.functions.Func")
                    (some #{1} arities))
             `(rx.Observable$OnSubscribeFunc
                (~'onSubscribe [~'this observer#]
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

  This will also implement rx.Observable$OnSubscribeFunc.onSubscribe for use with
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

(defn ^:private dashify
  [name]
  (let [s (str name)
        gsub (clojure.core/fn [s re sub] (.replaceAll (re-matcher re s) sub))]
    (-> s
        (gsub #"([A-Z]+)([A-Z][a-z])" "$1-$2")
        (gsub #"([a-z]+)([A-Z])" "$1-$2")
        (.replace "_" "-")
        (clojure.string/lower-case)
        symbol)))

(defn ^:private wrap-parameter
  [parameter-name parameter-type]
  (let [[parameter-type varargs?] (if (.endsWith (str parameter-type) "<>")
                                    [(symbol (.replace (str parameter-type) "<>" "")) true]
                                    [parameter-type false])
        param-class               (if parameter-type (resolve parameter-type))]
  ;(binding [*out* *err*] (println "HERE " parameter-name "->" (pr-str param-class)))
    (cond
      varargs?                                                   `(into-array ~param-class ~parameter-name)
      (nil? param-class)                                         parameter-name
      (var? param-class)                                         parameter-name
      (.isAssignableFrom rx.util.functions.Action param-class)   `(rx.lang.clojure.interop/action* ~parameter-name)

      (.isAssignableFrom rx.util.functions.Function param-class) `(rx.lang.clojure.interop/fn* ~parameter-name)

      :else parameter-name )))

(defn ^:private wrap-method
  [class-name {:keys [name parameter-types flags] :as method}]
  ;(binding [*out* *err*](println "M -> " (pr-str method)))
  (let [static?     (:static flags)
        this        (if static? class-name 'this)
        param-names (->> parameter-types
                         count
                         range
                         (map #(symbol (str "p" %))))
        param-types (zipmap param-names parameter-types)
        formal-params (if static?
                        param-names
                        (cons this param-names))
        call-params (mapv #(wrap-parameter % (param-types %)) param-names)]
    `(~(vec formal-params)
       (. ~this ~name ~@call-params))))

(defn ^:private count-params
  [method]
  (+ (-> method :parameter-types count)
     (if (-> method :flags :static)
       0
       1)))

(defn ^:private wrap-method-group
  "Generate a defn for a bunch of arities"
  [class-name [name methods]]
  `(defn ~name
      ~@(map (partial wrap-method class-name) methods)))

(defn ^:private split-fixed-args-methods
  [name methods]
  ; For now, only generate functions for overloads with different arities
  (let [groups (group-by count-params methods)
        keepers (->> groups
                     (filter #(= 1 (count (val %))))
                     (mapcat val)
                     seq)]
    (println "Skipping ambiguous overloads: " name "," (- (count methods) (count keepers)))
    (when keepers
      [[(dashify name) keepers]])))

(defn ^:private split-method-group
  [[name methods]]
  (let [{:keys [varargs fixedargs]} (group-by (clojure.core/fn [m] (if (-> m :flags :varargs)
                                                                     :varargs
                                                                     :fixedargs))
                                              methods)]
    ; we may generate several different functions depending on what's found
    (concat (condp = (count varargs)
              0 nil
              1 [[(symbol (str (str (dashify name)) "-&")) varargs]]
              (println "Skipping multi-arity varargs method: " varargs))
            ; if every overload has a distinct number of params...
            (if (or (= 1 (count fixedargs))
                    (and (not-empty fixedargs)
                         (= (count fixedargs)
                            (count (set (map count-params fixedargs))))))
              [[(dashify name) fixedargs]]
              ; otherwise, see what we can do
              (split-fixed-args-methods name fixedargs)))))

(defn ^:private wrap-class*
  [^Class class]
  (let [class-sym (symbol (.getName class))
        methods   (->> (:members (r/reflect class))
                       (filter (comp :public :flags))
                       (group-by :name)
                       (sort-by key))]
    (->> methods
         (mapcat split-method-group)
         (map (clojure.core/fn [group]
                (wrap-method-group class-sym group)))
         (filter identity))))

(defmacro wrap-class
  "Given a class, def a wrapper function for each public method in the class.

  Does the following:

    * Dashifies the name of the method, i.e. mapMany -> map-many
    * If an argument of the method is an rx Func or Action, assumes the argument
      will be a Clojure Ifn and automaticall wraps it appropriately
    * If the method takes varargs, the name of the method will have a -& suffix and will
      take a seq as an argument, automatically converting it to an array as needed.
    * Method overloads only distinguishable by argument type (i.e. same arity) are
      not wrapped. THIS IS THE TRICKY PART.

  Note that this may def functions that conflict with those in clojure.core so 
  clojure.core should not be referred into the current ns.

  "
  [class]
  `(do ~@(wrap-class* (resolve class))))

(comment (doseq [m (wrap-class* rx.Observable)] (println m)))

