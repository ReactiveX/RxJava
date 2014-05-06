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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.operators.OperatorTakeUntil.takeUntil;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

public class OperatorTakeUntilTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntil() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = takeUntil(Observable.create(source), Observable.create(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        other.sendOnNext("three");
        source.sendOnNext("four");
        source.sendOnCompleted();
        other.sendOnCompleted();

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(0)).onNext("four");
        verify(sSource, times(1)).unsubscribe();
        verify(sOther, times(1)).unsubscribe();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntilSourceCompleted() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = takeUntil(Observable.create(source), Observable.create(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        source.sendOnCompleted();

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(sSource, times(1)).unsubscribe();
        verify(sOther, times(1)).unsubscribe();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntilSourceError() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);
        Throwable error = new Throwable();

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = takeUntil(Observable.create(source), Observable.create(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        source.sendOnError(error);
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onError(error);
        verify(sSource, times(1)).unsubscribe();
        verify(sOther, times(1)).unsubscribe();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntilOtherError() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);
        Throwable error = new Throwable();

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = takeUntil(Observable.create(source), Observable.create(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        other.sendOnError(error);
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onError(error);
        verify(result, times(0)).onCompleted();
        verify(sSource, times(1)).unsubscribe();
        verify(sOther, times(1)).unsubscribe();

    }

    /**
     * If the 'other' onCompletes then we unsubscribe from the source and onComplete
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntilOtherCompleted() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = takeUntil(Observable.create(source), Observable.create(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        other.sendOnCompleted();
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onCompleted();
        verify(sSource, times(1)).unsubscribe();
        verify(sOther, times(1)).unsubscribe(); // unsubscribed since SafeSubscriber unsubscribes after onComplete

    }

    private static class TestObservable implements Observable.OnSubscribe<String> {

        Observer<? super String> observer = null;
        Subscription s;

        public TestObservable(Subscription s) {
            this.s = s;
        }

        /* used to simulate subscription */
        public void sendOnCompleted() {
            observer.onCompleted();
        }

        /* used to simulate subscription */
        public void sendOnNext(String value) {
            observer.onNext(value);
        }

        /* used to simulate subscription */
        public void sendOnError(Throwable e) {
            observer.onError(e);
        }

        @Override
        public void call(Subscriber<? super String> observer) {
            this.observer = observer;
            observer.add(s);
        }
    }
}
