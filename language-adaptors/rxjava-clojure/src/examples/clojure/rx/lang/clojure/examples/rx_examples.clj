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
(ns rx.lang.clojure.examples.rx-examples
  (:require [rx.lang.clojure.interop :as rx])
  (:import rx.Observable rx.subscriptions.Subscriptions))

; NOTE on naming conventions. I'm using camelCase names (against clojure convention)
; in this file as I'm purposefully keeping functions and methods across
; different language implementations in-sync for easy comparison.

; --------------------------------------------------
; Hello World!
; --------------------------------------------------

(defn hello
  [& args]
  (->
    ; type hint required due to `Observable/from` overloading
    (Observable/from ^java.lang.Iterable args)
    (.subscribe (rx/action [v] (println (str "Hello " v "!"))))))

; To see output
(comment
  (hello "Ben" "George"))

; --------------------------------------------------
; Create Observable from Existing Data
; --------------------------------------------------


(defn existingDataFromNumbersUsingFrom []
  (Observable/from [1 2 3 4 5 6]))

(defn existingDataFromObjectsUsingFrom []
  (Observable/from ["a" "b" "c"]))

(defn existingDataFromListUsingFrom []
  (let [list [5, 6, 7, 8]]
    (Observable/from list)))

(defn existingDataWithJust []
  (Observable/just "one object"))

; --------------------------------------------------
; Custom Observable
; --------------------------------------------------

(defn customObservable
  "This example shows a custom Observable. Note the
  .isUnsubscribed check so that it can be stopped early.

  returns Observable<String>"
  []
  (Observable/create
    (rx/action [^rx.Subscriber s]
      (loop [x (range 50)]
        (when (and (not (.isUnsubscribed s)) x)
          ; TODO
          (println "HERE " (.isUnsubscribed s) (first x))
          (-> s (.onNext (str "value_" (first x))))
          (recur (next x))))
      ; after sending all values we complete the sequence
      (-> s .onCompleted))))

; To see output
(comment
  (.subscribe (customObservable) (rx/action* println)))

; --------------------------------------------------
; Composition - Simple
; --------------------------------------------------

(defn simpleComposition
  "Calls 'customObservable' and defines
   a chain of operators to apply to the callback sequence."
  []
  (->
    (customObservable)
    (.skip 10)
    (.take 5)
    (.map (rx/fn [v] (str v "_transformed")))
    (.subscribe (rx/action [v] (println "onNext =>" v)))))

; To see output
(comment
  (simpleComposition))


; --------------------------------------------------
; Composition - Multiple async calls combined
; --------------------------------------------------

(defn getUser
  "Asynchronously fetch user data

  return Observable<Map>"
  [userId]
  (Observable/create
    (rx/action [^rx.Subscriber s]
      (let [f (future
                (try
                  ; simulate fetching user data via network service call with latency
                  (Thread/sleep 60)
                  (-> s (.onNext {:user-id userId
                                  :name "Sam Harris"
                                  :preferred-language (if (= 0 (rand-int 2)) "en-us" "es-us") }))
                  (-> s .onCompleted)
                  (catch Exception e
                    (-> s (.onError e))))) ]
        ; a subscription that cancels the future if unsubscribed
        (.add s (Subscriptions/create (rx/action [] (future-cancel f))))))))

(defn getVideoBookmark
  "Asynchronously fetch bookmark for video

  return Observable<Integer>"
  [userId, videoId]
  (Observable/create
    (rx/action [^rx.Subscriber s]
      (let [f (future
                (try
                  ; simulate fetching user data via network service call with latency
                  (Thread/sleep 20)
                  (-> s (.onNext {:video-id videoId
                                  ; 50/50 chance of giving back position 0 or 0-2500
                                  :position (if (= 0 (rand-int 2)) 0 (rand-int 2500))}))
                  (-> s .onCompleted)
                  (catch Exception e
                    (-> s (.onError e)))))]
        ; a subscription that cancels the future if unsubscribed
        (.add s (Subscriptions/create (rx/action [] (future-cancel f))))))))

(defn getVideoMetadata
  "Asynchronously fetch movie metadata for a given language
  return Observable<Map>"
  [videoId, preferredLanguage]
  (Observable/create
    (rx/action [^rx.Subscriber s]
      (let [f (future
                (println "getVideoMetadata " videoId)
                (try
                  ; simulate fetching video data via network service call with latency
                  (Thread/sleep 50)
                  ; contrived metadata for en-us or es-us
                  (if (= "en-us" preferredLanguage)
                    (-> s (.onNext {:video-id videoId
                                           :title "House of Cards: Episode 1"
                                           :director "David Fincher"
                                           :duration 3365})))
                  (if (= "es-us" preferredLanguage)
                    (-> s (.onNext {:video-id videoId
                                           :title "Cámara de Tarjetas: Episodio 1"
                                           :director "David Fincher"
                                           :duration 3365})))
                  (-> s .onCompleted)
                  (catch Exception e
                    (-> s (.onError e))))) ]
        ; a subscription that cancels the future if unsubscribed
        (.add s (Subscriptions/create (rx/action [] (future-cancel f))))))))


(defn getVideoForUser [userId videoId]
  "Get video metadata for a given userId
  - video metadata
  - video bookmark position
  - user data
  return Observable<Map>"
  (let [user-observable           (-> (getUser userId)
                                      (.map (rx/fn [user]
                                              {:user-name (:name user)
                                               :language (:preferred-language user)})))
        bookmark-observable       (-> (getVideoBookmark userId videoId)
                                    (.map (rx/fn [bookmark] {:viewed-position (:position bookmark)})))
        ; getVideoMetadata requires :language from user-observable so nest inside map function
        video-metadata-observable (-> user-observable
                                    (.mapMany
                                      ; fetch metadata after a response from user-observable is received
                                      (rx/fn [user-map]
                                        (getVideoMetadata videoId (:language user-map)))))]
    ; now combine 3 async sequences using zip
    (-> (Observable/zip bookmark-observable video-metadata-observable user-observable
                        (rx/fn [bookmark-map metadata-map user-map]
                          {:bookmark-map bookmark-map
                           :metadata-map metadata-map
                           :user-map user-map}))
      ; and transform into a single response object
      (.map (rx/fn [data]
              {:video-id       videoId
               :video-metadata (:metadata-map data)
               :user-id        userId
               :language       (:language (:user-map data))
               :bookmark       (:viewed-position (:bookmark-map data)) })))))

; To see output like this:
;    {:video-id 78965, :video-metadata {:video-id 78965, :title Cámara de Tarjetas: Episodio 1,
;      :director David Fincher, :duration 3365}, :user-id 12345, :language es-us, :bookmark 0}
;
(comment
  (-> (getVideoForUser 12345 78965)
      (.toBlockingObservable)
      .single))

