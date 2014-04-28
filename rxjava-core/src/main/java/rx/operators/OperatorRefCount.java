/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.operators;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

/**
 * Returns an observable sequence that stays connected to the source as long
 * as there is at least one subscription to the observable sequence.
 * @param <T> the value type
 */
public final class OperatorRefCount<T> implements OnSubscribe<T> {
    final ConnectableObservable<? extends T> source;
    final Object guard;
    /** Guarded by guard. */
    int count;
    /** Guarded by guard. */
    Subscription connection;
    public OperatorRefCount(ConnectableObservable<? extends T> source) {
        this.source = source;
        this.guard = new Object();
    }

    @Override
    public void call(Subscriber<? super T> t1) {
        t1.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                synchronized (guard) {
                    if (--count == 0) {
                        connection.unsubscribe();
                        connection = null;
                    }
                }
            }
        }));
        source.unsafeSubscribe(t1);
        synchronized (guard) {
            if (count++ == 0) {
                connection = source.connect();
            }
        }
    }
}
