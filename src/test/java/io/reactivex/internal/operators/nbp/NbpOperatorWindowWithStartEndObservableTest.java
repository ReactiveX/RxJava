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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorWindowWithStartEndObservableTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testObservableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<>();
        final List<List<String>> lists = new ArrayList<>();

        NbpObservable<String> source = NbpObservable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 10);
                push(NbpObserver, "two", 60);
                push(NbpObserver, "three", 110);
                push(NbpObserver, "four", 160);
                push(NbpObserver, "five", 210);
                complete(NbpObserver, 500);
            }
        });

        NbpObservable<Object> openings = NbpObservable.create(new NbpOnSubscribe<Object>() {
            @Override
            public void accept(NbpSubscriber<? super Object> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, new Object(), 50);
                push(NbpObserver, new Object(), 200);
                complete(NbpObserver, 250);
            }
        });

        Function<Object, NbpObservable<Object>> closer = new Function<Object, NbpObservable<Object>>() {
            @Override
            public NbpObservable<Object> apply(Object opening) {
                return NbpObservable.create(new NbpOnSubscribe<Object>() {
                    @Override
                    public void accept(NbpSubscriber<? super Object> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        push(NbpObserver, new Object(), 100);
                        complete(NbpObserver, 101);
                    }
                });
            }
        };

        NbpObservable<NbpObservable<String>> windowed = source.window(openings, closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(2, lists.size());
        assertEquals(lists.get(0), list("two", "three"));
        assertEquals(lists.get(1), list("five"));
    }

    @Test
    public void testObservableBasedCloser() {
        final List<String> list = new ArrayList<>();
        final List<List<String>> lists = new ArrayList<>();

        NbpObservable<String> source = NbpObservable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                push(NbpObserver, "one", 10);
                push(NbpObserver, "two", 60);
                push(NbpObserver, "three", 110);
                push(NbpObserver, "four", 160);
                push(NbpObserver, "five", 210);
                complete(NbpObserver, 250);
            }
        });

        Supplier<NbpObservable<Object>> closer = new Supplier<NbpObservable<Object>>() {
            int calls;
            @Override
            public NbpObservable<Object> get() {
                return NbpObservable.create(new NbpOnSubscribe<Object>() {
                    @Override
                    public void accept(NbpSubscriber<? super Object> NbpObserver) {
                        NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                        int c = calls++;
                        if (c == 0) {
                            push(NbpObserver, new Object(), 100);
                        } else
                        if (c == 1) {
                            push(NbpObserver, new Object(), 100);
                        } else {
                            complete(NbpObserver, 101);
                        }
                    }
                });
            }
        };

        NbpObservable<NbpObservable<String>> windowed = source.window(closer);
        windowed.subscribe(observeWindow(list, lists));

        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        assertEquals(3, lists.size());
        assertEquals(lists.get(0), list("one", "two"));
        assertEquals(lists.get(1), list("three", "four"));
        assertEquals(lists.get(2), list("five"));
    }

    private List<String> list(String... args) {
        List<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final NbpSubscriber<T> NbpObserver, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final NbpSubscriber<?> NbpObserver, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                NbpObserver.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Consumer<NbpObservable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
        return new Consumer<NbpObservable<String>>() {
            @Override
            public void accept(NbpObservable<String> stringObservable) {
                stringObservable.subscribe(new NbpObserver<String>() {
                    @Override
                    public void onComplete() {
                        lists.add(new ArrayList<>(list));
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        
        NbpPublishSubject<Integer> open = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> close = NbpPublishSubject.create();
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
        
        source.window(open, new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t) {
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        
        NbpPublishSubject<Integer> open = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> close = NbpPublishSubject.create();
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
        
        source.window(open, new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t) {
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