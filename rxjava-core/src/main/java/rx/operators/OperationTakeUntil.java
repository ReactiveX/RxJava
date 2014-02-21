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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Returns an Observable that emits the items from the source Observable until another Observable
 * emits an item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
 */
public class OperationTakeUntil {

    /**
     * Returns the values from the source observable sequence until the other observable sequence produces a value.
     * 
     * @param source
     *            the source sequence to propagate elements for.
     * @param other
     *            the observable sequence that terminates propagation of elements of the source sequence.
     * @param <T>
     *            the type of source.
     * @param <E>
     *            the other type.
     * @return An observable sequence containing the elements of the source sequence up to the point the other sequence interrupted further propagation.
     */
    public static <T, E> Observable<T> takeUntil(final Observable<? extends T> source, final Observable<? extends E> other) {
        Observable<Notification<T>> s = Observable.create(new SourceObservable<T>(source));
        Observable<Notification<T>> o = Observable.create(new OtherObservable<T, E>(other));

        Observable<Notification<T>> result = Observable.merge(s, o);

        return result.takeWhile(new Func1<Notification<T>, Boolean>() {
            @Override
            public Boolean call(Notification<T> notification) {
                return !notification.halt;
            }
        }).map(new Func1<Notification<T>, T>() {
            @Override
            public T call(Notification<T> notification) {
                return notification.value;
            }
        });
    }

    private static class Notification<T> {
        private final boolean halt;
        private final T value;

        public static <T> Notification<T> value(T value) {
            return new Notification<T>(false, value);
        }

        public static <T> Notification<T> halt() {
            return new Notification<T>(true, null);
        }

        private Notification(boolean halt, T value) {
            this.halt = halt;
            this.value = value;
        }

    }

    private static class SourceObservable<T> implements OnSubscribeFunc<Notification<T>> {
        private final Observable<? extends T> sequence;

        private SourceObservable(Observable<? extends T> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Notification<T>> notificationObserver) {
            return sequence.subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    notificationObserver.onNext(Notification.<T> halt());
                }

                @Override
                public void onError(Throwable e) {
                    notificationObserver.onError(e);
                }

                @Override
                public void onNext(T args) {
                    notificationObserver.onNext(Notification.value(args));
                }
            });
        }
    }

    private static class OtherObservable<T, E> implements OnSubscribeFunc<Notification<T>> {
        private final Observable<? extends E> sequence;

        private OtherObservable(Observable<? extends E> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Notification<T>> notificationObserver) {
            return sequence.subscribe(new Observer<E>() {
                @Override
                public void onCompleted() {
                    notificationObserver.onNext(Notification.<T> halt());
                }

                @Override
                public void onError(Throwable e) {
                    notificationObserver.onError(e);
                }

                @Override
                public void onNext(E args) {
                    notificationObserver.onNext(Notification.<T> halt());
                }
            });
        }
    }
}
