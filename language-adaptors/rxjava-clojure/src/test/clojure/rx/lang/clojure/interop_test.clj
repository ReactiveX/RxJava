(ns rx.lang.clojure.interop-test
  (:require [rx.lang.clojure.interop :as rx]
            [clojure.test :refer [deftest testing is]])
  (:import [rx Observable]
           [rx.observables BlockingObservable]
           [rx.lang.clojure.interop DummyObservable]))

(deftest test-fn*
  (testing "implements Func0-9"
    (let [f (rx/fn* vector)]
      (is (instance? rx.Observable$OnSubscribeFunc f))
      (is (instance? rx.functions.Func0 f))
      (is (instance? rx.functions.Func1 f))
      (is (instance? rx.functions.Func2 f))
      (is (instance? rx.functions.Func3 f))
      (is (instance? rx.functions.Func4 f))
      (is (instance? rx.functions.Func5 f))
      (is (instance? rx.functions.Func6 f))
      (is (instance? rx.functions.Func7 f))
      (is (instance? rx.functions.Func8 f))
      (is (instance? rx.functions.Func9 f))
      (is (= []                  (.call f)))
      (is (= [1]                 (.call f 1)))
      (is (= [1 2]               (.call f 1 2)))
      (is (= [1 2 3]             (.call f 1 2 3)))
      (is (= [1 2 3 4]           (.call f 1 2 3 4)))
      (is (= [1 2 3 4 5]         (.call f 1 2 3 4 5)))
      (is (= [1 2 3 4 5 6]       (.call f 1 2 3 4 5 6)))
      (is (= [1 2 3 4 5 6 7]     (.call f 1 2 3 4 5 6 7)))
      (is (= [1 2 3 4 5 6 7 8]   (.call f 1 2 3 4 5 6 7 8)))
      (is (= [1 2 3 4 5 6 7 8 9] (.call f 1 2 3 4 5 6 7 8 9)))))

  (let [dummy (DummyObservable.)]
    (testing "preserves metadata applied to form"
      ; No type hint, picks Object overload
      (is (= "Object"
             (.call dummy (rx/fn* +))))
      (is (= "rx.functions.Func1"
             (.call dummy
                    ^rx.functions.Func1 (rx/fn* +))))
      (is (= "rx.functions.Func2"
             (.call dummy
                    ^rx.functions.Func2 (rx/fn* *)))))))

(deftest test-fn
  (testing "makes appropriate Func*"
    (let [f (rx/fn [a b c] (println "test-fn") (+ a b c))]
      (is (= 6 (.call f 1 2 3)))))

  (let [dummy (DummyObservable.)]
    (testing "preserves metadata applied to form"
      ; No type hint, picks Object overload
      (is (= "Object"
             (.call dummy
                    (rx/fn [a] a))))
      (is (= "rx.functions.Func1"
             (.call dummy
                    ^rx.functions.Func1 (rx/fn [a] a))))
      (is (= "rx.functions.Func2"
             (.call dummy
                    ^rx.functions.Func2 (rx/fn [a b] (* a b))))))))


(deftest test-fnN*
  (testing "implements FuncN"
    (is (= (vec (range 99))
           (.call (rx/fnN* vector) (into-array Object (range 99)))))))

(deftest test-action*
  (testing "implements Action0-3"
    (let [calls (atom [])
          a (rx/action* #(swap! calls conj (vec %&)))]
      (is (instance? rx.Observable$OnSubscribe a))
      (is (instance? rx.functions.Action0 a))
      (is (instance? rx.functions.Action1 a))
      (is (instance? rx.functions.Action2 a))
      (is (instance? rx.functions.Action3 a))
      (.call a)
      (.call a 1)
      (.call a 1 2)
      (.call a 1 2 3)
      (is (= [[] [1] [1 2] [1 2 3]]))))
  (let [dummy (DummyObservable.)]
    (testing "preserves metadata applied to form"
      ; no meta, picks Object overload
      (is (= "Object"
             (.call dummy
                    (rx/action* println))))
      (is (= "rx.functions.Action1"
             (.call dummy
                    ^rx.functions.Action1 (rx/action* println))))
      (is (= "rx.functions.Action2"
             (.call dummy
                    ^rx.functions.Action2 (rx/action* prn)))))))

(deftest test-action
  (testing "makes appropriate Action*"
    (let [called (atom nil)
          a (rx/action [a b] (reset! called [a b]))]
      (.call a 9 10)
      (is (= [9 10] @called))))

  (let [dummy (DummyObservable.)]
    (testing "preserves metadata applied to form"
      ; no meta, picks Object overload
      (is (= "Object"
             (.call dummy
                    (rx/action [a] a))))
      (is (= "rx.functions.Action1"
             (.call dummy
                    ^rx.functions.Action1 (rx/action [a] a))))
      (is (= "rx.functions.Action2"
             (.call dummy
                    ^rx.functions.Action2 (rx/action [a b] (* a b))))))))

(deftest test-basic-usage

  (testing "can create an observable with old style fn"
    (is (= 99
           (-> (Observable/create (rx/fn [^rx.Observer o]
                                    (.onNext o 99)
                                    (.onCompleted o)
                                    (rx.subscriptions.Subscriptions/empty)))
               .toBlockingObservable
               .single))))

  (testing "can create an observable with new-style action"
    (is (= 99
           (-> (Observable/create (rx/action [^rx.Subscriber s]
                                    (when-not (.isUnsubscribed s)
                                      (.onNext s 99))
                                    (.onCompleted s)))
               .toBlockingObservable
               .single))))
  (testing "can pass rx/fn to map and friends"
    (is (= (+ 1 4 9)
           (-> (Observable/from [1 2 3])
               (.map (rx/fn [v] (* v v)))
               (.reduce (rx/fn* +))
           .toBlockingObservable
           .single))))

  (testing "can pass rx/action to subscribe and friends"
    (let [finally-called (atom nil)
          complete-called (promise)
          result (atom [])
          o (-> (Observable/from ["4" "5" "6"])
                 (.map (rx/fn* #(Long/parseLong %)))
                 (.finallyDo (rx/action []
                                             (reset! finally-called true)))
                 (.reduce (rx/fn [a v] (* a v)))
                 (.subscribe (rx/action [v] (swap! result conj v))
                             (rx/action [e])
                             (rx/action [] (deliver complete-called true)))) ]
      (is (= true @complete-called))
      (is (= true @finally-called))
      (is (= [(* 4 5 6)] @result)))))

;################################################################################
