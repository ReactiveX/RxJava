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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnGetSubscriptionFunc;
import rx.Observable.OnPartialSubscribeFunc;
import rx.Observable.OnPartialUnsubscribeFunc;
import rx.Observable.OnSubscribeFunc;
import rx.Observable.PartialSubscription;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Applies a function of your choosing to every item emitted by an Observable, and returns this
 * transformation as a new Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/map.png">
 */
public final class OperationMap {

    /**
     * Accepts a sequence and a transformation function. Returns a sequence that is the result of
     * applying the transformation function to each item in the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in
     *         the input sequence.
     */
    public static <T, R> OnGetSubscriptionFunc<R> map(final Observable<? extends T> sequence, final Func1<? super T, ? extends R> func) {
        return mapWithIndex(sequence, new Func2<T, Integer, R>() {
            @Override
            public R call(T value, @SuppressWarnings("unused") Integer unused) {
                return func.call(value);
            }
        });
    }

    /**
     * Accepts a sequence and a transformation function. Returns a sequence that is the result of
     * applying the transformation function to each item in the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence. The function gets the index of
     *            the emitted item
     *            as additional parameter.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in
     *         the input sequence.
     */
    public static <T, R> OnGetSubscriptionFunc<R> mapWithIndex(final Observable<? extends T> sequence, final Func2<? super T, Integer, ? extends R> func) {
        return new OnGetSubscriptionFunc<R>() {
            @Override
            public PartialSubscription<R> onGetSubscription() {
                return new MapObservable<T, R>(sequence, func).onGetSubscription();
            }
        };
    }

    /**
     * Accepts a sequence of observable sequences and a transformation function. Returns a flattened
     * sequence that is the result of
     * applying the transformation function to each item in the sequence of each observable
     * sequence.
     * <p>
     * The closure should return an Observable which will then be merged.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in
     *         the input sequence.
     */
    public static <T, R> OnSubscribeFunc<R> mapMany(Observable<? extends T> sequence, Func1<? super T, ? extends Observable<? extends R>> func) {
        return OperationMerge.merge(Observable.create(map(sequence, func)));
    }

    /**
     * An observable sequence that is the result of applying a transformation to each item in an
     * input sequence.
     * 
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     */
    private static class MapObservable<T, R> implements OnGetSubscriptionFunc<R> {
        public MapObservable(Observable<? extends T> sequence, Func2<? super T, Integer, ? extends R> func) {
            this.sequence = sequence;
            this.func = func;
        }

        private final Observable<? extends T> sequence;
        private final Func2<? super T, Integer, ? extends R> func;
        private int index;

        @Override
        public PartialSubscription<R> onGetSubscription() {
            final AtomicReference<PartialSubscription<? extends T>> subscription = new AtomicReference<PartialSubscription<? extends T>>();
            return PartialSubscription.create(new OnPartialSubscribeFunc<R>() {

                @Override
                public void onSubscribe(final Observer<? super R> observer) {
                    subscription.set(sequence.getSubscription());
                    subscription.get().subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T value) {
                            observer.onNext(func.call(value, index));
                            index++;
                        }

                        @Override
                        public void onError(Throwable ex) {
                            observer.onError(ex);
                        }

                        @Override
                        public void onCompleted() {
                            observer.onCompleted();
                        }
                    });
                }
            }, new OnPartialUnsubscribeFunc() {

                @Override
                public void onUnsubscribe() {
                    subscription.get().unsubscribe();
                }
            });
        }
    }
}
