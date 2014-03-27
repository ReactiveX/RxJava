(ns rx.lang.clojure.realized-test
  (:require [rx.lang.clojure.realized :as r]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.future :as rx-future]
            [rx.lang.clojure.blocking :as rx-blocking]
            [clojure.test :refer [deftest testing is]]))



(deftest test-realized-map
  (testing "Turns map of observables into observable of map"
    (let [o (r/realized-map :a (r/all (rx/seq->o [1 2 3]))
                               :a2 (rx/seq->o [99 100 101])
                               :b (rx/return "hi")
                               :c [(->> [1 2 3]
                                       rx/seq->o
                                       (rx/map #(* % %)))
                                   next]
                               :d (rx/return "just one")
                               :e "just a value")
          result (rx-blocking/single o)]
      (is (= {:a [1 2 3]
              :a2 101
              :b "hi"
              :c [4 9]
              :d "just one"
              :e "just a value" }
             result)))))

(deftest test-realized-map
  (testing "works like realized-map, but takes a map instead of key/value pairs"
    (is (= {:a [1 2]
            :b 500 }
           (->> {:a (r/all (rx/seq->o [1 2]))
                 :b 500 }
                r/realized-map*
                rx-blocking/single)))))

(deftest test-let-realized
  (is (= {:a* 2
          :b* 500
          :c* 1000 }
         (->> (r/let-realized [a [(rx/seq->o [1 2]) last]
                               b 500
                               c (rx/return 1000) ]
                                 {:a* a
                                  :b* b
                                  :c* c })
              rx-blocking/single))))

(deftest test-only
  (testing "raises IllegalStateException if sequence is empty"
    (is (thrown-with-msg? IllegalStateException #"did not produce"
                          (->> (r/let-realized [a (rx/seq->o [1 2])]
                                               {:a a})
                               rx-blocking/single)))
    ; Just to be sure, make sure it goes through onError.
    (let [values (atom [])
          errors (atom [])]
      (rx/subscribe (r/let-realized [a (rx/seq->o [1 2])]
                              {:a a})
                  #(swap! values conj %)
                  #(swap! errors conj %))
      (is (empty? @values))
      (is (= 1 (count @errors)))
      (let [[e] @errors]
        (is (instance? IllegalStateException e))))))

(deftest test-all
  (testing "collects all values from an observable"
    (is (= [1 2 3]
           (->> (r/let-realized [a (r/all (rx/seq->o [1 2 3]))]
                           a)
                rx-blocking/single)))))

; Playing with some expressing some of the video stuff with this.
(comment
  (->> (get-list-of-lists user-id)
       (rx/mapcat (fn [list]
                    (->> (video-list->videos list)
                         (rx/take 10))))
       (rx/mapcat (fn [video]
                    (->> (r/let-realized [md (video->metadata video)
                                             bm (video->bookmark video)
                                             rt (video->rating video user-id)]
                            {:id       (:id video)
                             :title    (:title md)
                             :length   (:duration md)
                             :bookmark bm
                             :rating   {:actual    (:actual-star-rating rt)
                                        :average   (:average-star-rating rt)
                                        :predicted (:predicted-star-rating rt) } })))))

  (->> (get-list-of-lists user-id)
       (rx/mapcat (fn [list]
                    (->> (video-list->videos list)
                         (rx/take 10))))
       (rx/mapcat (fn [video]
                 (->> (r/realized-map :md (video->metadata video)
                                         :bm (video->bookmark video)
                                         :rt (video->rating video user-id))
                      (rx/map (fn [{:keys [md bm rt]}]
                             {:id       (:id video)
                              :title    (:title md)
                              :length   (:duration md)
                              :bookmark bm
                              :rating   {:actual (:actual-star-rating rt)
                                         :average (:average-star-rating rt)
                                         :predicted (:predicted-star-rating rt) } }))))))

  (->> (get-list-of-lists user-id)
       (rx/mapcat (fn [list]
                    (->> (video-list->videos list)
                         (rx/take 10))))
       (rx/mapcat (fn [video]
                    (->> (r/realized-map :id (:id video)
                                            :md [(video->metadata video)
                                                 first
                                                 #(select-keys % [:title :duration])]
                                            :bookmark (video->bookmark video)
                                            :rating [(video->rating video user-id)
                                                     first
                                                     #(hash-map :actual (:actual-star-rating %)
                                                                :average (:average-star-rating %)
                                                                :predicted (:predicted-star-rating %))])
                         (rx/map (fn [m]
                                   (-> m
                                       (merge (:md m))
                                       (dissoc :md)))))))))

