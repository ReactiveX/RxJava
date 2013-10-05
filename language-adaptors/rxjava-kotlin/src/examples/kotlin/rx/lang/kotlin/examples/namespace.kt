package rx.lang.kotlin.examples

import rx.Observable
import rx.Observer
import rx.subscriptions.Subscriptions
import rx.lang.kotlin.asObservable
import kotlin.concurrent.thread
import rx.Subscription
import java.net.URL
import java.util.Scanner

/**
 * Created by IntelliJ IDEA.
 * @author Mario Arias
 * Date: 28/09/13
 * Time: 3:00
 */

fun main(args: Array<String>) {
    hello(array("Ben", "George"))
    customObservableNonBlocking().subscribe { println(it) }
    customObservableBlocking().subscribe { println(it) }
    val printArticle: (String?) -> Unit = {
        println("""--- Article ---
                    ${it!!.substring(0, 125)}
                    """)
    }
    fetchWikipediaArticleAsynchronously("Tiger", "Elephant").subscribe(printArticle)
    simpleComposition()

    fetchWikipediaArticleAsynchronouslyWithErrorHandling("Tiger", "NonExistentTitle", "Elephant").subscribe (printArticle) {
        println("""--- Error ---
                ${it!!.getMessage()}
                """)
    }
}

fun hello(names: Array<String>) {
    Observable.from(names)!!.subscribe { s -> println("Hello $s!") }
}

fun customObservableBlocking(): Observable<String> {
    return {(observer: Observer<in String>) ->
        (0..50).forEach { i ->
            observer.onNext("value_$i")
        }
        observer.onCompleted()
        Subscriptions.empty()!!
    }.asObservable()
}

fun customObservableNonBlocking(): Observable<String> {
    return {(observer: Observer<in String>) ->
        val t = thread {
            (0..50).forEach { i ->
                observer.onNext("anotherValue_$i")
            }
            observer.onCompleted()
        }
        Subscription {
            t.interrupt()
        }
    }.asObservable()
}

fun fetchWikipediaArticleAsynchronously(vararg wikipediaArticleNames: String): Observable<String> {
    return {(observer: Observer<in String>) ->
        thread {
            wikipediaArticleNames.forEach { article ->
                observer.onNext(URL("http://en.wikipedia.org/wiki/$article").getText())
            }
            observer.onCompleted()
        }
        Subscriptions.empty()!!
    }.asObservable()
}

fun simpleComposition() {
    customObservableNonBlocking()
            .skip(10)!!
            .take(5)!!
            .map { s -> "${s}_transformed" }!!
            .subscribe { println("onNext => $it") }
}

fun fetchWikipediaArticleAsynchronouslyWithErrorHandling(vararg wikipediaArticleNames: String): Observable<String> {
    return {(observer: Observer<in String>) ->
        thread {
            try {
                wikipediaArticleNames.forEach { article ->
                    observer.onNext(URL("http://en.wikipedia.org/wiki/$article").getText())
                }
                observer.onCompleted()
            } catch(e: Exception) {
                observer.onError(e)
            }
        }
        Subscriptions.empty()!!
    }.asObservable()
}


//Extensions
fun URL.getText(): String {
    val scanner = Scanner(this.openStream()!!)
    val sb = StringBuilder(1024)
    while(scanner.hasNextLine()){
        sb.append(scanner.nextLine())
        sb.append('\n')
    }
    return sb.toString()
}


