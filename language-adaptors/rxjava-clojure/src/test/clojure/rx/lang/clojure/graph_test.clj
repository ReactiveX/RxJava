(ns rx.lang.clojure.graph-test
  (:require [rx.lang.clojure.graph :as graph]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.future :as rx-future]
            [rx.lang.clojure.blocking :as rx-blocking]
            [clojure.test :refer [deftest testing is]]))

(deftest test-let-o*
  (testing "throws on cycle"
    (is (thrown-with-msg? IllegalArgumentException #"Cycle found"
                          (graph/let-o* {:a {:deps [:a]}}))))

  (testing "throws on unknown"
    (is (thrown-with-msg? IllegalArgumentException #"Unknown node"
                          (graph/let-o* {:a {:deps [:b]}}))))

  (testing "it works in a simple case"
    (let [d {:a {:deps []
                 :factory (fn [_] (rx/seq->o [1 2 3 4 5]))}
             :b {:deps [:a]
                 :factory (fn [{:keys [a]}] (rx/map #(* % %) a)) }
             :c {:deps [:a :b]
                 :factory (fn [{:keys [a b]}] (rx/map #(+ %1 %2) a b)) }
             :d {:deps [:c :b]
                 :factory (fn [{:keys [c b]}] (rx/map #(+ %1 %2) c b)) }
             }
          f (graph/let-o* d) ]
      (println f)
      ; (n^2 + n) + n^2
      (is (= [3 10 21 36 55]
             (rx-blocking/into [] (:d f)))))))

(deftest test-let-o
  (testing "it works"
    (let [f (graph/let-o [?a (rx/seq->o [1 2 3])
                          ?b (rx/seq->o [4 5 6])]
              (rx/map + ?a ?b))]
      (is (= [5 7 9]
             (rx-blocking/into [] f)))))

  (testing "it still works"
    (is (= {:a 99 :b 100 :z "hi"}
           (rx-blocking/single
             (-> (let [z (rx/return "hi")] ; an observable from "somewhere else"
                   (graph/let-o
                     [?a (rx-future/future* future-call #(do (Thread/sleep 50) 99))
                      ?b (rx-future/future* future-call #(do (Thread/sleep 500) 100))
                      ?c (rx/map #(hash-map :a %1 :b %2 :z %3) ?a ?b ?z)
                      ?z z]
                     (rx/reduce merge {} ?c)))))))))

(deftest test-complicated-graph
  ; These funcs model network requests for various stuff. They all return observable.
  (let [request-vhs (fn []
                      (rx-future/future-generator*
                        future-call
                        (fn [o]
                          (Thread/sleep 50)
                          (doseq [i (range 3)]
                            (rx/on-next o {:id i})))))
        request-user (fn [id]
                       (rx-future/future*
                         future-call
                         #(do (Thread/sleep (rand-int 250))
                            {:id id
                             :name (str "friend" id) })))
        request-ab (fn [u]
                     (rx-future/future*
                       future-call
                       #(do (Thread/sleep (rand-int 250))
                          {:user-id (:id u)
                           :cell    (* 2 (:id u))})))

        request-video-md (fn [v]
                           (rx/return {:video v
                                       :title (str "title" (:id v)) }))

        ; Now we can stitch all these requests together into an rx graph to
        ; produce a response.
        o (graph/let-o [?user-info (rx-future/future*
                                     future-call
                                     #(do (Thread/sleep 20)
                                        {:name "Bob"
                                         :id 12345
                                         :friend-ids [1 2 3] }))

                        ?friends   (->> ?user-info
                                        (rx/mapcat (fn [ui]
                                                     (rx/mapcat request-user
                                                                (rx/seq->o (:friend-ids ui))))))

                        ?ab        (->> (rx/concat ?user-info ?friends)
                                        (rx/mapcat request-ab))

                        ?ab-lookup (->> ?ab
                                        (rx/map (juxt :user-id #(dissoc % :user-id)))
                                        (rx/into {}))

                        ?vhs       (request-vhs)


                        ?metadata  (->> ?vhs
                                        (rx/mapcat request-video-md))]
            (rx/map (fn [u m f ab-lookup]
                      {:user    (dissoc u :friend-ids)
                      :videos  m
                      :friends (sort-by :id f)
                      :ab      ab-lookup})
                    ?user-info
                    (rx/into [] ?metadata)
                    (rx/into [] ?friends)
                    ?ab-lookup))]

    (is (= {:user {:name "Bob" :id 12345}
            :videos [{:video {:id 0} :title "title0"}
                     {:video {:id 1} :title "title1"}
                     {:video {:id 2} :title "title2"}]
            :friends [{:name "friend1" :id 1}{:name "friend2" :id 2}{:name "friend3" :id 3}]
            :ab {12345 {:cell 24690} 1 {:cell 2} 2 {:cell 4} 3 {:cell 6}} }
           (rx-blocking/single o)))))


