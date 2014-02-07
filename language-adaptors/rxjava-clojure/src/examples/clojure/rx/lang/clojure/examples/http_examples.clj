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
(ns rx.lang.clojure.examples.http-examples
  (:require [rx.lang.clojure.interop :as rx]
            [clj-http.client :as http])
  (:import rx.Observable rx.subscriptions.Subscriptions))

; NOTE on naming conventions. I'm using camelCase names (against clojure convention)
; in this file as I'm purposefully keeping functions and methods across
; different language implementations in-sync for easy comparison.

(defn fetchWikipediaArticleAsynchronously [wikipediaArticleNames]
  "Fetch a list of Wikipedia articles asynchronously.

   return Observable<String> of HTML"
  (Observable/create
    (rx/action [observer]
      (let [f (future
                (doseq [articleName wikipediaArticleNames]
                  (-> observer (.onNext (http/get (str "http://en.wikipedia.org/wiki/" articleName)))))
                ; after sending response to onnext we complete the sequence
                (-> observer .onCompleted))]
        ; a subscription that cancels the future if unsubscribed
        (.add observer (Subscriptions/create (rx/action [] (future-cancel f))))))))

; To see output
(comment
  (-> (fetchWikipediaArticleAsynchronously ["Tiger" "Elephant"])
    (.subscribe (rx/action [v] (println "--- Article ---\n" (subs (:body v) 0 125) "...")))))


; --------------------------------------------------
; Error Handling
; --------------------------------------------------

(defn fetchWikipediaArticleAsynchronouslyWithErrorHandling [wikipediaArticleNames]
  "Fetch a list of Wikipedia articles asynchronously
   with proper error handling.

   return Observable<String> of HTML"
  (Observable/create
    (rx/action [observer]
      (let [f (future
                (try
                  (doseq [articleName wikipediaArticleNames]
                    (-> observer (.onNext (http/get (str "http://en.wikipedia.org/wiki/" articleName)))))
                  ;(catch Exception e (prn "exception")))
                  (catch Exception e (-> observer (.onError e))))
                ; after sending response to onNext we complete the sequence
                (-> observer .onCompleted))]
        ; a subscription that cancels the future if unsubscribed
        (.add observer (Subscriptions/create (rx/action [] (future-cancel f))))))))

; To see output
(comment
  (-> (fetchWikipediaArticleAsynchronouslyWithErrorHandling ["Tiger" "NonExistentTitle" "Elephant"])
    (.subscribe (rx/action [v] (println "--- Article ---\n" (subs (:body v) 0 125) "..."))
                (rx/action [e] (println "--- Error ---\n" (.getMessage e))))))


