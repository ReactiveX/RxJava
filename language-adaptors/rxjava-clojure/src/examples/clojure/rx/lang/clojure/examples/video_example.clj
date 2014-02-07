;
; Copyright 2013 Netflix, Inc.
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
(ns rx.lang.clojure.examples.video-example
  (:require [rx.lang.clojure.interop :as rx])
  (:import [rx Observable Observer Subscription]
           rx.subscriptions.Subscriptions))

; Adapted from language-adaptors/rxjava-groovy/src/examples/groovy/rx/lang/groovy/examples/VideoExample.groovy

(declare get-video-grid-for-display)
(declare get-list-of-lists)
(declare video-list)
(declare video-list->videos)
(declare video->metadata)
(declare video->bookmark)
(declare video->rating)

; just use a simple lock to keep multi-threaded output from being interleaved
(def print-lock (Object.))

(defn example1
  [on-complete]
  (println "Starting example 1")
  ; this will print the dictionary for each video and is a good representation of
  ; how progressive rendering could work
  (println "---- sequence of video dictionaries ----")
  (-> (get-video-grid-for-display 1)
    (.subscribe (rx/action [v] (locking print-lock (println v)))
                (rx/action [v] (locking print-lock (println "Error: " v)))
                (rx/action []
                  (println "Finished example 1")
                  (on-complete)))))

(defn example2
  [on-complete]
  (println "Starting example 2")
  ; onNext will be called once with a list and demonstrates how a sequence can be combined
  ; for document style responses (most webservices)
  (-> (get-video-grid-for-display 1)
    .toList
    (.subscribe (rx/action [v] (println "\n ---- single list of video dictionaries ----\n" v))
                (rx/action [v] (println "Error: " v))
                (rx/action []
                   (println "Finished Example 2")
                   (println "Exiting")
                   (on-complete)))))

(defn -main
  [& args]
  ; Run example1 followed by example2, then exit
  (example1 (fn [] (example2 #(System/exit 0)))))

(defn ^Observable get-video-grid-for-display
  "
  Demonstrate how Rx is used to compose Observables together such as
  how a web service would to generate a JSON response.

  The simulated methods for the metadata represent different services
  that are often backed by network calls.

  This will return a sequence of maps like this:

   {:id 1000, :title video-1000-title, :length 5428, :bookmark 0,
    :rating {:actual 4 :average 3 :predicted 0}}
  "
  [user-id]
  (-> (get-list-of-lists user-id)
    (.mapMany (rx/fn [list]
                ; for each VideoList we want to fetch the videos
                (-> (video-list->videos list)
                  (.take 10) ; we only want the first 10 of each list
                  (.mapMany (rx/fn [video]
                              ; for each video we want to fetch metadata
                              (let [m (-> (video->metadata video)
                                          (.map (rx/fn [md]
                                                  ; transform to the data and format we want
                                                  {:title  (:title md)
                                                  :length (:duration md) })))
                                    b (-> (video->bookmark video user-id)
                                          (.map (rx/fn [position]
                                                  {:bookmark position})))
                                    r (-> (video->rating video user-id)
                                          (.map (rx/fn [rating]
                                                  {:rating {:actual    (:actual-star-rating rating)
                                                            :average   (:average-star-rating rating)
                                                            :predicted (:predicted-star-rating rating) }})))]
                                ; join these together into a single, merged map for each video
                                (Observable/zip m b r (rx/fn [m b r]
                                                        (merge {:id video} m b r)))))))))))


; A little helper to make the future-based observables a little less verbose
; this has possibilities ...
(defn- ^Observable future-observable
  "Returns an observable that executes (f observer) in a future, returning a
  subscription that will cancel the future."
  [f]
  (Observable/create (rx/action [^rx.Subscriber s]
                       (println "Starting f")
                       (let [f (future (f s))]
                         (.add s (Subscriptions/create (rx/action [] (future-cancel f))))))))

(defn ^Observable get-list-of-lists
  "
  Retrieve a list of lists of videos (grid).

  Observable<VideoList> is the \"push\" equivalent to List<VideoList>
  "
  [user-id]
  (future-observable (fn [^rx.Subscriber s]
                       (Thread/sleep 180)
                       (dotimes [i 15]
                         (.onNext s (video-list i)))
                       (.onCompleted s))))


(comment (-> (get-list-of-lists 7777)
             .toList
             .toBlockingObservable
             .single))

(defn video-list
  [position]
  {:position position
   :name     (str "ListName-" position) })

(defn ^Observable video-list->videos
  [{:keys [position] :as video-list}]
  (Observable/create (rx/action [^rx.Subscriber s]
                       (dotimes [i 50]
                         (.onNext s (+ (* position 1000) i)))
                       (.onCompleted s))))

(comment (-> (video-list->videos (video-list 2))
             .toList
             .toBlockingObservable
             .single))

(defn ^Observable video->metadata
  [video-id]
  (Observable/create (rx/action [^rx.Subscriber s]
                                (.onNext s {:title (str "video-" video-id "-title")
                                            :actors ["actor1" "actor2"]
                                            :duration 5428 })
                                (.onCompleted s))))

(comment (-> (video->metadata 10)
             .toList
             .toBlockingObservable
             .single))

(defn ^Observable video->bookmark
  [video-id user-id]
  (future-observable (fn [^Observer observer]
                       (Thread/sleep 4)
                       (println "onNext")
                       (.onNext observer (if (> (rand-int 6) 1) 0 (rand-int 4000)))
                       (println "onComplete")
                       (.onCompleted observer))))

(comment (-> (video->bookmark 112345 99999)
            .toList
            .toBlockingObservable
            .single))

(defn ^Observable video->rating
  [video-id user-id]
  (future-observable (fn [^Observer observer]
                       (Thread/sleep 10)
                       (.onNext observer {:video-id              video-id
                                          :user-id               user-id
                                          :predicted-star-rating (rand-int 5)
                                          :average-star-rating   (rand-int 5)
                                          :actual-star-rating    (rand-int 5) })
                       (.onCompleted observer))))

(comment (-> (video->rating 234345 8888)
             .toList
             .toBlockingObservable
             .single))

