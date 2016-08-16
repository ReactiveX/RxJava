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

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.*;


public class FlowableWindowWithTimeTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testTimedAndCount() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 90);
                push(observer, "three", 110);
                push(observer, "four", 190);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Flowable<Flowable<String>> windowed = source.window(100, TimeUnit.MILLISECONDS, scheduler, 2);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(100, TimeUnit.MILLISECONDS);
        assertEquals(1, lists.size());
        assertEquals(lists.get(0), list("one", "two"));

        scheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(1), list("three", "four"));

        scheduler.advanceTimeTo(300, TimeUnit.MILLISECONDS);
        assertEquals(3, lists.size());
        assertEquals(lists.get(2), list("five"));
    }

    @Test
    public void testTimed() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 98);
                push(observer, "two", 99);
                push(observer, "three", 99); // FIXME happens after the window is open
                push(observer, "four", 101);
                push(observer, "five", 102);
                complete(observer, 150);
            }
        });

        Flowable<Flowable<String>> windowed = source.window(100, TimeUnit.MILLISECONDS, scheduler);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(101, TimeUnit.MILLISECONDS);
        assertEquals(1, lists.size());
        assertEquals(lists.get(0), list("one", "two", "three"));

        scheduler.advanceTimeTo(201, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(1), list("four", "five"));
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final Subscriber<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Subscriber<?> observer, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> Consumer<Flowable<T>> observeWindow(final List<T> list, final List<List<T>> lists) {
        return new Consumer<Flowable<T>>() {
            @Override
            public void accept(Flowable<T> stringObservable) {
                stringObservable.subscribe(new DefaultSubscriber<T>() {
                    @Override
                    public void onComplete() {
                        lists.add(new ArrayList<T>(list));
                        list.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Assert.fail(e.getMessage());
                    }

                    @Override
                    public void onNext(T args) {
                        list.add(args);
                    }
                });
            }
        };
    }
    @Test
    public void testExactWindowSize() {
        Flowable<Flowable<Integer>> source = Flowable.range(1, 10)
                .window(1, TimeUnit.MINUTES, scheduler, 3);
        
        final List<Integer> list = new ArrayList<Integer>();
        final List<List<Integer>> lists = new ArrayList<List<Integer>>();
        
        source.subscribe(observeWindow(list, lists));
        
        assertEquals(4, lists.size());
        assertEquals(3, lists.get(0).size());
        assertEquals(Arrays.asList(1, 2, 3), lists.get(0));
        assertEquals(3, lists.get(1).size());
        assertEquals(Arrays.asList(4, 5, 6), lists.get(1));
        assertEquals(3, lists.get(2).size());
        assertEquals(Arrays.asList(7, 8, 9), lists.get(2));
        assertEquals(1, lists.get(3).size());
        assertEquals(Arrays.asList(10), lists.get(3));
    }
    
    @Test
    public void testTakeFlatMapCompletes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final AtomicInteger wip = new AtomicInteger();
        
        final int indicator = 999999999;
        
        FlowableWindowWithSizeTest.hotStream()
        .window(300, TimeUnit.MILLISECONDS)
        .take(10)
        .doOnComplete(new Action() {
            @Override
            public void run() {
                System.out.println("Main done!");
            }
        })
        .flatMap(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> w) {
                return w.startWith(indicator)
                        .doOnComplete(new Action() {
                            @Override
                            public void run() {
                                System.out.println("inner done: " + wip.incrementAndGet());
                            }
                        })
                        ;
            }
        })
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer pv) {
                System.out.println(pv);
            }
        })
        .subscribe(ts);
        
        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertComplete();
        Assert.assertTrue(ts.valueCount() != 0);
    }
    
}