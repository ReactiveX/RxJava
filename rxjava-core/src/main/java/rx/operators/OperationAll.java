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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Returns an Observable that emits a Boolean that indicates whether all items emitted by an
 * Observable satisfy a condition.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
 */
public class OperationAll {

    public static <T> OnSubscribeFunc<Boolean> all(Observable<? extends T> sequence, Func1<? super T, Boolean> predicate) {
        return new AllObservable<T>(sequence, predicate);
    }

    private static class AllObservable<T> implements OnSubscribeFunc<Boolean> {
        private final Observable<? extends T> sequence;
        private final Func1<? super T, Boolean> predicate;

        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        private AllObservable(Observable<? extends T> sequence, Func1<? super T, Boolean> predicate) {
            this.sequence = sequence;
            this.predicate = predicate;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Boolean> observer) {
            return subscription.wrap(sequence.subscribe(new AllObserver(observer)));

        }

        private class AllObserver implements Observer<T> {
            private final Observer<? super Boolean> underlying;

            private final AtomicBoolean status = new AtomicBoolean(true);

            public AllObserver(Observer<? super Boolean> underlying) {
                this.underlying = underlying;
            }

            @Override
            public void onCompleted() {
                if (status.get()) {
                    underlying.onNext(true);
                    underlying.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                underlying.onError(e);
            }

            @Override
            public void onNext(T args) {
                boolean result = predicate.call(args);
                boolean changed = status.compareAndSet(true, result);

                if (changed && !result) {
                    underlying.onNext(false);
                    underlying.onCompleted();
                    subscription.unsubscribe();
                }
            }
        }

    }
}
