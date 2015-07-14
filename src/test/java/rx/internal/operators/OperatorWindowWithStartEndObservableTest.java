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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import rx.*;
import rx.Observable;
import rx.Observer;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

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

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Observable<Object> openings = Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> observer) {
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Func1<Object, Observable<Object>> closer = new Func1<Object, Observable<Object>>() {
            @Override
            public Observable<Object> call(Object opening) {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
                        push(observer, new Object(), 100);
                        complete(observer, 101);
                    }
                });
            }
        };

        Observable<Observable<String>> windowed = source.window(openings, closer);
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

        Observable<String> source = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Func0<Observable<Object>> closer = new Func0<Observable<Object>>() {
            int calls;
            @Override
            public Observable<Object> call() {
                return Observable.create(new Observable.OnSubscribe<Object>() {
                    @Override
                    public void call(Subscriber<? super Object> observer) {
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

        Observable<Observable<String>> windowed = source.window(closer);
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

    private <T> void push(final Observer<T> observer, final T value, int delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Observer<?> observer, int delay) {
        innerScheduler.schedule(new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Action1<Observable<String>> observeWindow(final List<String> list, final List<List<String>> lists) {
        return new Action1<Observable<String>>() {
            @Override
            public void call(Observable<String> stringObservable) {
                stringObservable.subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
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
        PublishSubject<Integer> source = PublishSubject.create();
        
        PublishSubject<Integer> open = PublishSubject.create();
        final PublishSubject<Integer> close = PublishSubject.create();
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        
        source.window(open, new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return close;
            }
        }).unsafeSubscribe(ts);
        
        open.onNext(1);
        source.onNext(1);
        
        assertTrue(open.hasObservers());
        assertTrue(close.hasObservers());

        close.onNext(1);
        
        assertFalse(close.hasObservers());
        
        source.onCompleted();
        
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(1);
        
        assertFalse(ts.isUnsubscribed());
        assertFalse(open.hasObservers());
        assertFalse(close.hasObservers());
    }
    
    @Test
    public void testUnsubscribeAll() {
        PublishSubject<Integer> source = PublishSubject.create();
        
        PublishSubject<Integer> open = PublishSubject.create();
        final PublishSubject<Integer> close = PublishSubject.create();
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        
        source.window(open, new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return close;
            }
        }).unsafeSubscribe(ts);
        
        open.onNext(1);
        
        assertTrue(open.hasObservers());
        assertTrue(close.hasObservers());

        ts.unsubscribe();
        
        assertFalse(open.hasObservers());
        assertFalse(close.hasObservers());
    }
}