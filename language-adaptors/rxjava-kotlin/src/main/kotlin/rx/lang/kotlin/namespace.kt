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
