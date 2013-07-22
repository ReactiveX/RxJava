(ns rx.lang.clojure.examples.video-example
  (:import [rx Observable Observer Subscription] rx.subscriptions.Subscriptions))

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
    (.subscribe #(locking print-lock (println %))
                #(locking print-lock (println "Error: " %))
                #(do
                   (println "Finished example 1")
                   (on-complete)))))

(defn example2
  [on-complete]
  (println "Starting example 2")
  ; onNext will be called once with a list and demonstrates how a sequence can be combined
  ; for document style responses (most webservices)
  (-> (get-video-grid-for-display 1)
    .toList
    (.subscribe #(println "\n ---- single list of video dictionaries ----\n" %)
                #(println "Error: " %)
                #(do
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
    (.mapMany (fn [list]
                ; for each VideoList we want to fetch the videos
                (-> (video-list->videos list)
                  (.take 10) ; we only want the first 10 of each list
                  (.mapMany (fn [video]
                              ; for each video we want to fetch metadata
                              (let [m (-> (video->metadata video)
                                          (.map (fn [md]
                                                  ; transform to the data and format we want
                                                  {:title  (:title md)
                                                  :length (:duration md) })))
                                    b (-> (video->bookmark video user-id)
                                          (.map (fn [position]
                                                  {:bookmark position})))
                                    r (-> (video->rating video user-id)
                                          (.map (fn [rating]
                                                  {:rating {:actual    (:actual-star-rating rating)
                                                            :average   (:average-star-rating rating)
                                                            :predicted (:predicted-star-rating rating) }})))]
                                ; join these together into a single, merged map for each video
                                (Observable/zip m b r (fn [m b r]
                                                        (merge {:id video} m b r)))))))))))


; A little helper to make the future-based observables a little less verbose
; this has possibilities ...
(defn- ^Observable future-observable
  "Returns an observable that executes (f observer) in a future, returning a
  subscription that will cancel the future."
  [f]
  (Observable/create (fn [^Observer observer]
                       (let [f (future (f observer))]
                         (Subscriptions/create #(future-cancel f))))))

(defn ^Observable get-list-of-lists
  "
  Retrieve a list of lists of videos (grid).

  Observable<VideoList> is the \"push\" equivalent to List<VideoList>
  "
  [user-id]
  (future-observable (fn [^Observer observer]
                       (Thread/sleep 180)
                       (dotimes [i 15]
                         (.onNext observer (video-list i)))
                       (.onCompleted observer))))


(comment (.subscribe (get-list-of-lists 7777) println))

(defn video-list
  [position]
  {:position position
   :name     (str "ListName-" position) })

(defn ^Observable video-list->videos
  [{:keys [position] :as video-list}]
  (Observable/create (fn [^Observer observer]
                       (dotimes [i 50]
                         (.onNext observer (+ (* position 1000) i)))
                       (.onCompleted observer)
                       (Subscriptions/empty))))

(comment (.subscribe (video-list->videos (video-list 2)) println))

(defn ^Observable video->metadata
  [video-id]
  (Observable/create (fn [^Observer observer]
                       (.onNext observer {:title (str "video-" video-id "-title")
                                          :actors ["actor1" "actor2"]
                                          :duration 5428 })
                       (.onCompleted observer)
                       (Subscriptions/empty))))

(comment (.subscribe (video->metadata 10) println))

(defn ^Observable video->bookmark
  [video-id user-id]
  (future-observable (fn [^Observer observer]
                       (Thread/sleep 4)
                       (.onNext observer (if (> (rand-int 6) 1) 0 (rand-int 4000)))
                       (.onCompleted observer))))

(comment (.subscribe (video->bookmark 112345 99999) println))

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

(comment (.subscribe (video->rating 234345 8888) println))

