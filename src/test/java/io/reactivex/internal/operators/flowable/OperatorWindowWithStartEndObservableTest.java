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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.DefaultObserver;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorWindowWithStartEndObservableTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.create(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Flowable<Object> openings = Flowable.create(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Function<Object, Flowable<Object>> closer = new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object opening) {
                return Flowable.create(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                    }
                });
            }
        };

        Flowable<Flowable<String>> windowed = source.window(openings, closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(0), list("two", "three"));
        assertEquals(lists.get(1), list("five"));
    }

    @Test
    public void testObservableBasedCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.create(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Supplier<Flowable<Object>> closer = new Supplier<Flowable<Object>>() {
            int calls;
            @Override
            public Flowable<Object> get() {
                return Flowable.create(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(EmptySubscription.INSTANCE);
                        int c = calls++;
                        if (c == 0) {
                            push(observer, new Object(), 100);
                        } else
                        if (c == 1) {
                            push(observer, new Object(), 100);
                        } else {
                            complete(observer, 101);
                        }
                    }
                });
            }
        };

        Flowable<Flowable<String>> windowed = source.window(closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(3, lists.size());
        assertEquals(lists.get(0), list("one", "two"));
        assertEquals(lists.get(1), list("three", "four"));
        assertEquals(lists.get(2), list("five"));
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

    private Consumer<Flowable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
        return new Consumer<Flowable<String>>() {
            @Override
            public void accept(Flowable<String> stringObservable) {
                stringObservable.subscribe(new DefaultObserver<String>() {
                    @Override
                    public void onComplete() {
                        lists.add(new ArrayList<String>(list));
                        list.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                        fail(e.getMessage());
                    }

                    @Override
                    public void onNext(String args) {
                        list.add(args);
                    }
                });
            }
        };
    }
    
    @Test
    public void testNoUnsubscribeAndNoLeak() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        PublishProcessor<Integer> open = PublishProcessor.create();
        final PublishProcessor<Integer> close = PublishProcessor.create();
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
        
        source.window(open, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return close;
            }
        }).unsafeSubscribe(ts);
        
        open.onNext(1);
        source.onNext(1);
        
        assertTrue(open.hasSubscribers());
        assertTrue(close.hasSubscribers());

        close.onNext(1);
        
        assertFalse(close.hasSubscribers());
        
        source.onComplete();
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
        
        assertFalse(ts.isCancelled());
        assertFalse(open.hasSubscribers());
        assertFalse(close.hasSubscribers());
    }
    
    @Test
    public void testUnsubscribeAll() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        
        PublishProcessor<Integer> open = PublishProcessor.create();
        final PublishProcessor<Integer> close = PublishProcessor.create();
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
        
        source.window(open, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return close;
            }
        }).unsafeSubscribe(ts);
        
        open.onNext(1);
        
        assertTrue(open.hasSubscribers());
        assertTrue(close.hasSubscribers());

        ts.dispose();
        
        // FIXME subject has subscribers because of the open window
        assertTrue(open.hasSubscribers());
        // FIXME subject has subscribers because of the open window
        assertTrue(close.hasSubscribers());
    }
}