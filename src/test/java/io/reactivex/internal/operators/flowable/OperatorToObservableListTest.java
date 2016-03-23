/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.flowable.TestHelper;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorToObservableListTest {

    @Test
    public void testList() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }
    
    @Test
    public void testListViaObservable() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testListMultipleSubscribers() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        Flowable<List<String>> observable = w.toList();

        Subscriber<List<String>> o1 = TestHelper.mockSubscriber();
        observable.subscribe(o1);

        Subscriber<List<String>> o2 = TestHelper.mockSubscriber();
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onNext(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));
        verify(o1, times(1)).onComplete();

        verify(o2, times(1)).onNext(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
        verify(o2, times(1)).onComplete();
    }

    @Test
    @Ignore("Null values are not allowed")
    public void testListWithNullValue() {
        Flowable<String> w = Flowable.fromIterable(Arrays.asList("one", null, "three"));
        Flowable<List<String>> observable = w.toList();

        Subscriber<List<String>> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", null, "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testListWithBlockingFirst() {
        Flowable<String> o = Flowable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().toBlocking().first();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }
    @Test
    public void testBackpressureHonored() {
        Flowable<List<Integer>> w = Flowable.just(1, 2, 3, 4, 5).toList();
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>((Long)null);
        
        w.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(1);
        
        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();

        ts.request(1);

        ts.assertValue(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertNoErrors();
        ts.assertComplete();
    }
    @Test(timeout = 2000)
    @Ignore("PublishSubject no longer emits without requests so this test fails due to the race of onComplete and request")
    public void testAsyncRequested() {
        Scheduler.Worker w = Schedulers.newThread().createWorker();
        try {
            for (int i = 0; i < 1000; i++) {
                if (i % 50 == 0) {
                    System.out.println("testAsyncRequested -> " + i);
                }
                PublishProcessor<Integer> source = PublishProcessor.create();
                Flowable<List<Integer>> sorted = source.toList();

                final CyclicBarrier cb = new CyclicBarrier(2);
                final TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>((Long)null);
                sorted.subscribe(ts);
                
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        await(cb);
                        ts.request(1);
                    }
                });
                source.onNext(1);
                await(cb);
                source.onComplete();
                ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
                ts.assertTerminated();
                ts.assertNoErrors();
                ts.assertValue(Arrays.asList(1));
            }
        } finally {
            w.dispose();
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