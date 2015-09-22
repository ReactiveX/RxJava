/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorTakeUntilTest {

    @Test
    public void testTakeUntil() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        NbpSubscriber<String> result = TestHelper.mockNbpSubscriber();
        NbpObservable<String> stringObservable = NbpObservable.create(source)
                .takeUntil(NbpObservable.create(other));
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

        NbpSubscriber<String> result = TestHelper.mockNbpSubscriber();
        NbpObservable<String> stringObservable = NbpObservable.create(source).takeUntil(NbpObservable.create(other));
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

        NbpSubscriber<String> result = TestHelper.mockNbpSubscriber();
        NbpObservable<String> stringObservable = NbpObservable.create(source).takeUntil(NbpObservable.create(other));
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

        NbpSubscriber<String> result = TestHelper.mockNbpSubscriber();
        NbpObservable<String> stringObservable = NbpObservable.create(source).takeUntil(NbpObservable.create(other));
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
     * If the 'other' onCompletes then we unsubscribe from the source and onComplete
     */
    @Test
    public void testTakeUntilOtherCompleted() {
        Disposable sSource = mock(Disposable.class);
        Disposable sOther = mock(Disposable.class);
        TestObservable source = new TestObservable(sSource);
        TestObservable other = new TestObservable(sOther);

        NbpSubscriber<String> result = TestHelper.mockNbpSubscriber();
        NbpObservable<String> stringObservable = NbpObservable.create(source).takeUntil(NbpObservable.create(other));
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

    private static class TestObservable implements NbpOnSubscribe<String> {

        NbpSubscriber<? super String> NbpObserver;
        Disposable s;

        public TestObservable(Disposable s) {
            this.s = s;
        }

        /* used to simulate subscription */
        public void sendOnCompleted() {
            NbpObserver.onComplete();
        }

        /* used to simulate subscription */
        public void sendOnNext(String value) {
            NbpObserver.onNext(value);
        }

        /* used to simulate subscription */
        public void sendOnError(Throwable e) {
            NbpObserver.onError(e);
        }

        @Override
        public void accept(NbpSubscriber<? super String> NbpObserver) {
            this.NbpObserver = NbpObserver;
            NbpObserver.onSubscribe(s);
        }
    }
    
    @Test
    public void testUntilFires() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> until = NbpPublishSubject.create();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(until.hasSubscribers());

        source.onNext(1);
        
        ts.assertValue(1);
        until.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();
        
        assertFalse("Source still has observers", source.hasSubscribers());
        assertFalse("Until still has observers", until.hasSubscribers());
        assertFalse("NbpTestSubscriber is unsubscribed", ts.isCancelled());
    }
    @Test
    public void testMainCompletes() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> until = NbpPublishSubject.create();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(until.hasSubscribers());

        source.onNext(1);
        source.onComplete();
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();
        
        assertFalse("Source still has observers", source.hasSubscribers());
        assertFalse("Until still has observers", until.hasSubscribers());
        assertFalse("NbpTestSubscriber is unsubscribed", ts.isCancelled());
    }
    @Test
    public void testDownstreamUnsubscribes() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> until = NbpPublishSubject.create();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.takeUntil(until).take(1).unsafeSubscribe(ts);

        assertTrue(source.hasSubscribers());
        assertTrue(until.hasSubscribers());

        source.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminated();
        
        assertFalse("Source still has observers", source.hasSubscribers());
        assertFalse("Until still has observers", until.hasSubscribers());
        assertFalse("NbpTestSubscriber is unsubscribed", ts.isCancelled());
    }
}