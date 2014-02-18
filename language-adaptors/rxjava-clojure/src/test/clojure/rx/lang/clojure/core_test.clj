(ns rx.lang.clojure.core-test
  (:require [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.blocking :as b]
            [clojure.test :refer [deftest is testing are]]))

(deftest test-observable?
  (is (rx/observable? (rx/return 99)))
  (is (not (rx/observable? "I'm not an observable"))))

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

(deftest test-fn->subscription
  (let [called (atom 0)
        s (rx/fn->subscription #(swap! called inc))]
    (is (identical? s (rx/unsubscribe s)))
    (is (= 1 @called))))

(deftest test-unsubscribed?
  (let [s (rx/fn->subscription #())]
    (is (not (rx/unsubscribed? s)))
    (rx/unsubscribe s)
    (is (rx/unsubscribed? s))))


(deftest test-fn->o
  (let [o (rx/fn->o (fn [s]
                      (rx/on-next s 0)
                      (rx/on-next s 1)
                      (when-not (rx/unsubscribed? s) (rx/on-next s 2))
                      (rx/on-completed s)))]
    (is (= [0 1 2] (b/into [] o)))))

(deftest test-->operator
  (let [o (rx/->operator #(rx/->subscriber %
                                           (fn [o v]
                                             (if (even? v)
                                               (rx/on-next o v)))))
        result (->> (rx/seq->o [1 2 3 4 5])
                    (rx/lift o)
                    (b/into []))]
    (is (= [2 4] result))))

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

(deftest test-interpose
  (is (= (interpose \, [1 2 3])
         (b/into [] (rx/interpose \, (rx/seq->o [1 2 3]))))))

(deftest test-into
  (is (= (into [6 7 8] [9 10 [11]])
         (b/first (rx/into [6 7 8] (rx/seq->o [9 10 [11]]))))))

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

(deftest test-map-indexed
  (is (= (map-indexed vector [:a :b :c])
         (b/into [] (rx/map-indexed vector (rx/seq->o [:a :b :c]))))))

(deftest test-merge
  (is (= [{:a 1 :b 2 :c 3 :d 4}]
         (b/into [] (rx/merge (rx/seq->o [{:a 1 :d 0} {:b 2} {:c 3} {:d 4} ]))))))

(deftest test-mapcat
  (let [f  (fn [v] [v (* v v)])
        xs (range 10)]
    (is (= (mapcat f xs)
           (b/into [] (rx/mapcat (comp rx/seq->o f) (rx/seq->o xs))))))
  (comment
    (is (= (into [] (mapcat vector
                              [:q :r :s :t :u]
                              (range 10)
                              ["a" "b" "c" "d" "e"] ))
        (b/into [] (rx/mapcat vector
                              (rx/seq->o [:q :r :s :t :u])
                              (rx/seq->o (range 10) )
                              (rx/seq->o ["a" "b" "c" "d" "e"] )))))))

(deftest test-next
  (let [in [:q :r :s :t :u]]
    (is (= (next in) (b/into [] (rx/next (rx/seq->o in)))))))

(deftest test-rest
  (let [in [:q :r :s :t :u]]
    (is (= (rest in) (b/into [] (rx/rest (rx/seq->o in)))))))

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
  (is (= [[]] (b/into [] (rx/sort (rx/empty)))))
  (is (= [[1 2 3]]
         (b/into [] (rx/sort (rx/seq->o [3 1 2])))))
  (is (= [[3 2 1]]
         (b/into [] (rx/sort (fn [a b] (- (compare a b))) (rx/seq->o [2 1 3]))))))

(deftest test-sort-by
  (is (= [[]] (b/into [] (rx/sort-by :foo (rx/empty)))))
  (is (= [[{:foo 1} {:foo 2} {:foo 3}]]
         (b/into [] (rx/sort-by :foo (rx/seq->o [{:foo 2}{:foo 1}{:foo 3}])))))
  (is (= [[{:foo 3} {:foo 2} {:foo 1}]]
         (b/into [] (rx/sort-by :foo (fn [a b] (- (compare a b))) (rx/seq->o [{:foo 2}{:foo 1}{:foo 3}]))))))

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
           (b/into []
                   (->
                     (rx/seq->o [1 2 3])
                     (rx/catch* Exception (fn [e] (throw "OH NO"))))))))

  (testing "Can catch a particular exception type and continue with an observable"
    (is (= [1 2 4 5 6 "foo"]
           (b/into []
                   (->
                     (rx/generator [o]
                                   (rx/on-next o 1)
                                   (rx/on-next o 2)
                                   (rx/on-error o (IllegalStateException. "foo")))
                     (rx/catch* IllegalStateException
                             (fn [e]
                               (rx/seq->o [4 5 6 (.getMessage e)]))))))))

  (testing "if exception isn't matched, it's passed to on-error"
    (let [expected (IllegalArgumentException. "HI")
          called (atom nil)]
    (rx/subscribe (->
                    (rx/generator [o]
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
             (b/into []
                     (->
                       (rx/generator [o]
                                     (rx/on-error o wrapper))
                       (rx/catch #(.getCause %) e
                         (rx/return e)))))))))


(deftest test-finally
  (testing "Supports a finally clause"
    (testing "called on completed"
      (let [completed (atom nil)
            called (atom nil)]
        (rx/subscribe (->
                        (rx/seq->o [1 2 3])
                        (rx/finally* (fn [extra] (reset! called (str "got " extra)))
                                     "it"))
                      (fn [_])
                      (fn [_] (throw (IllegalStateException. "WAT")))
                      (fn [] (reset! completed "DONE")))
        (is (= "got it" @called))
        (is (= "DONE" @completed))))

    (testing "called on error"
      (let [expected (IllegalStateException. "expected")
            completed (atom nil)
            called (atom nil)]
        (rx/subscribe (->
                        (rx/generator [o]
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
