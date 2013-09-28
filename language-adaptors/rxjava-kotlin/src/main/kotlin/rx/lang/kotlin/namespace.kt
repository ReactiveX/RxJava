package rx.lang.kotlin

import rx.Subscription
import rx.Observer
import rx.Observable

public fun<T> Function1<Observer<in T>, Subscription>.asObservable(): Observable<T> {
    return Observable.create { this(it!!) }!!
}

public fun<T> Function0<Observable<out T>>.defer(): Observable<T> {
    return Observable.defer(this)!!
}

public fun<T> Iterable<T>.asObservable(): Observable<T> {
    return Observable.from(this)!!
}

public fun<T> T.asObservable(): Observable<T> {
    return Observable.from(this)!!
}

public fun<T> Throwable.asObservable(): Observable<T> {
    return Observable.error(this)!!
}

public fun<T> Pair<T, T>.asObservable(): Observable<T> {
    return Observable.from(this.component1(), this.component2())!!
}

public fun<T> Triple<T, T, T>.asObservable(): Observable<T> {
    return Observable.from(this.component1(), this.component2(), this.component3())!!
}

public fun<T> Pair<Observable<T>, Observable<T>>.merge(): Observable<T> {
    return Observable.merge(this.component1(), this.component2())!!
}

public fun<T> Triple<Observable<T>, Observable<T>, Observable<T>>.merge(): Observable<T> {
    return Observable.merge(this.component1(), this.component2(), this.component3())!!
}

public fun<T> Pair<Observable<T>, Observable<T>>.mergeDelayError(): Observable<T> {
    return Observable.mergeDelayError(this.component1(), this.component2())!!
}

public fun<T> Triple<Observable<T>, Observable<T>, Observable<T>>.mergeDelayError(): Observable<T> {
    return Observable.mergeDelayError(this.component1(), this.component2(), this.component3())!!
}
