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
package rx.schedulers;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.observables.TestableObservable;
import rx.observers.TestableObserver;
import static rx.util.Recorded.*;
import static rx.util.RecordedSubscription.subscribe;
import rx.util.functions.Func0;

/**
 * Who tests the testers?
 */
public class TestSchedulerTest {
    TestScheduler scheduler;
    @Mock
    Observer<Object> observer;
    @Before
    public void before() {
        scheduler = new TestScheduler();
        MockitoAnnotations.initMocks(this);
    }
    @Test
    @SuppressWarnings("unchecked")
    public void testHotObservable() {
        
        TestableObserver<Integer> testable = scheduler.createObserver();
        
        TestableObservable<Integer> source = scheduler.createHotObservable(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onCompleted(500, 5)
        );
        
        source.subscribe(observer);
        source.subscribe(testable);
        
        source.runToEnd();
        
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onCompleted();
        
        verify(observer, never()).onError(any(Throwable.class));
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onCompleted(500, 5)
        ), testable.messages());
        
        Observer<Object> o2 = mock(Observer.class);
        InOrder io2 = inOrder(o2);
        
        source.subscribe(o2);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        io2.verifyNoMoreInteractions();

        TestableObserver<Integer> testable2 = scheduler.createObserver();
 
        source.subscribe(testable2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500),
                subscribe(500),
                subscribe(1500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(), testable2.messages());
        
    }
    static class CustomException extends RuntimeException {
        
    }
    @Test
    @SuppressWarnings("unchecked")
    public void testThrowingHotObservable() {
        TestableObserver<Integer> testable = scheduler.createObserver();
        
        CustomException ex = new CustomException();
        
        TestableObservable<Integer> source = scheduler.createHotObservable(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onError(500, ex, 5)
        );
        
        source.subscribe(observer);
        source.subscribe(testable);
        
        source.runToEnd();
        
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onError(ex);
        
        verify(observer, never()).onCompleted();
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onError(500, ex, 5)
        ), testable.messages());
        
        Observer<Object> o2 = mock(Observer.class);
        InOrder io2 = inOrder(o2);
        
        source.subscribe(o2);
        
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        io2.verifyNoMoreInteractions();
        
                TestableObserver<Integer> testable2 = scheduler.createObserver();
 
        source.subscribe(testable2);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500),
                subscribe(500),
                subscribe(1500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(), testable2.messages());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testColdObservable() {
        
        TestableObserver<Integer> testable = scheduler.createObserver();
        
        TestableObservable<Integer> source = scheduler.createColdObservable(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onCompleted(500, 5)
        );
        
        source.subscribe(observer);
        source.subscribe(testable);
        
        source.runToEnd();
        
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onCompleted();
        
        verify(observer, never()).onError(any(Throwable.class));
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onCompleted(500, 5)
        ), testable.messages());
        
        TestableObserver<Integer> testable2 = scheduler.createObserver();
 
        source.subscribe(testable2);
        source.runToEnd();
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500),
                subscribe(500, 1000)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(600, 1),
                onNext(700, 2),
                onNext(800, 3),
                onNext(900, 4),
                onCompleted(1000, 5)
        ), testable2.messages());

    }
    @Test
    @SuppressWarnings("unchecked")
    public void testThrowingColdObservable() {
        CustomException ex = new CustomException();
        
        TestableObserver<Integer> testable = scheduler.createObserver();
        
        TestableObservable<Integer> source = scheduler.createColdObservable(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onError(500, ex, 5)
        );
        
        source.subscribe(observer);
        source.subscribe(testable);
        
        source.runToEnd();
        
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onNext(4);
        inOrder.verify(observer, times(1)).onError(ex);
        
        verify(observer, never()).onCompleted();
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(100, 1),
                onNext(200, 2),
                onNext(300, 3),
                onNext(400, 4),
                onError(500, ex, 5)
        ), testable.messages());
        
        TestableObserver<Integer> testable2 = scheduler.createObserver();
 
        source.subscribe(testable2);
        source.runToEnd();
        
        assertEquals(Arrays.asList(
                subscribe(0, 500),
                subscribe(0, 500),
                subscribe(500, 1000)
        ), source.subscriptions());
        
        assertEquals(Arrays.asList(
                onNext(600, 1),
                onNext(700, 2),
                onNext(800, 3),
                onNext(900, 4),
                onError(1000, ex, 5)
        ), testable2.messages());

    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testStart() {
        final Observable<Long> source = Observable.interval(100, TimeUnit.MILLISECONDS, scheduler).take(10);
        
        Func0<Observable<Long>> func = new Func0<Observable<Long>>() {
            @Override
            public Observable<Long> call() {
                return source;
            }
        };
        
        TestableObserver<Long> testable = scheduler.start(func, 100, 200, 950);
        
        assertEquals(Arrays.asList(
                onNext(300, 0L),
                onNext(400, 1L),
                onNext(500, 2L),
                onNext(600, 3L),
                onNext(700, 4L),
                onNext(800, 5L),
                onNext(900, 6L)
        ), testable.messages());
    }
}
