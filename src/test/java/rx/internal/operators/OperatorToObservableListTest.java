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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorToObservableListTest {

    @Test
    public void testList() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList();

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }
    
    @Test
    public void testListViaObservable() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList();

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testListMultipleObservers() {
        Observable<String> w = Observable.from(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList();

        @SuppressWarnings("unchecked")
        Observer<List<String>> o1 = mock(Observer.class);
        observable.subscribe(o1);

        @SuppressWarnings("unchecked")
        Observer<List<String>> o2 = mock(Observer.class);
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onNext(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));
        verify(o1, times(1)).onCompleted();

        verify(o2, times(1)).onNext(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
        verify(o2, times(1)).onCompleted();
    }

    @Test
    public void testListWithNullValue() {
        Observable<String> w = Observable.from(Arrays.asList("one", null, "three"));
        Observable<List<String>> observable = w.toList();

        @SuppressWarnings("unchecked")
        Observer<List<String>> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", null, "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testListWithBlockingFirst() {
        Observable<String> o = Observable.from(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().toBlocking().first();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }
    @Test
    public void testBackpressureHonored() {
        Observable<List<Integer>> w = Observable.just(1, 2, 3, 4, 5).toList();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
            @Override
            public void onStart() {
                requestMore(0);
            }
        };
        
        w.subscribe(ts);
        
        assertTrue(ts.getOnNextEvents().isEmpty());
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertTrue(ts.getOnCompletedEvents().isEmpty());
        
        ts.requestMore(1);
        
        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getOnCompletedEvents().size());

        ts.requestMore(1);

        ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1, 2, 3, 4, 5)));
        assertTrue(ts.getOnErrorEvents().isEmpty());
        assertEquals(1, ts.getOnCompletedEvents().size());
    }
    @Test(timeout = 2000)
    public void testAsyncRequested() {
        Scheduler.Worker w = Schedulers.newThread().createWorker();
        try {
            for (int i = 0; i < 1000; i++) {
                if (i % 50 == 0) {
                    System.out.println("testAsyncRequested -> " + i);
                }
                PublishSubject<Integer> source = PublishSubject.create();
                Observable<List<Integer>> sorted = source.toList();

                final CyclicBarrier cb = new CyclicBarrier(2);
                final TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>() {
                    @Override
                    public void onStart() {
                        requestMore(0);
                    }
                };
                sorted.subscribe(ts);
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        await(cb);
                        ts.requestMore(1);
                    }
                });
                source.onNext(1);
                await(cb);
                source.onCompleted();
                ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
                ts.assertTerminalEvent();
                ts.assertNoErrors();
                ts.assertReceivedOnNext(Collections.singletonList(Arrays.asList(1)));
            }
        } finally {
            w.unsubscribe();
        }
    }
    static void await(CyclicBarrier cb) {
        try {
            cb.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
        }
    }
}
