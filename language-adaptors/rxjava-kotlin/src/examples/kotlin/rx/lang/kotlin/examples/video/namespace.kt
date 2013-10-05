package rx.lang.kotlin.examples.video

import rx.Observable

import rx.Observer
import rx.Subscription
import rx.lang.kotlin.asObservable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadPoolExecutor
import rx.subscriptions.BooleanSubscription
import java.util.HashMap

fun main(args: Array<String>) {
    getVideoGridForDisplay(1).subscribe({ videoDictionary ->
        println(videoDictionary)
    }, { exception ->
        println("Error:$exception")
    }) {
        executor.shutdownNow()
    }
}

val executor = ThreadPoolExecutor(4, 4, 1, TimeUnit.MINUTES, LinkedBlockingQueue<Runnable>())

fun getListOfLists(userId: Int): Observable<VideoList> {
    return {(observer: Observer<in VideoList>) ->
        val subscription = BooleanSubscription()
        try{
            executor.execute {
                Thread.sleep(180)
                (0..15).forEach {(i: Int):Unit ->
                    if (subscription.isUnsubscribed()) {
                        return@forEach
                    }
                    try{
                        observer.onNext(VideoList(i))
                    } catch (e: Exception){
                        observer.onError(e)
                    }
                }
            }
        } catch (e: Exception){
            observer.onError(e)
        }
        subscription
    }.asObservable()
}

fun getVideoGridForDisplay(userId: Int): Observable<Map<String, Any?>> {
    return getListOfLists(userId).take(5)!!.mapMany { list ->
        list!!.videos.take(10)!!.mapMany { video ->
            val m = video!!.metadata.map { md ->
                mapOf("title" to md!!["title"], "lenght" to md["duration"])
            }
            val b = video.getBookmark(userId).map { position ->
                mapOf("bookmark" to position)
            }
            val r = video.getRating(userId).map { rating ->
                mapOf("rating" to mapOf(
                        "actual" to rating!!.actualStarRating,
                        "average" to rating.averageStarRating,
                        "predicted" to rating.predictedStarRating
                ))
            }

            Observable.zip(m, b, r) { metadata, bookmark, rating ->
                val map: HashMap<String, Any?> = hashMapOf("id" to video.videoId)
                map.putAll(metadata!!)
                map.putAll(bookmark!!)
                map.putAll(rating!!)
                map as Map<String, Any?>
            }
        }
    }!!
}

class Video(val videoId: Int){
    val metadata: Observable<Map<String, String>>
        get(){
            return {(observer: Observer<in Map<String, String>>) ->
                observer.onNext(mapOf("title" to "video-$videoId-title", "duration" to "5428"))
                Subscription { }
            }.asObservable()
        }

    fun getBookmark(userId: Int): Observable<Int> {
        return {(observer: Observer<in Int>) ->
            executor.execute {
                Thread.sleep(4)
                if(randInt(6) > 1){
                    observer.onNext(randInt(0))
                } else {
                    observer.onNext(randInt(4000))
                }
                observer.onCompleted()
            }
            Subscription { }
        }.asObservable()
    }

    fun getRating(userId: Int): Observable<VideoRating> {
        return {(observer: Observer<in VideoRating>) ->
            executor.execute {
                Thread.sleep(10)
                observer.onNext(VideoRating(videoId, userId))
                observer.onCompleted()
            }
            Subscription { }
        }.asObservable()
    }
}

class VideoRating(val videoId: Int, val userId: Int){
    val predictedStarRating: Int
        get(){
            return randInt(5)
        }

    val averageStarRating: Int
        get(){
            return randInt(4)
        }

    val actualStarRating: Int
        get(){
            return randInt(5)
        }
}

class VideoList(val position: Int){
    val listName: String
        get(){
            return "ListName-$position"
        }

    val videos: Observable<Video>
        get(){
            return {(observer: Observer<in Video>) ->
                (0..50).forEach { i ->
                    observer.onNext(Video((1000 * position) + i))
                }
                observer.onCompleted()
                Subscription { }
            }.asObservable()
        }

}

fun randInt(max: Int): Int {
    return Math.round(Math.random() * max).toInt()
}