(ns rx.examples.clojure.rx-examples
  (import rx.Observable)
  (:require [clj-http.client :as http]))

; NOTE on naming conventions. I'm using camelCase names (against clojure convention)
; in this file as I'm purposefully keeping functions and methods across
; different language implementations in-sync for easy comparison.
;
; See rx.examples.groovy.RxExamples, rx.examples.java.RxExamples, etc.

; --------------------------------------------------
; Hello World!
; --------------------------------------------------

(defn hello
  [&rest]
  (-> (Observable/toObservable &rest)
    (.subscribe #(println (str "Hello " % "!")))))

; To see output
#_(hello ["Ben" "George"])

; --------------------------------------------------
; Create Observable from Existing Data
; --------------------------------------------------
 
(defn existingDataFromNumbers []
    (Observable/toObservable [1 2 3 4 5 6]))

(defn existingDataFromNumbersUsingFrom []
    (Observable/from [1 2 3 4 5 6]))

(defn existingDataFromObjects []
    (Observable/toObservable ["a" "b" "c"]))
    
(defn existingDataFromObjectsUsingFrom []
    (Observable/from ["a" "b" "c"]))

(defn existingDataFromList []
    (let [list [5, 6, 7, 8]]
      (Observable/toObservable list)))
    
(defn existingDataFromListUsingFrom []
  (let [list [5, 6, 7, 8]]
    (Observable/from list)))

(defn existingDataWithJust []
    (Observable/just "one object"))

; --------------------------------------------------
; Custom Observable
; --------------------------------------------------

(defn customObservableBlocking []
  "This example shows a custom Observable that blocks 
   when subscribed to (does not spawn an extra thread).
   
  returns Observable<String>"
  (Observable/create 
    (fn [observer]
      (doseq [x (range 50)] (-> observer (.onNext (str "value_" x))))
      ; after sending all values we complete the sequence
      (-> observer .onCompleted)
      ; return a NoOpSubsription since this blocks and thus
      ; can't be unsubscribed from
      (Observable/noOpSubscription))))

; To see output
#_(.subscribe (customObservableBlocking) #(println %))

(defn customObservableNonBlocking []
  "This example shows a custom Observable that does not block 
   when subscribed to as it spawns a separate thread.
   
  returns Observable<String>"
  (Observable/create 
    (fn [observer]
      (let [f (future 
                (doseq [x (range 50)] (-> observer (.onNext (str "anotherValue_" x))))
                ; after sending all values we complete the sequence
                (-> observer .onCompleted))
            ; a subscription that cancels the future if unsubscribed
            subscription (Observable/createSubscription #(-> f (.cancel true)))]
        ))
      ))

; To see output
#_(.subscribe (customObservableNonBlocking) #(println %))


(defn fetchWikipediaArticleAsynchronously [wikipediaArticleNames]
  "Fetch a list of Wikipedia articles asynchronously.
  
   return Observable<String> of HTML"
  (Observable/create 
    (fn [observer]
      (let [f (future
                (doseq [articleName wikipediaArticleNames]
                  (-> observer (.onNext (http/get (str "http://en.wikipedia.org/wiki/" articleName)))))
                ; after sending response to onnext we complete the sequence
                (-> observer .onCompleted))
            ; a subscription that cancels the future if unsubscribed
            subscription (Observable/createSubscription #(-> f (.cancel true)))]
        ))))

; To see output
#_(-> (fetchWikipediaArticleAsynchronously ["Tiger" "Elephant"]) 
  (.subscribe #(println "--- Article ---\n" (subs (:body %) 0 125) "...")))


; --------------------------------------------------
; Composition - Simple
; --------------------------------------------------

(defn simpleComposition []
  "Asynchronously calls 'customObservableNonBlocking' and defines
   a chain of operators to apply to the callback sequence."
  (-> 
    (customObservableNonBlocking)
    (.skip 10)
    (.take 5)
    (.map #(str % "_transformed"))
    (.subscribe #(println "onNext =>" %))))

; To see output
#_(simpleComposition)


; --------------------------------------------------
; Composition - Multiple async calls combined
; --------------------------------------------------

(defn getUser [userId]
  "Asynchronously fetch user data
  return Observable<Map>"
  (Observable/create 
      (fn [observer]
        (let [f (future
          (try 
            ; simulate fetching user data via network service call with latency
            (Thread/sleep 60)
            (-> observer (.onNext {:user-id userId :name "Sam Harris" 
                                   :preferred-language (if (= 0 (rand-int 2)) "en-us" "es-us") }))    
            (-> observer .onCompleted)
          (catch Exception e (-> observer (.onError e)))))
          ; a subscription that cancels the future if unsubscribed
          subscription (Observable/createSubscription #(-> f (.cancel true)))]
      ))))

(defn getVideoBookmark [userId, videoId]
  "Asynchronously fetch bookmark for video
  return Observable<Integer>"
  (Observable/create 
      (fn [observer]
        (let [f (future
          (try 
            ; simulate fetching user data via network service call with latency
            (Thread/sleep 20)
            (-> observer (.onNext {:video-id videoId
                           ; 50/50 chance of giving back position 0 or 0-2500
                           :position (if (= 0 (rand-int 2)) 0 (rand-int 2500))}))
            (-> observer .onCompleted)
          (catch Exception e (-> observer (.onError e)))))
          ; a subscription that cancels the future if unsubscribed
          subscription (Observable/createSubscription #(-> f (.cancel true)))]
      ))))

(defn getVideoMetadata [videoId, preferredLanguage]
  "Asynchronously fetch movie metadata for a given language
  return Observable<Map>"
  (Observable/create 
      (fn [observer]
        (let [f (future
          (try 
            ; simulate fetching video data via network service call with latency
            (Thread/sleep 50)
            ; contrived metadata for en-us or es-us
            (if (= "en-us" preferredLanguage)
              (-> observer (.onNext {:video-id videoId :title "House of Cards: Episode 1" 
                                   :director "David Fincher" :duration 3365})))
            (if (= "es-us" preferredLanguage)
              (-> observer (.onNext {:video-id videoId :title "Cámara de Tarjetas: Episodio 1" 
                                   :director "David Fincher" :duration 3365})))
            (-> observer .onCompleted)
          (catch Exception e (-> observer (.onError e)))))
          ; a subscription that cancels the future if unsubscribed
          subscription (Observable/createSubscription #(-> f (.cancel true)))]
      ))))

  
(defn getVideoForUser [userId videoId]
  "Get video metadata for a given userId
   - video metadata
   - video bookmark position
   - user data
  return Observable<Map>"
    (let [user-observable (-> (getUser userId)
              (.map (fn [user] {:user-name (:name user) :language (:preferred-language user)})))
          bookmark-observable (-> (getVideoBookmark userId videoId)
              (.map (fn [bookmark] {:viewed-position (:position bookmark)})))
          ; getVideoMetadata requires :language from user-observable so nest inside map function
          video-metadata-observable (-> user-observable 
              (.mapMany
                ; fetch metadata after a response from user-observable is received
                (fn [user-map] 
                  (getVideoMetadata videoId (:language user-map)))))]
          ; now combine 3 async sequences using zip
          (-> (Observable/zip bookmark-observable video-metadata-observable user-observable 
                (fn [bookmark-map metadata-map user-map]
                  {:bookmark-map bookmark-map 
                  :metadata-map metadata-map
                  :user-map user-map}))
            ; and transform into a single response object
            (.map (fn [data]
                  {:video-id videoId
                   :video-metadata (:metadata-map data)
                   :user-id userId
                   :language (:language (:user-map data))
                   :bookmark (:viewed-position (:bookmark-map data))
                  })))))
  
; To see output like this:
;    {:video-id 78965, :video-metadata {:video-id 78965, :title Cámara de Tarjetas: Episodio 1, 
;      :director David Fincher, :duration 3365}, :user-id 12345, :language es-us, :bookmark 0}
;
#_(-> (getVideoForUser 12345 78965)
        (.subscribe 
          (fn [x] (println "--- Object ---\n" x))
          (fn [e] (println "--- Error ---\n" e))
          (fn [] (println "--- Completed ---"))))


; --------------------------------------------------
; Error Handling
; --------------------------------------------------

(defn fetchWikipediaArticleAsynchronouslyWithErrorHandling [wikipediaArticleNames]
  "Fetch a list of Wikipedia articles asynchronously
   with proper error handling.
  
   return Observable<String> of HTML"
  (Observable/create 
    (fn [observer]
      (let [f (future
                (try 
                (doseq [articleName wikipediaArticleNames]
                  (-> observer (.onNext (http/get (str "http://en.wikipedia.org/wiki/" articleName)))))
                ;(catch Exception e (prn "exception")))
                (catch Exception e (-> observer (.onError e))))
                ; after sending response to onnext we complete the sequence
                (-> observer .onCompleted))
            ; a subscription that cancels the future if unsubscribed
            subscription (Observable/createSubscription #(-> f (.cancel true)))]
        ))))

; To see output
#_(-> (fetchWikipediaArticleAsynchronouslyWithErrorHandling ["Tiger" "NonExistentTitle" "Elephant"]) 
  (.subscribe 
    #(println "--- Article ---\n" (subs (:body %) 0 125) "...")
    #(println "--- Error ---\n" (.getMessage %))))

  