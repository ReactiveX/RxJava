(ns rx.lang.clojure.graph
  "This is an implementation namespace. Don't use it directly. Use the symbols
  in rx.lang.clojure.core
  "
  (:require [clojure.set :as set]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defn ^:private ->let-o*-observable
  [^rx.Observable o n name]
  (if (= n 1)
    o
    ; TODO This is a shortcut. We know the expected number of subscriptions so
    ; we only need to cache values until we get the nth subscription at which
    ; point, it just becomes a pass through. I haven't found a cache/replay-ish
    ; operator that gives this level of control over the cached values
    (.cache o)))

(defn let-o*
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  Given a graph description, returns an observable that emits a single
  map of observables all hooked up and ready for subscription.

  A graph is a map from name to a map with keys:

    :deps     A vector of dependency names
    :factory  A function that takes a map from name to Observable
              for the names in :deps and returns an Observable

  Returns a map from name to Observable. Additionally, there will be a
  ::non-terminals key in the map with a vector of non-terminal names.

  See:
    let-o
  "
  [description]
  (let [in-dep-counts (->> description
                           vals
                           (mapcat :deps)
                           frequencies)
        terminals     (set/difference (set (keys description)) (set (keys in-dep-counts)))
        non-terminals (vec (keys in-dep-counts))

        resolve-node (fn resolve-node [state {:keys [id deps factory] :as node}]
                       (let [existing (state id)]
                         (cond
                           ; It's already resolving up the stack. We've hit a cycle.
                           (= ::resolving existing) (throw (IllegalArgumentException. (format "Cycle found at '%s'" id)))

                           ; It's already resolved. Done.
                           (not (nil? existing))    state

                           :else
                            ; recursively resolve dependencies
                            (let [new-state (reduce (fn [s dep]
                                                      (if-let [dep-node (description dep)]
                                                        (resolve-node s (assoc dep-node :id dep))
                                                        (throw (IllegalArgumentException. (format "Unknown node '%s' referenced from '%s'" dep id)))))
                                                    (assoc state id ::resolving)
                                                    deps)
                                  ; execute the factory function and wrap it in an observable that delays dependencies
                                  o         (-> (select-keys new-state deps)
                                                factory
                                                (->let-o*-observable (in-dep-counts id 1) id))]
                              ; return the updated state with the resolved node
                              (assoc new-state id o)))))]
    ; resolve the graph and build the result map
    (-> (reduce (fn [s [id node]]
                  (resolve-node s (assoc node :id id)))
                {}
                description)
        (select-keys terminals)
        (assoc ::non-terminals non-terminals))))

(defmacro let-o
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  Similar to clojure.core/let, but bindings are Observables and the result of the body
  must be an Observable. Binding names must start with ?. Binding order doesn't matter
  and any binding is visible to all other expressions as long as no cycles are produced
  in the resulting Observable expression.

  The key difference here is that the macro can identify the dependencies between Observables
  and correctly connect them, protecting from variations in subscribe behavior as well as
  the idiosyncracies of setting up multiple subscriptions to Observables.

  This is only very useful for constructing graphs of Observables where you'd usually have
  to fiddle around with publish, connect, replay and all that stuff. If you have a linear
  sequence of operators, just chain them together.

  Current limitations:

    * All Observables are cache()'d so watch out for large sequences. This will be
      fixed eventually.
    * let-o cannot be nested. Some deep-walking macro-magic will be required for this.

  Example:

    ; Note that both ?c and ?d depend on ?b and the result Observable depends on
    ; ?c and ?d.
    (let-o [?a (rx/return 99)
            ?b (... some observable network request ...)
            ?c (rx/map vector ?a ?b)
            ?d (rx/map ... ?b)]
      (rx/map vector ?c ?d))

  See:
    let-o*
  "
  [bindings & result-body]
  (let [sym->dep-sym (fn [s]
                       (when (and (symbol? s)
                                  (not (namespace s))
                                  (.startsWith (name s) "?"))
                         s))
        body->dep-syms (fn [body]
                          (->> body
                               flatten
                               (keep sym->dep-sym)
                               distinct
                               vec))
        ->node-map (fn [[id & body]]
                     (let [dep-syms (body->dep-syms body)
                           dep-keys (->> dep-syms (map (comp keyword name)) vec)]
                       [(keyword (name id)) {:deps dep-keys
                                             :factory `(fn [{:keys ~dep-syms}] ~@body) }]))
        node-map (let [base-map  (->> bindings
                                      (partition 2)
                                      (map ->node-map)
                                      (into {}))
                       result-dep-syms (body->dep-syms result-body)]
                   (assoc base-map
                          :rx.lang.clojure.core/result
                          {:deps     (mapv keyword result-dep-syms)
                           :factory `(fn [{:keys ~result-dep-syms}] ~@result-body) }))]
    `(->> ~node-map
          let-o*
          :rx.lang.clojure.core/result)))

