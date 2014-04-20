(ns rx.lang.clojure.core-test
  (:require [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.blocking :as b]
            [rx.lang.clojure.future :as f]
            [clojure.test :refer [deftest is testing are]]))

(deftest test-observable?
  (is (rx/observable? (rx/return 99)))
  (is (not (rx/observable? "I'm not an observable"))))

(deftest test-on-next
  (testing "calls onNext"
    (let [called (atom [])
          o (reify rx.Observer (onNext [this value] (swap! called conj value)))]
      (is (identical? o (rx/on-next o 1)))
      (is (= [1] @called)))))

(deftest test-on-completed
  (testing "calls onCompleted"
    (let [called (atom 0)
          o (reify rx.Observer (onCompleted [this] (swap! called inc)))]
      (is (identical? o (rx/on-completed o)))
      (is (= 1 @called)))))

(deftest test-on-error
  (testing "calls onError"
    (let [called (atom [])
          e (java.io.FileNotFoundException. "yum")
          o (reify rx.Observer (onError [this e] (swap! called conj e)))]
      (is (identical? o (rx/on-error o e)))
      (is (= [e] @called)))))

(deftest test-catch-error-value
  (testing "if no exception, returns body"
    (let [o (reify rx.Observer)]
      (is (= 3 (rx/catch-error-value o 99
                 (+ 1 2))))))

  (testing "exceptions call onError on observable and inject value in exception"
    (let [called (atom [])
          e      (java.io.FileNotFoundException. "boo")
          o      (reify rx.Observer
                   (onError [this e]
                     (swap! called conj e)))
          result (rx/catch-error-value o 100
                   (throw e))
          cause  (.getCause e)]
      (is (identical? e result))
      (is (= [e] @called))
      (when (is (instance? rx.exceptions.OnErrorThrowable$OnNextValue cause))
        (is (= 100 (.getValue cause)))))))

(deftest test-subscribe
  (testing "subscribe overload with only onNext"
    (let [o (rx/return 1)
          called (atom nil)]
      (rx/subscribe o (fn [v] (swap! called (fn [_] v))))
      (is (= 1 @called)))))

(deftest test-fn->predicate
  (are [f arg result] (= result (.call (rx/fn->predicate f) arg))
       identity nil    false
       identity false  false
       identity 1      true
       identity "true" true
       identity true   true))

