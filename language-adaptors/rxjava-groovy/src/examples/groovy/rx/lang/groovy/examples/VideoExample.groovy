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
import rx.subscriptions.BooleanSubscription;
import rx.functions.Func1;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class VideoExample {

static void main(String[] args) {
    VideoExample v = new VideoExample();
    println("---- sequence of video dictionaries ----")
    v.getVideoGridForDisplay(1).subscribe(
        { videoDictionary -> // onNext
            // this will print the dictionary for each video
            // and is a good representation of how progressive rendering could work
            println(videoDictionary) }, 
        { exception -> // onError
            println("Error: " + exception) }, 
        { // onCompleted
            v.executor.shutdownNow();
        });
    
    VideoExample v2 = new VideoExample();
    v2.getVideoGridForDisplay(1).toList().subscribe(
        { videoDictionaryList -> // onNext
            // this will be called once with a list
            // and demonstrates how a sequence can be combined
            // for document style responses (most webservices)
            println("\n ---- single list of video dictionaries ----\n" + videoDictionaryList) },
        { exception -> // onError
            println("Error: " + exception) },
        { // onCompleted
            v2.executor.shutdownNow();
        });
}
/**
 * Demonstrate how Rx is used to compose Observables together such as
 * how a web service would to generate a JSON response.
 * 
 * The simulated methods for the metadata represent different services
 * that are often backed by network calls.
 * 
 * This will return a sequence of dictionaries such as this:
 * 
 *  [id:1000, title:video-1000-title, length:5428, bookmark:0, 
 *      rating:[actual:4, average:3, predicted:0]]
 */
Observable getVideoGridForDisplay(userId) {
    // take the first 5 lists
    getListOfLists(userId).take(5).mapMany({ VideoList list ->
        // for each VideoList we want to fetch the videos
        list.getVideos()
            .take(10) // we only want the first 10 of each list
            .mapMany({ Video video -> 
                // for each video we want to fetch metadata
                def m = video.getMetadata().map({ Map<String, String> md -> 
                    // transform to the data and format we want
                    return [title: md.get("title"),
                            length: md.get("duration")]
                })
                def b = video.getBookmark(userId).map({ position -> 
                    return [bookmark: position]
                })
                def r = video.getRating(userId).map({ VideoRating rating -> 
                    return [rating: 
                        [actual: rating.getActualStarRating(),
                         average: rating.getAverageStarRating(),
                         predicted: rating.getPredictedStarRating()]]
                })
                // compose these together
                return Observable.zip(m, b, r, {
                        metadata, bookmark, rating -> 
                    // now transform to complete dictionary of data
                    // we want for each Video
                    return [id: video.videoId] << metadata << bookmark << rating
                })
            })   
    })
}

/**
 * Retrieve a list of lists of videos (grid).
 * 
 * Observable<VideoList> is the "push" equivalent to List<VideoList>
 */
Observable<VideoList> getListOfLists(userId) {
    return Observable.create({ observer -> 
        BooleanSubscription subscription = new BooleanSubscription();
        try {
            // this will happen on a separate thread as it requires a network call
            executor.execute({
                    // simulate network latency
                    Thread.sleep(180);
                    for(i in 0..15) {
                        if(subscription.isUnsubscribed()) {
                            break;
                        }
                        try {
                            //println("****** emitting list: " + i)
                            observer.onNext(new VideoList(i))
                        }catch(Exception e) {
                            observer.onError(e);
                        }
                    }
                    observer.onCompleted();
            })
        }catch(Exception e) {
            observer.onError(e);
        }
        return subscription;
    })
}

/**
 * Represents a list of videos as part of a grid (list of lists).  
 */
class VideoList {
    
    int listPosition;
    VideoList(int position) {
        this.listPosition = position
    }
    
    String getListName() {
        return "ListName-" + listPosition
    }
    
    Integer getListPosition() {
        return listPosition
    }
    
    Observable<Video> getVideos() {
        return Observable.create({ observer ->
            // we already have the videos once a list is loaded
            // so we won't launch another thread but return
            // the sequence of videos via push
            // NOTE: This will always execute all 50 even if take(2) asks for only 2
            //       as it performs synchronously and is not lazy.
            for(i in 0..50) {
                //println("emitting video: " + i)
                observer.onNext(new Video((listPosition*1000)+i))
            }
            observer.onCompleted();
        })
    }
}

class Video {
    int videoId;
    Video(int videoId) {
        this.videoId = videoId;
    }
    
    // synchronous
    Observable<Map<String, String>> getMetadata() {
        // simulate fetching metadata from an in-memory cache
        // so it will not asynchronously execute on a thread but
        // immediately return an Observable with the data
        return Observable.create({ observer ->
            observer.onNext([
                title: "video-" + videoId + "-title", 
                actors: ["actor1", "actor2"],
                duration: 5428])
            observer.onCompleted();
        });
    }
    
    // asynchronous
    Observable<Integer> getBookmark(userId) {
        // simulate fetching the bookmark for this user
        // that specifies the last played position if
        // this video has been played before
        return Observable.create({ observer -> 
            // this will happen on a separate thread as it requires a network call
            executor.execute({
                    // simulate network latency
                    Thread.sleep(4);
                    if(randint(6) > 1) {
                        // most of the time they haven't watched a movie
                        // so the position is 0
                        observer.onNext(randint(0));
                    } else {
                        observer.onNext(randint(4000));
                    }
                    observer.onCompleted();
            })
        })
    }
    
    // asynchronous
    Observable<VideoRating> getRating(userId) {
        // simulate fetching the VideoRating for this user
        return Observable.create({ observer ->
            // this will happen on a separate thread as it requires a network call
            executor.execute({
                    // simulate network latency
                    Thread.sleep(10);
                    observer.onNext(new VideoRating(videoId, userId))
                    observer.onCompleted();
            })
        })
    }
}

class VideoRating {
    int videoId, userId
    VideoRating(videoId, userId) {
        this.videoId = videoId;
        this.userId = userId;
    }
    
    Integer getPredictedStarRating() {
        return randint(5)
    }
    
    Integer getAverageStarRating() {
        return randint(4)
    }
    
    Integer getActualStarRating() {
        return randint(5)
    }
}

ExecutorService executor = new ThreadPoolExecutor(4, 4, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

def randint(int max) {
    return Math.round(Math.random() * max)
}

def combine( Map... m ) {
    m.collectMany { it.entrySet() }.inject( [:] ) { result, e ->
      result << [ (e.key):e.value + ( result[ e.key ] ?: 0 ) ]
    }
  }

}
