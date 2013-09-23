package rx.lang.kotlin

import rx.Subscription
import rx.Observer
import rx.Observable

public fun<T> Function1<Observer<in T>, Subscription>.asObservable(): Observable<T> {
    return Observable.create{ this(it!!) }!!
}

public fun<T> Iterable<T>.asObservable(): Observable<T> {
    return Observable.from(this)!!
}

public fun<T> T.asObservable(): Observable<T> {
    return Observable.from(this)!!
}
