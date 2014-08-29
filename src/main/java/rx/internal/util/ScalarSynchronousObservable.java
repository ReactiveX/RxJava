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
package rx.internal.util;

import rx.Observable;
import rx.Subscriber;

public final class ScalarSynchronousObservable<T> extends Observable<T> {

    public static final <T> ScalarSynchronousObservable<T> create(T t) {
        return new ScalarSynchronousObservable<T>(t);
    }

    private final T t;

    protected ScalarSynchronousObservable(final T t) {
        super(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> s) {
                /*
                 *  We don't check isUnsubscribed as it is a significant performance impact in the fast-path use cases.
                 *  See PerfBaseline tests and https://github.com/ReactiveX/RxJava/issues/1383 for more information.
                 *  The assumption here is that when asking for a single item we should emit it and not concern ourselves with 
                 *  being unsubscribed already. If the Subscriber unsubscribes at 0, they shouldn't have subscribed, or it will 
                 *  filter it out (such as take(0)). This prevents us from paying the price on every subscription. 
                 */
                s.onNext(t);
                s.onCompleted();
            }

        });
        this.t = t;
    }

    public T get() {
        return t;
    }

}
