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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorTakeUntilTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testTakeUntil() {
        Subscription sSource = mock(Subscription.class);
        Subscription sOther = mock(Subscription.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = mock(Observer.class);
        Observable<String> stringObservable = Observable.create(source).takeUntil(Observable.create(other));
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
        Observable<String> stringObservable = Observable.create(source).takeUntil(Observable.create(other));
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
        Observable<String> stringObservable = Observable.create(source).takeUntil(Observable.create(other));
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
        Observable<String> stringObservable = Observable.create(source).takeUntil(Observable.create(other));
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
        Observable<String> stringObservable = Observable.create(source).takeUntil(Observable.create(other));
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
    
    @Test
    public void testUntilFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        until.onNext(1);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        
        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        assertFalse("TestSubscriber is unsubscribed", ts.isUnsubscribed());
    }
    @Test
    public void testMainCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);
        source.onCompleted();
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        
        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        assertFalse("TestSubscriber is unsubscribed", ts.isUnsubscribed());
    }
    @Test
    public void testDownstreamUnsubscribes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.takeUntil(until).take(1).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.assertNoErrors();
        ts.assertTerminalEvent();
        
        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        assertFalse("TestSubscriber is unsubscribed", ts.isUnsubscribed());
    }
    public void testBackpressure() {
        PublishSubject<Integer> until = PublishSubject.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        Observable.range(1, 10).takeUntil(until).unsafeSubscribe(ts);

        assertTrue(until.hasObservers());

        ts.requestMore(1);
        
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.assertNoErrors();
        assertTrue("TestSubscriber completed", ts.getOnCompletedEvents().isEmpty());
        
        assertFalse("Until still has observers", until.hasObservers());
        assertFalse("TestSubscriber is unsubscribed", ts.isUnsubscribed());
    }
}
