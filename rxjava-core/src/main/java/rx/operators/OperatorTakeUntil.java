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
import rx.Subscriber;
import rx.functions.Func1;

import static rx.Observable.Operator;

/**
 * Returns an Observable that emits the items from the source Observable until another Observable
 * emits an item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
 */
public final class OperatorTakeUntil {

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
        Observable<Object> s = source.lift(new SourceObservable<T>());
        Observable<Object> o = other.lift(new OtherObservable<E>());

        Observable<Object> result = Observable.merge(s, o);

        final NotificationLite<T> notification = NotificationLite.instance();

        return result.takeWhile(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object args) {
                return !notification.isCompleted(args);
            }
        }).map(new Func1<Object, T>() {
            @Override
            public T call(Object args) {
                return notification.getValue(args);
            }
        });
    }

    private final static class SourceObservable<T> implements Operator<Object, T> {

        private final NotificationLite<T> notification = NotificationLite.instance();

        @Override
        public Subscriber<? super T> call(final Subscriber<? super Object> subscriber) {
            return new Subscriber<T>(subscriber) {
                @Override
                public void onCompleted() {
                    subscriber.onNext(notification.completed());
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(T args) {
                    subscriber.onNext(notification.next(args));
                }
            };
        }
    }

    private final static class OtherObservable<E> implements Operator<Object, E> {

        private final NotificationLite<E> notification = NotificationLite.instance();

        @Override
        public Subscriber<? super E> call(final Subscriber<? super Object> subscriber) {
            return new Subscriber<E>(subscriber) {
                @Override
                public void onCompleted() {
                    subscriber.onNext(notification.completed());
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onError(e);
                }

                @Override
                public void onNext(E args) {
                    subscriber.onNext(notification.completed());
                }
            };
        }
    }
}
