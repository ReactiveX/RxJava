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
package rx.internal.operators;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;

import java.util.NoSuchElementException;

/**
 * Allows conversion of an Observable to a Single ensuring that exactly one item is emitted - no more and no less.
 * Also forwards errors as appropriate.
 * @param <T> the value type
 */
public class OnSubscribeSingle<T> implements Single.OnSubscribe<T> {

    private final Observable<T> observable;

    public OnSubscribeSingle(Observable<T> observable) {
        this.observable = observable;
    }

    @Override
    public void call(final SingleSubscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {
            private boolean emittedTooMany = false;
            private boolean itemEmitted = false;
            private T emission = null;

            @Override
            public void onStart() {
                // We request 2 here since we need 1 for the single and 1 to check that the observable
                // doesn't emit more than one item
                request(2);
            }

            @Override
            public void onCompleted() {
                if (emittedTooMany) {
                    // Don't need to do anything here since we already sent an error downstream
                } else {
                    if (itemEmitted) {
                        child.onSuccess(emission);
                    } else {
                        child.onError(new NoSuchElementException("Observable emitted no items"));
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
                unsubscribe();
            }

            @Override
            public void onNext(T t) {
                if (itemEmitted) {
                    emittedTooMany = true;
                    child.onError(new IllegalArgumentException("Observable emitted too many elements"));
                    unsubscribe();
                } else {
                    itemEmitted = true;
                    emission = t;
                }
            }
        };
        child.add(parent);
        observable.unsafeSubscribe(parent);
    }

    public static <T> OnSubscribeSingle<T> create(Observable<T> observable) {
        return new OnSubscribeSingle<T>(observable);
    }
}
