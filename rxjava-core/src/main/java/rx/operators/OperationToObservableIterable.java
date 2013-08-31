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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

/**
 * Converts an Iterable sequence into an Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/toObservable.png">
 * <p>
 * You can convert any object that supports the Iterable interface into an Observable that emits
 * each item in the object, with the toObservable operation.
 */
public final class OperationToObservableIterable<T> {

    public static <T> Func1<Observer<? super T>, Subscription> toObservableIterable(Iterable<T> list) {
        return new ToObservableIterable<T>(list);
    }

    private static class ToObservableIterable<T> implements Func1<Observer<? super T>, Subscription> {
        public ToObservableIterable(Iterable<T> list) {
            this.iterable = list;
        }

        public Iterable<T> iterable;

        public Subscription call(Observer<? super T> observer) {
            for (T item : iterable) {
                observer.onNext(item);
            }
            observer.onCompleted();

            return Subscriptions.empty();
        }
    }

    public static class UnitTest {

        @Test
        public void testIterable() {
            Observable<String> observable = Observable.create(toObservableIterable(Arrays.<String> asList("one", "two", "three")));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}
