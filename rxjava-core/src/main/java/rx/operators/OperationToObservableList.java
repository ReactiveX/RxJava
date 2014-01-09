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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.IObservable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Returns an Observable that emits a single item, a list composed of all the items emitted by the
 * source Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/toList.png">
 * <p>
 * Normally, an Observable that returns multiple items will do so by invoking its Observer's
 * <code>onNext</code> method for each such item. You can change this behavior, instructing the
 * Observable to compose a list of all of these multiple items and then to invoke the Observer's
 * <code>onNext</code> method once, passing it the entire list, by using the toList operator.
 * <p>
 * Be careful not to use this operator on Observables that emit infinite or very large numbers of
 * items, as you do not have the option to unsubscribe.
 */
public final class OperationToObservableList<T> {

    public static <T> OnSubscribeFunc<List<T>> toObservableList(IObservable<? extends T> that) {
        return new ToObservableList<T>(that);
    }

    private static class ToObservableList<T> implements OnSubscribeFunc<List<T>> {

        private final IObservable<? extends T> that;

        public ToObservableList(IObservable<? extends T> that) {
            this.that = that;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super List<T>> observer) {

            return that.subscribe(new Observer<T>() {
                final List<T> list = new ArrayList<T>();

                @Override
                public void onNext(T value) {
                    list.add(value);
                }

                @Override
                public void onError(Throwable ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    try {
                        // benjchristensen => I want to make this list immutable but some clients are sorting this
                        // instead of using toSortedList() and this change breaks them until we migrate their code.
                        // observer.onNext(Collections.unmodifiableList(l));
                        observer.onNext(new ArrayList<T>(list));
                        observer.onCompleted();
                    } catch (Throwable e) {
                        onError(e);
                    }

                }
            });
        }
    }
}
