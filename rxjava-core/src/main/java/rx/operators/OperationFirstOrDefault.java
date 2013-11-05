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
package rx.operators;

import static rx.util.functions.Functions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Returns an Observable that emits the first item emitted by the source
 * Observable, or a default value if the source emits nothing.
 */
public final class OperationFirstOrDefault {

    /**
     * Returns an Observable that emits the first item emitted by the source
     * Observable that satisfies the given condition,
     * or a default value if the source emits no items that satisfy the given condition.
     * 
     * @param source
     *            The source Observable to emit the first item for.
     * @param predicate
     *            The condition the emitted source items have to satisfy.
     * @param defaultValue
     *            The default value to use whenever the source Observable doesn't emit anything.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> firstOrDefault(Observable<? extends T> source, Func1<? super T, Boolean> predicate, T defaultValue) {
        return new FirstOrElse<T>(source, predicate, defaultValue);
    }

    /**
     * Returns an Observable that emits the first item emitted by the source
     * Observable, or a default value if the source emits nothing.
     * 
     * @param source
     *            The source Observable to emit the first item for.
     * @param defaultValue
     *            The default value to use whenever the source Observable doesn't emit anything.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> firstOrDefault(Observable<? extends T> source, T defaultValue) {
        return new FirstOrElse<T>(source, alwaysTrue(), defaultValue);
    }

    private static class FirstOrElse<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Func1<? super T, Boolean> predicate;
        private final T defaultValue;

        private FirstOrElse(Observable<? extends T> source, Func1<? super T, Boolean> predicate, T defaultValue) {
            this.source = source;
            this.defaultValue = defaultValue;
            this.predicate = predicate;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final Subscription sourceSub = source.subscribe(new Observer<T>() {
                private final AtomicBoolean hasEmitted = new AtomicBoolean(false);

                @Override
                public void onCompleted() {
                    if (!hasEmitted.get()) {
                        observer.onNext(defaultValue);
                        observer.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(T next) {
                    try {
                        if (!hasEmitted.get() && predicate.call(next)) {
                            hasEmitted.set(true);
                            observer.onNext(next);
                            observer.onCompleted();
                        }
                    } catch (Throwable t) {
                        // may happen within the predicate call (user code)
                        observer.onError(t);
                    }
                }
            });

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    sourceSub.unsubscribe();
                }
            });
        }
    }
}
