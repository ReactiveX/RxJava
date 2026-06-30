/*
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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.*;

public class ObservableTakeUntilTest extends RxJavaTest {

    @Test
    public void takeUntil() {
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
    public void takeUntilSourceCompleted() {
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
        verify(sSource, never()).dispose(); // no longer disposing itself on terminal events
        verify(sOther, times(1)).dispose();

    }

    @Test
    public void takeUntilSourceError() {
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
        verify(sSource, never()).dispose(); // no longer disposing itself on terminal events
        verify(sOther, times(1)).dispose();

    }

    @Test
    public void takeUntilOtherError() {
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
        verify(sOther, never()).dispose(); // no longer disposing itself on termination

    }

    /**
     * If the 'other' onCompletes then we unsubscribe from the source and onComplete.
     */
    @Test
    public void takeUntilOtherCompleted() {
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
        verify(sOther, never()).dispose(); // no longer disposing itself on terminal events

    }

    private static class TestObservable implements ObservableSource<String> {

        Observer<? super String> observer;
        Disposable upstream;

        TestObservable(Disposable d) {
            this.upstream = d;
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
            observer.onSubscribe(upstream);
        }
    }

    @Test
    public void untilFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserverEx<Integer> to = new TestObserverEx<>();

        source.takeUntil(until).subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        to.assertValue(1);
        until.onNext(1);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertTerminated();

        assertFalse(source.hasObservers(), "Source still has observers");
        assertFalse(until.hasObservers(), "Until still has observers");
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }

    @Test
    public void mainCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserverEx<Integer> to = new TestObserverEx<>();

        source.takeUntil(until).subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);
        source.onComplete();

        to.assertValue(1);
        to.assertNoErrors();
        to.assertTerminated();

        assertFalse(source.hasObservers(), "Source still has observers");
        assertFalse(until.hasObservers(), "Until still has observers");
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }

    @Test
    public void downstreamUnsubscribes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestObserverEx<Integer> to = new TestObserverEx<>();

        source.takeUntil(until).take(1).subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        to.assertValue(1);
        to.assertNoErrors();
        to.assertTerminated();

        assertFalse(source.hasObservers(), "Source still has observers");
        assertFalse(until.hasObservers(), "Until still has observers");
        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().takeUntil(Observable.never()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable((Function<Observable<Integer>, Observable<Integer>>) o -> o.takeUntil(Observable.never()));
    }

    @Test
    public void untilPublisherMainSuccess() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        main.onNext(1);
        main.onNext(2);
        main.onComplete();

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertResult(1, 2);
    }

    @Test
    public void untilPublisherMainComplete() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        main.onComplete();

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertResult();
    }

    @Test
    public void untilPublisherMainError() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        main.onError(new TestException());

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertFailure(TestException.class);
    }

    @Test
    public void untilPublisherOtherOnNext() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        other.onNext(1);

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertResult();
    }

    @Test
    public void untilPublisherOtherOnComplete() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        other.onComplete();

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertResult();
    }

    @Test
    public void untilPublisherOtherError() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        other.onError(new TestException());

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertFailure(TestException.class);
    }

    @Test
    public void untilPublisherDispose() {
        PublishSubject<Integer> main = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        TestObserver<Integer> to = main.takeUntil(other).test();

        assertTrue(main.hasObservers(), "Main no observers?");
        assertTrue(other.hasObservers(), "Other no observers?");

        to.dispose();

        assertFalse(main.hasObservers(), "Main has observers?");
        assertFalse(other.hasObservers(), "Other has observers?");

        to.assertEmpty();
    }

}
