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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Scheduler.Worker;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.*;
import rx.subjects.PublishSubject;

public class OperatorTakeLastTimedTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void testTakeLastTimedWithNegativeCount() {
        Observable.just("one").takeLast(-1, 1, TimeUnit.SECONDS);
    }

    @Test
    public void takeLastTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Object> source = PublishSubject.create();

        Observable<Object> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onCompleted(); // T: 1250ms

        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onNext(4);
        inOrder.verify(o, times(1)).onNext(5);
        inOrder.verify(o, times(1)).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedDelayCompletion() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Object> source = PublishSubject.create();

        Observable<Object> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(1250, TimeUnit.MILLISECONDS);
        source.onCompleted(); // T: 2250ms

        inOrder.verify(o, times(1)).onCompleted();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedWithCapacity() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Object> source = PublishSubject.create();

        Observable<Object> result = source.takeLast(2, 1, TimeUnit.SECONDS, scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onCompleted(); // T: 1250ms

        inOrder.verify(o, times(1)).onNext(4);
        inOrder.verify(o, times(1)).onNext(5);
        inOrder.verify(o, times(1)).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void takeLastTimedThrowingSource() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Object> source = PublishSubject.create();

        Observable<Object> result = source.takeLast(1, TimeUnit.SECONDS, scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onError(new TestException()); // T: 1250ms

        inOrder.verify(o, times(1)).onError(any(TestException.class));

        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void takeLastTimedWithZeroCapacity() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Object> source = PublishSubject.create();

        Observable<Object> result = source.takeLast(0, 1, TimeUnit.SECONDS, scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        source.onNext(1); // T: 0ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(2); // T: 250ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(3); // T: 500ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(4); // T: 750ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onNext(5); // T: 1000ms
        scheduler.advanceTimeBy(250, TimeUnit.MILLISECONDS);
        source.onCompleted(); // T: 1250ms

        inOrder.verify(o, times(1)).onCompleted();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test(timeout = 30000) // original could get into an infinite loop
    public void completionRequestRace() {
        Worker w = Schedulers.computation().createWorker();
        try {
            final int n = 1000;
            for (int i = 0; i < 25000; i++) {
                if (i % 1000 == 0) {
                    System.out.println("completionRequestRace >> " + i);
                }
                PublishSubject<Integer> ps = PublishSubject.create();
                final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
                
                ps.takeLast(n, 1, TimeUnit.DAYS).subscribe(ts);
                
                for (int j = 0; j < n; j++) {
                    ps.onNext(j);
                }

                final AtomicBoolean go = new AtomicBoolean();
                
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        while (!go.get());
                        ts.requestMore(n + 1);
                    }
                });
                
                go.set(true);
                ps.onCompleted();
                
                ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
                
                ts.assertValueCount(n);
                ts.assertNoErrors();
                ts.assertCompleted();
                
                List<Integer> list = ts.getOnNextEvents();
                for (int j = 0; j < n; j++) {
                    Assert.assertEquals(j, list.get(j).intValue());
                }
            }
        } finally {
            w.unsubscribe();
        }
    }
    
    @Test
    public void nullElements() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        
        Observable.from(new Integer[] { 1, null, 2}).takeLast(4, 1, TimeUnit.DAYS)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1, null, 2);
        ts.assertCompleted();
        ts.assertNoErrors();
    }
}
