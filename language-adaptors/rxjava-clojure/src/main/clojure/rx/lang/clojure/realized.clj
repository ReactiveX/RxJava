(ns rx.lang.clojure.realized
  (:require [rx.lang.clojure.interop :as iop]))

(def ^:private -ns- *ns*)
(set! *warn-on-reflection* true)

(defrecord ^:private PostProc [o f])

(defn all
  "Tell realized map to capture all output of the observable, not just the last one"
  [o]
  (->PostProc o identity))

(defn only
  "Tell realized map to capture the only value emitted by the observable.
  If there are 0 or more than one values, an IllegalStateException is thrown
  which should propagate to onError.

  This is the default mode of realized-map and let-realized.
  "
  [o]
  (->PostProc o (fn [values]
                  (condp = (count values)
                    1 (first values)
                    (throw (IllegalStateException. "Observable did not produce exactly one value"))))))

(defn ^:private ->post-proc
  [v]
  (cond
    (instance? rx.Observable v) (only v)
    (instance? PostProc v)      v
    (vector? v)                 (->PostProc (first v)
                                            (apply comp (reverse (next v))))
    :else                       (->post-proc (rx.Observable/just v))))

(defn realized-map
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  See let-realized.

  Given a map from key to observable, returns an observable that emits a single
  map from the same keys to the values emitted by their corresponding observable.

  keyvals is a list of key/value pairs where key is a key in the emitted map and val
  can be one of the following:

    rx.Observable The only value of the emitted sequence is bound to the key. This is the
                  default since this is often a singleton response from a request. If the
                  Observable produces 0 or more than 1 values, an IllegalStateException is
                  produced.

    vector        The first element of the vector must be an Observable. Remaining elements
                  are functions applied in sequence to the list of values emitted by the
                  observable. For example [my-observable first] will result in a single
                  value in the emitted map rather than a vector of values.

    other         The value is placed in the emitted map as is

  Note the observable can also be wrapped with realized/all to get the full list rather than
  just the last value.

  The purpose of this is to simplify the messy pattern of mapping observables to
  single key maps, merging and then folding all the separate maps together. So code
  like this:

    ; TODO update
    (->> (rx/merge (->> (user-info-o user-id)
                             (rx/map (fn [u] {:user u})))
                        (->> (user-likes-o user-id)
                             (rx/map (fn [u] {:likes u}))))
         (rx/reduce merge {}))

  becomes:

    (realized-map :user  (user-info-o user-id)
                  :likes (user-likes-o user-id))

  See:
    let-realized
  "
  [& keyvals]
  (let [o (->> keyvals
               (partition 2)
               ; generate a sequence of observables
               (map (fn [[k v]]
                      (let [{:keys [^rx.Observable o f]} (->post-proc v)]
                        ; pour the observable into a single list and apply post-proc func to it
                        (-> o
                            .toList
                            (.map (iop/fn [list] {k (f list)})))))))]

    (-> ^Iterable o
        (rx.Observable/merge) ; funnel all the observables into a single sequence
        (.reduce {} (iop/fn* merge))))) ; do the map merge dance

(defn ^rx.Observable realized-map*
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  Same as realized-map, but takes a map argument rather than key-value pairs."
  [map-description]
  (apply realized-map (apply concat map-description)))

(defmacro let-realized
  "EXTREMELY EXPERIMENTAL AND SUBJECT TO CHANGE OR DELETION

  'let' version of realized map.

    (let-realized [a (make-observable)]
      (* 2 a))

  is equivalent to:

    (->> (realized-map :a (make-observable))
         (map (fn [{:keys [a]}] (* 2 a))))

  That is, it eliminates the repition of the map keys when you want to do something
  with the final result.

  Evaluates to an Observable that emits the value of the let body.

  See:
    rx.lang.clojure.realized/realized-map
    rx.lang.clojure.realized/all
  "
  [bindings & body]
  (let [b-parts (partition 2 bindings)
        b-map (->> b-parts
                   (map (fn [[k v]]
                          [(keyword (name k)) v]))
                   (into {}))
        b-names (mapv first b-parts)]
    `(.map (realized-map* ~b-map)
           (iop/fn [{:keys ~b-names}] ~@body))))

