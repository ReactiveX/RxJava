/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.groovy.examples;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.functions.Action0
import rx.functions.Func1;

// --------------------------------------------------
// Hello World!
// --------------------------------------------------

def hello(String[] names) {
    Observable.from(names)
        .subscribe({ println "Hello " + it + "!"})
}

hello("Ben", "George")


// --------------------------------------------------
// Create Observables from Existing Data
// --------------------------------------------------

def existingDataFromNumbers() {
    Observable<Integer> o = Observable.from(1, 2, 3, 4, 5, 6);
}

def existingDataFromNumbersUsingFrom() {
    Observable<Integer> o2 = Observable.from(1, 2, 3, 4, 5, 6);
}

def existingDataFromObjects() {
    Observable<String> o = Observable.from("a", "b", "c");
}

def existingDataFromObjectsUsingFrom() {
    Observable<String> o = Observable.from("a", "b", "c");
}

def existingDataFromList() {
    def list = [5, 6, 7, 8]
    Observable<Integer> o = Observable.from(list);
}

def existingDataFromListUsingFrom() {
    def list = [5, 6, 7, 8]
    Observable<Integer> o2 = Observable.from(list);
}

def existingDataWithJust() {
    Observable<String> o = Observable.just("one object");
}


// --------------------------------------------------
// Create Custom Observables
// --------------------------------------------------


/**
 * This example shows a custom Observable that blocks 
 * when subscribed to (does not spawn an extra thread).
 * 
 * @return Observable<String>
 */
def customObservableBlocking() {
    return Observable.create(new Func1<Observer<String>, Subscription>() {
        def Subscription call(Observer<String> observer) {
            for(int i=0; i<50; i++) {
                observer.onNext("value_" + i);
            }
            // after sending all values we complete the sequence
            observer.onCompleted();
            // return an empty subscription since this blocks and thus
            // can't be unsubscribed from
            return Subscriptions.empty();
        };
    });
}

// To see output:
customObservableBlocking().subscribe({ println(it)});

/**
 * This example shows a custom Observable that does not block
 * when subscribed to as it spawns a separate thread.
 *
 * @return Observable<String>
 */
def customObservableNonBlocking() {
    return Observable.create(new Func1<Observer<String>, Subscription>() {
        /**
         * This 'call' method will be invoked with the Observable is subscribed to.
         * 
         * It spawns a thread to do it asynchronously.
         */
        def Subscription call(Observer<String> observer) {
            // For simplicity this example uses a Thread instead of an ExecutorService/ThreadPool
            final Thread t = new Thread(new Runnable() {
                void run() {
                    for(int i=0; i<75; i++) {
                        observer.onNext("anotherValue_" + i);
                    }
                    // after sending all values we complete the sequence
                    observer.onCompleted();
                };
            });
            t.start();
        
            return Subscriptions.create(new Action0() {
                public void call() {
                    // Ask the thread to stop doing work.
                    // For this simple example it just interrupts.
                    t.interrupt();
                }
            });
        };
    });
}

// To see output:
customObservableNonBlocking().subscribe({ println(it)});


/**
 * Fetch a list of Wikipedia articles asynchronously.
 * 
 * @param wikipediaArticleName
 * @return Observable<String> of HTML
 */
def fetchWikipediaArticleAsynchronously(String... wikipediaArticleNames) {
    return Observable.create({ Observer<String> observer ->
        Thread.start {
            for(articleName in wikipediaArticleNames) {
                observer.onNext(new URL("http://en.wikipedia.org/wiki/"+articleName).getText());
            }
            observer.onCompleted();
        }
        return Subscriptions.empty();
    });
}

// To see output:
fetchWikipediaArticleAsynchronously("Tiger", "Elephant")
    .subscribe({ println "--- Article ---\n" + it.substring(0, 125)})


// --------------------------------------------------
// Composition
// --------------------------------------------------

/**
 * Asynchronously calls 'customObservableNonBlocking' and defines
 * a chain of operators to apply to the callback sequence.
 */
def simpleComposition() {
    customObservableNonBlocking()
        .skip(10)
        .take(5)
        .map({ stringValue -> return stringValue + "_transformed"})
        .subscribe({ println "onNext => " + it})
}
 
// To see output:
simpleComposition();

/*

(defn simpleComposition []
    "Asynchronously calls 'customObservableNonBlocking' and defines
     a chain of operators to apply to the callback sequence."
    (->
      (customObservableNonBlocking)
      (.skip 10)
      (.take 5)
      (.map #(do (str % "_transformed")))
      (.subscribe #(println "onNext =>" %))))
  */



// --------------------------------------------------
// Error Handling
// --------------------------------------------------



/**
 * Fetch a list of Wikipedia articles asynchronously with error handling.
 *
 * @param wikipediaArticleName
 * @return Observable<String> of HTML
 */
def fetchWikipediaArticleAsynchronouslyWithErrorHandling(String... wikipediaArticleNames) {
    return Observable.create({ Observer<String> observer ->
        Thread.start {
            try {
                for(articleName in wikipediaArticleNames) {
                    observer.onNext(new URL("http://en.wikipedia.org/wiki/"+articleName).getText());
                }
                observer.onCompleted();
            } catch(Exception e) {
                observer.onError(e);
            }
        }
            return Subscriptions.empty();
    });
}

fetchWikipediaArticleAsynchronouslyWithErrorHandling("Tiger", "NonExistentTitle", "Elephant")
    .subscribe(
        { println "--- Article ---\n" + it.substring(0, 125)}, 
        { println "--- Error ---\n" + it.getMessage()})
    