(deftest test-subscription
  (let [called (atom 0)
        s (rx/subscription #(swap! called inc))]
    (is (identical? s (rx/unsubscribe s)))
    (is (= 1 @called))))

(deftest test-unsubscribed?
  (let [s (rx/subscription #())]
    (is (not (rx/unsubscribed? s)))
    (rx/unsubscribe s)
    (is (rx/unsubscribed? s))))


(deftest test-observable*
  (let [o (rx/observable* (fn [s]
                            (rx/on-next s 0)
                            (rx/on-next s 1)
                            (when-not (rx/unsubscribed? s) (rx/on-next s 2))
                            (rx/on-completed s)))]
    (is (= [0 1 2] (b/into [] o)))))

(deftest test-operator*
  (let [o (rx/operator* #(rx/subscriber %
                                           (fn [o v]
                                             (if (even? v)
                                               (rx/on-next o v)))))
        result (->> (rx/seq->o [1 2 3 4 5])
                    (rx/lift o)
                    (b/into []))]
    (is (= [2 4] result))))

(deftest test-serialize
  ; I'm going to believe serialize works and just exercise it
  ; here for sanity.
  (is (= [1 2 3]
         (->> [1 2 3]
              (rx/seq->o)
              (rx/serialize)
              (b/into [])))))

(let [expected-result [[1 3 5] [2 4 6]]
      sleepy-o        #(f/future-generator*
                         future-call
                         (fn [o]
                           (doseq [x %]
                             (Thread/sleep 10)
                             (rx/on-next o x))))
      make-inputs (fn [] (mapv sleepy-o expected-result))
      make-output (fn [r] [(keep #{1 3 5} r)
                           (keep #{2 4 6} r)])]
  (deftest test-merge*
    (is (= expected-result
           (->> (make-inputs)
                (rx/seq->o)
                (rx/merge*)
                (b/into [])
                (make-output)))))
  (deftest test-merge
    (is (= expected-result
           (->> (make-inputs)
                (apply rx/merge)
                (b/into [])
                (make-output)))))
  (deftest test-merge-delay-error*
    (is (= expected-result
           (->> (make-inputs)
                (rx/seq->o)
                (rx/merge-delay-error*)
                (b/into [])
                (make-output)))))
  (deftest test-merge-delay-error
    (is (= expected-result
           (->> (make-inputs)
                (apply rx/merge-delay-error)
                (b/into [])
                (make-output))))))

(deftest test-generator
  (testing "calls on-completed automatically"
    (let [o (rx/generator [o])
          called (atom nil)]
      (rx/subscribe o (fn [v]) (fn [_]) #(reset! called "YES"))
      (is (= "YES" @called))))

  (testing "exceptions automatically go to on-error"
    (let [expected (IllegalArgumentException. "hi")
          actual   (atom nil)]
      (rx/subscribe (rx/generator [o] (throw expected))
                     #()
                     #(reset! actual %))
      (is (identical? expected @actual)))))

(deftest test-seq->o
  (is (= [] (b/into [] (rx/seq->o []))))
  (is (= [] (b/into [] (rx/seq->o nil))))
  (is (= [\a \b \c] (b/into [] (rx/seq->o "abc"))))
  (is (= [0 1 2 3] (b/first (rx/into [] (rx/seq->o (range 4))))))
  (is (= #{0 1 2 3} (b/first (rx/into #{} (rx/seq->o (range 4))))))
  (is (= {:a 1 :b 2 :c 3} (b/first (rx/into {} (rx/seq->o [[:a 1] [:b 2] [:c 3]]))))))

(deftest test-return
  (is (= [0] (b/into [] (rx/return 0)))))

(deftest test-cache
  (let [value (atom 0)
        o (->>
            (rx/return 0)
            (rx/map (fn [x] (swap! value inc)))
            (rx/cache))]
    (is (= 1 (b/single o)))
    (is (= 1 @value))
    (is (= 1 (b/single o)))
    (is (= 1 @value))
    (is (= 1 (b/single o)))))

(deftest test-cons
  (is (= [1] (b/into [] (rx/cons 1 (rx/empty)))))
  (is (= [1 2 3 4] (b/into [] (rx/cons 1 (rx/seq->o [2 3 4]))))))

(deftest test-concat
  (is (= [:q :r]
         (b/into [] (rx/concat (rx/seq->o [:q :r])))))
  (is (= [:q :r 1 2 3]
         (b/into [] (rx/concat (rx/seq->o [:q :r])
                               (rx/seq->o [1 2 3]))))))

(deftest test-concat*
  (is (= [:q :r]
         (b/into [] (rx/concat* (rx/return (rx/seq->o [:q :r]))))))
  (is (= [:q :r 1 2 3]
         (b/into [] (rx/concat* (rx/seq->o [(rx/seq->o [:q :r])
                                            (rx/seq->o [1 2 3])]))))))

(deftest test-count
  (are [xs] (= (count xs) (->> xs (rx/seq->o) (rx/count) (b/single)))
       []
       [1]
       [5 6 7]
       (range 10000)))

(deftest test-cycle
  (is (= [1 2 3 1 2 3 1 2 3 1 2]
         (->> [1 2 3]
              (rx/seq->o)
              (rx/cycle)
              (rx/take 11)
              (b/into [])))))

(deftest test-distinct
  (let [input [{:a 1} {:a 1} {:b 1} {"a" (int 1)} {:a (int 1)}]]
    (is (= (distinct input)
           (->> input
                (rx/seq->o)
                (rx/distinct)
                (b/into [])))))
  (let [input [{:name "Bob" :x 2} {:name "Jim" :x 99} {:name "Bob" :x 3}]]
    (is (= [{:name "Bob" :x 2} {:name "Jim" :x 99}]
           (->> input
                (rx/seq->o)
                (rx/distinct :name)
                (b/into []))))))

(deftest test-do
  (testing "calls a function with each element"
    (let [collected (atom [])]
      (is (= [1 2 3]
             (->> (rx/seq->o [1 2 3])
                  (rx/do (fn [v]
                           (swap! collected conj (* 2 v))))
                  (rx/do (partial println "GOT"))
                  (b/into []))))
      (is (= [2 4 6] @collected))))
  (testing "ends sequence with onError if action code throws an exception"
    (let [collected (atom [])
          o (->> (rx/seq->o [1 2 3])
                 (rx/do (fn [v]
                           (if (= v 2)
                             (throw (IllegalStateException. (str "blah" v)))
                             (swap! collected conj (* 99 v))))))]
      (is (thrown-with-msg? IllegalStateException #"blah2"
                            (b/into [] o)))
      (is (= [99] @collected)))))

(deftest test-drop-while
  (is (= (into [] (drop-while even? [2 4 6 8 1 2 3]))
         (b/into [] (rx/drop-while even? (rx/seq->o [2 4 6 8 1 2 3])))))
  (is (= (into [] (drop-while even? [2 4 6 8 1 2 3]))
         (b/into [] (rx/drop-while even? (rx/seq->o [2 4 6 8 1 2 3]))))))

(deftest test-every?
  (are [xs p result] (= result (->> xs (rx/seq->o) (rx/every? p) (b/single)))
       [2 4 6 8] even?      true
       [2 4 3 8] even?      false
       [1 2 3 4] #{1 2 3 4} true
       [1 2 3 4] #{1 3 4}   false))

(deftest test-filter
  (is (= (into [] (->> [:a :b :c :d :e :f :G :e]
                    (filter #{:b :e :G})))
         (b/into [] (->> (rx/seq->o [:a :b :c :d :e :f :G :e])
                        (rx/filter #{:b :e :G}))))))

(deftest test-first
  (is (= [3]
         (b/into [] (rx/first (rx/seq->o [3 4 5])))))
  (is (= []
         (b/into [] (rx/first (rx/empty))))))

(deftest test-group-by
  (let [xs [{:k :a :v 1} {:k :b :v 2} {:k :a :v 3} {:k :c :v 4}]]
    (testing "with just a key-fn"
      (is (= [[:a {:k :a :v 1}]
              [:b {:k :b :v 2}]
              [:a {:k :a :v 3}]
              [:c {:k :c :v 4}]]
             (->> xs
                  (rx/seq->o)
                  (rx/group-by :k)
                  (rx/mapcat (fn [[k vo :as me]]
                               (is (instance? clojure.lang.MapEntry me))
                               (rx/map #(vector k %) vo)))
                  (b/into [])))))

    ; TODO reinstate once this is implemented
    ; see https://github.com/Netflix/RxJava/commit/02ccc4d727a9297f14219549208757c6e0efce2a
    #_(testing "with a val-fn"
      (is (= [[:a 1]
              [:b 2]
              [:a 3]
              [:c 4]]
             (->> xs
                  (rx/seq->o)
                  (rx/group-by :k :v)
                  (rx/mapcat (fn [[k vo :as me]]
                               (is (instance? clojure.lang.MapEntry me))
                               (rx/map #(vector k %) vo)))
                  (b/into [])))))))

(deftest test-interleave
  (are [inputs] (= (apply interleave inputs)
                   (->> (apply rx/interleave (map rx/seq->o inputs))
                        (b/into [])))
       [[] []]
       [[] [1]]
       [(range 5) (range 10) (range 10) (range 3)]
       [(range 50) (range 10)]
       [(range 5) (range 10 60) (range 10) (range 50)])

  ; one-arg case, not supported by clojure.core/interleave
  (is (= (range 10)
         (->> (rx/interleave (rx/seq->o (range 10)))
              (b/into [])))))

(deftest test-interleave*
  (are [inputs] (= (apply interleave inputs)
                   (->> (rx/interleave* (->> inputs
                                             (map rx/seq->o)
                                             (rx/seq->o)))
                        (b/into [])))
       [[] []]
       [[] [1]]
       [(range 5) (range 10) (range 10) (range 3)]
       [(range 50) (range 10)]
       [(range 5) (range 10 60) (range 10) (range 50)]))

(deftest test-interpose
  (is (= (interpose \, [1 2 3])
         (b/into [] (rx/interpose \, (rx/seq->o [1 2 3]))))))

(deftest test-into
  (are [input to] (= (into to input)
                     (b/single (rx/into to (rx/seq->o input))))
       [6 7 8] [9 10 [11]]
       #{} [1 2 3 2 4 5]
       {} [[1 2] [3 2] [4 5]]
       {} []
       '() (range 50)))

(deftest test-iterate
  (are [f x n] (= (->> (iterate f x) (take n))
                  (->> (rx/iterate f x) (rx/take n) (b/into [])))
       inc 0 10
       dec 20 100
       #(conj % (count %)) [] 5
       #(cons (count %) % ) nil 5))

(deftest test-keep
  (is (= (into [] (keep identity [true true false]))
         (b/into [] (rx/keep identity (rx/seq->o [true true false])))))

  (is (= (into [] (keep #(if (even? %) (* 2 %)) (range 9)))
         (b/into [] (rx/keep #(if (even? %) (* 2 %)) (rx/seq->o (range 9)))))))

(deftest test-keep-indexed
  (is (= (into [] (keep-indexed (fn [i v]
                                  (if (even? i) v))
                                [true true false]))
         (b/into [] (rx/keep-indexed (fn [i v]
                                         (if (even? i) v))
                                       (rx/seq->o [true true false]))))))

(deftest test-map
  (is (= (into {} (map (juxt identity name)
                       [:q :r :s :t :u]))
         (b/into {} (rx/map (juxt identity name)
                              (rx/seq->o [:q :r :s :t :u])))))
  (is (= (into [] (map vector
                       [:q :r :s :t :u]
                       (range 10)
                       ["a" "b" "c" "d" "e"] ))
         (b/into [] (rx/map vector
                              (rx/seq->o [:q :r :s :t :u])
                              (rx/seq->o (range 10) )
                              (rx/seq->o ["a" "b" "c" "d" "e"] )))))
  ; check > 4 arg case
  (is (= (into [] (map vector
                       [:q :r :s :t :u]
                       [:q :r :s :t :u]
                       [:q :r :s :t :u]
                       (range 10)
                       (range 10)
                       (range 10)
                       ["a" "b" "c" "d" "e"]
                       ["a" "b" "c" "d" "e"]
                       ["a" "b" "c" "d" "e"]))
         (b/into [] (rx/map vector
                            (rx/seq->o [:q :r :s :t :u])
                            (rx/seq->o [:q :r :s :t :u])
                            (rx/seq->o [:q :r :s :t :u])
                            (rx/seq->o (range 10))
                            (rx/seq->o (range 10))
                            (rx/seq->o (range 10))
                            (rx/seq->o ["a" "b" "c" "d" "e"])
                            (rx/seq->o ["a" "b" "c" "d" "e"])
                            (rx/seq->o ["a" "b" "c" "d" "e"]))))))

(deftest test-map*
  (is (= [[1 2 3 4 5 6 7 8]]
           (b/into [] (rx/map* vector
                               (rx/seq->o [(rx/seq->o [1])
                                           (rx/seq->o [2])
                                           (rx/seq->o [3])
                                           (rx/seq->o [4])
                                           (rx/seq->o [5])
                                           (rx/seq->o [6])
                                           (rx/seq->o [7])
                                           (rx/seq->o [8])]))))))
(deftest test-map-indexed
  (is (= (map-indexed vector [:a :b :c])
         (b/into [] (rx/map-indexed vector (rx/seq->o [:a :b :c])))))
  (testing "exceptions from fn have error value injected"
    (try
      (->> (rx/seq->o [:a :b :c])
           (rx/map-indexed (fn [i v]
                        (if (= 1 i)
                          (throw (java.io.FileNotFoundException. "blah")))
                        v))
           (b/into []))
      (catch java.io.FileNotFoundException e
        (is (= :b (-> e .getCause .getValue)))))))

(deftest test-mapcat*
  (let [f (fn [a b c d e]
            [(+ a b) (+ c d) e])]
    (is (= (->> (range 5)
                (map (fn [_] (range 5)))
                (apply mapcat f))
           (->> (range 5)
                (map (fn [_] (rx/seq->o (range 5))))
                (rx/seq->o)
                (rx/mapcat* (fn [& args] (rx/seq->o (apply f args))))
                (b/into []))))))

(deftest test-mapcat
  (let [f  (fn [v] [v (* v v)])
        xs (range 10)]
    (is (= (mapcat f xs)
           (b/into [] (rx/mapcat (comp rx/seq->o f) (rx/seq->o xs))))))

  (let [f  (fn [a b] [a b (* a b)])
        as (range 10)
        bs (range 15)]
    (is (= (mapcat f as bs)
           (b/into [] (rx/mapcat (comp rx/seq->o f)
                                 (rx/seq->o as)
                                 (rx/seq->o bs)))))))

(deftest test-next
  (let [in [:q :r :s :t :u]]
    (is (= (next in) (b/into [] (rx/next (rx/seq->o in)))))))

(deftest test-nth
  (is (= [:a]
         (b/into [] (rx/nth (rx/seq->o [:s :b :a :c]) 2))))
  (is (= [:fallback]
         (b/into [] (rx/nth (rx/seq->o [:s :b :a :c]) 25 :fallback)))))

(deftest test-rest
  (let [in [:q :r :s :t :u]]
    (is (= (rest in) (b/into [] (rx/rest (rx/seq->o in)))))))

(deftest test-partition-all
  (are [input-size part-size step] (= (->> (range input-size)
                                           (partition-all part-size step))
                                      (->> (range input-size)
                                           (rx/seq->o)
                                           (rx/partition-all part-size step)
                                           (rx/map #(rx/into [] %))
                                           (rx/concat*)
                                           (b/into [])))
       0 1 1
       10 2 2
       10 3 2
       15 30 4)

  (are [input-size part-size] (= (->> (range input-size)
                                      (partition-all part-size))
                                 (->> (range input-size)
                                      (rx/seq->o)
                                      (rx/partition-all part-size)
                                      (rx/map #(rx/into [] %))
                                      (rx/concat*)
                                      (b/into [])))
       0 1
       10 2
       10 3
       15 30))

(deftest test-range
  (are [start end step] (= (range start end step)
                           (->> (rx/range start end step) (b/into [])))
       0   10  2
       0 -100 -1
       5 100   9)

  (are [start end] (= (range start end)
                      (->> (rx/range start end) (b/into [])))
       0   10
       0 -100
       5 100)

  (are [start] (= (->> (range start) (take 100))
                  (->> (rx/range start) (rx/take 100) (b/into [])))
       50
       0
       5
       -20)
  (is (= (->> (range) (take 500))
         (->> (rx/range) (rx/take 500) (b/into [])))))

(deftest test-reduce
  (is (= (reduce + 0 (range 4))
         (b/first (rx/reduce + 0 (rx/seq->o (range 4)))))))

(deftest test-reductions
  (is (= (into [] (reductions + 0 (range 4)))
         (b/into [] (rx/reductions + 0 (rx/seq->o (range 4)))))))

(deftest test-some
  (is (= [:r] (b/into [] (rx/some #{:r :s :t} (rx/seq->o [:q :v :r])))))
  (is (= [] (b/into [] (rx/some #{:r :s :t} (rx/seq->o [:q :v]))))))

(deftest test-sort
  (are [in cmp] (= (if cmp
                     (sort cmp in)
                     (sort in))
                   (->> in
                        (rx/seq->o)
                        (#(if cmp (rx/sort cmp %) (rx/sort %)))
                        (b/into [])))
       []      nil
       []      (comp - compare)
       [3 1 2] nil
       [1 2 3] nil
       [1 2 3] (comp - compare)
       [2 1 3] (comp - compare)))

(deftest test-sort-by
  (are [rin cmp] (let [in (map #(hash-map :foo %) rin)]
                   (= (if cmp
                        (sort-by :foo cmp in)
                        (sort-by :foo in))
                      (->> in
                           (rx/seq->o)
                           (#(if cmp (rx/sort-by :foo cmp %) (rx/sort-by :foo %)))
                           (b/into []))))
       []      nil
       []      (comp - compare)
       [3 1 2] nil
       [1 2 3] nil
       [1 2 3] (comp - compare)
       [2 1 3] (comp - compare)))


(deftest test-split-with
  (is (= (split-with (partial >= 3) (range 6))
         (->> (rx/seq->o (range 6))
              (rx/split-with (partial >= 3))
              b/first
              (map (partial b/into []))))))

(deftest test-take-while
  (is (= (into [] (take-while even? [2 4 6 8 1 2 3]))
         (b/into [] (rx/take-while even? (rx/seq->o [2 4 6 8 1 2 3]))))))

(deftest test-throw
  (let [expected (IllegalArgumentException. "HI")
        called (atom nil)]
    (rx/subscribe (rx/throw expected)
                  (fn [_])
                  (fn [e] (reset! called expected))
                  (fn [_]))
    (is (identical? expected @called))))

(deftest test-catch*
  (testing "Is just a passthrough if there's no error"
    (is (= [1 2 3]
           (->> (rx/seq->o [1 2 3])
                (rx/catch* Exception (fn [e] (throw "OH NO")))
                (b/into [])))))

  (testing "Can catch a particular exception type and continue with an observable"
    (is (= [1 2 4 5 6 "foo"]
           (->> (rx/generator [o]
                              (rx/on-next o 1)
                              (rx/on-next o 2)
                              (rx/on-error o (IllegalStateException. "foo")))
                (rx/catch* IllegalStateException
                           (fn [e]
                             (rx/seq->o [4 5 6 (.getMessage e)])))
                (b/into [])))))

  (testing "if exception isn't matched, it's passed to on-error"
    (let [expected (IllegalArgumentException. "HI")
          called (atom nil)]
      (rx/subscribe (->> (rx/generator [o]
                                       (rx/on-next o 1)
                                       (rx/on-next o 2)
                                       (rx/on-error o expected))
                         (rx/catch* IllegalStateException (fn [e]
                                                            (rx/return "WAT?"))))
                  (fn [_])
                  (fn [e] (reset! called expected))
                  (fn [_]))
    (is (identical? expected @called))))

  (testing "if p returns Throwable, that's passed as e"
    (let [cause (IllegalArgumentException. "HI")
          wrapper (java.util.concurrent.ExecutionException. cause)]
      (is (= [cause]
             (->> (rx/throw wrapper)
                  (rx/catch #(.getCause %) e
                    (rx/return e))
                  (b/into [])))))))


(deftest test-finally
  (testing "Supports a finally clause"
    (testing "called on completed"
      (let [completed (atom nil)
            called (atom nil)]
        (rx/subscribe (->> (rx/seq->o [1 2 3])
                           (rx/finally* (fn [] (reset! called (str "got it")))))
                      (fn [_])
                      (fn [_] (throw (IllegalStateException. "WAT")))
                      (fn [] (reset! completed "DONE")))
        (is (= "got it" @called))
        (is (= "DONE" @completed))))

    (testing "called on error"
      (let [expected (IllegalStateException. "expected")
            completed (atom nil)
            called (atom nil)]
        (rx/subscribe (->> (rx/generator [o]
                                         (rx/on-next o 1)
                                         (rx/on-next o 2)
                                         (rx/on-error o expected))
                           (rx/finally
                             (reset! called "got it")))
                      (fn [_])
                      (fn [e] (reset! completed e))
                      (fn [] (throw (IllegalStateException. "WAT"))))
        (is (= "got it" @called))
        (is (identical? expected @completed))))))


;################################################################################

(deftest test-graph-imports
  (is (= 99
         (-> {:a {:deps [] :factory (fn [_] (rx/return 99))}}
             rx/let-o*
             :a
             b/single)))
  (is (= 100
         (b/single (rx/let-o [?a (rx/return 100)]
                     ?a)))))

;################################################################################

(deftest test-realized-imports
  (is (= {:a 1 :b 2}
         (->> (rx/let-realized [a (rx/return 1)
                                b (rx/return 2)]
                {:a a :b b})
              b/single))))


