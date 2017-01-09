/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;

public class ObservableTakeUntilTest {

    @Test
    public void testTakeUntil() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = TestHelper.mockObserver();
        Observable<String> stringObservable = Observable.unsafeCreate(source)
                .takeUntil(Observable.unsafeCreate(other));
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
        verify(sSource, times(1)).dispose();
        verify(sOther, times(1)).dispose();

    }

    @Test
    public void testTakeUntilSourceCompleted() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = TestHelper.mockObserver();
        Observable<String> stringObservable = Observable.unsafeCreate(source).takeUntil(Observable.unsafeCreate(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        source.sendOnCompleted();

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(sSource, times(1)).dispose();
        verify(sOther, times(1)).dispose();

    }

    @Test
    public void testTakeUntilSourceError() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);
        Throwable error = new Throwable();

        Observer<String> result = TestHelper.mockObserver();
        Observable<String> stringObservable = Observable.unsafeCreate(source).takeUntil(Observable.unsafeCreate(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        source.sendOnError(error);
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onError(error);
        verify(sSource, times(1)).dispose();
        verify(sOther, times(1)).dispose();

    }

    @Test
    public void testTakeUntilOtherError() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);
        Throwable error = new Throwable();

        Observer<String> result = TestHelper.mockObserver();
        Observable<String> stringObservable = Observable.unsafeCreate(source).takeUntil(Observable.unsafeCreate(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        other.sendOnError(error);
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onError(error);
        verify(result, times(0)).onComplete();
        verify(sSource, times(1)).dispose();
        verify(sOther, times(1)).dispose();

    }

    /**
     * If the 'other' onCompletes then we unsubscribe from the source and onComplete.
     */
    @Test
    public void testTakeUntilOtherCompleted() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        Observer<String> result = TestHelper.mockObserver();
        Observable<String> stringObservable = Observable.unsafeCreate(source).takeUntil(Observable.unsafeCreate(other));
        stringObservable.subscribe(result);
        source.sendOnNext("one");
        source.sendOnNext("two");
        other.sendOnCompleted();
        source.sendOnNext("three");

        verify(result, times(1)).onNext("one");
        verify(result, times(1)).onNext("two");
        verify(result, times(0)).onNext("three");
        verify(result, times(1)).onComplete();
        verify(sSource, times(1)).dispose();
        verify(sOther, times(1)).dispose(); // unsubscribed since SafeSubscriber unsubscribes after onComplete

    }

    private static class TestObservable implements ObservableSource<String> {

        Observer<? super String> observer;
        Disposable s;

        TestObservable(Disposable s) {
            this.s = s;
        }

        /* used to simulate subscription */
        public void sendOnCompleted() {
            observer.onComplete();
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
        public void subscribe(Observer<? super String> observer) {
            this.observer = observer;
            observer.onSubscribe(s);
        }
    }

    @Test
    public void testUntilFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.takeUntil(until).subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        ts.assertValue(1);
        until.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }
    @Test
    public void testMainCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.takeUntil(until).subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);
        source.onComplete();

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }
    @Test
    public void testDownstreamUnsubscribes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.takeUntil(until).take(1).subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();

        assertFalse("Source still has observers", source.hasObservers());
        assertFalse("Until still has observers", until.hasObservers());
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().takeUntil(Observable.never()));
    }
}
