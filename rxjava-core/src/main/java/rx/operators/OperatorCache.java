/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.observers.Subscribers;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * This method has similar behavior to {@link Observable#replay()} except that this auto-subscribes
 * to the source Observable rather than returning a connectable Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
 * <p>
 * This is useful with an Observable that you want to cache responses when you can't control the
 * subscribe/unsubscribe behavior of all the Observers.
 * <p>
 * NOTE: You sacrifice the ability to unsubscribe from the origin when you use this operator, so be
 * careful not to use this operator on Observables that emit infinite or very large numbers of
 * items, as this will use up memory.
 * 
 * @param <T>
 *            the cached value type
 */
public final class OperatorCache<T> implements OnSubscribe<T> {
    protected final Observable<? extends T> source;
    protected final Subject<? super T, ? extends T> cache;
    protected final AtomicBoolean sourceSubscribed;

    public OperatorCache(Observable<? extends T> source) {
        this(source, ReplaySubject.<T> create());
    }

    /* accessible to tests */OperatorCache(Observable<? extends T> source, Subject<? super T, ? extends T> cache) {
        this.source = source;
        this.cache = cache;
        this.sourceSubscribed = new AtomicBoolean();
    }

    @Override
    public void call(Subscriber<? super T> s) {
        if (sourceSubscribed.compareAndSet(false, true)) {
            source.unsafeSubscribe(Subscribers.from(cache));
            /*
             * Note that we will never unsubscribe from 'source' as we want to receive and cache all of its values.
             * 
             * This means this should never be used on an infinite or very large sequence, similar to toList().
             */
        }
        cache.unsafeSubscribe(s);
    }
}