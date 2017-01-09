/**
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.*;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subscribers.*;

public class FlowableWindowWithStartEndFlowableTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void testFlowableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 500);
            }
        });

        Flowable<Object> openings = Flowable.unsafeCreate(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, new Object(), 50);
                push(observer, new Object(), 200);
                complete(observer, 250);
            }
        });

        Function<Object, Flowable<Object>> closer = new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object opening) {
                return Flowable.unsafeCreate(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(new BooleanSubscription());
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
    public void testFlowableBasedCloser() {
        final List<String> list = new ArrayList<String>();
        final List<List<String>> lists = new ArrayList<List<String>>();

        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                push(observer, "one", 10);
                push(observer, "two", 60);
                push(observer, "three", 110);
                push(observer, "four", 160);
                push(observer, "five", 210);
                complete(observer, 250);
            }
        });

        Callable<Flowable<Object>> closer = new Callable<Flowable<Object>>() {
            int calls;
            @Override
            public Flowable<Object> call() {
                return Flowable.unsafeCreate(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> observer) {
                        observer.onSubscribe(new BooleanSubscription());
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
            public void accept(Flowable<String> stringFlowable) {
                stringFlowable.subscribe(new DefaultSubscriber<String>() {
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
        }).subscribe(ts);

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
        }).subscribe(ts);

        open.onNext(1);

        assertTrue(open.hasSubscribers());
        assertTrue(close.hasSubscribers());

        ts.dispose();

        // FIXME subject has subscribers because of the open window
        assertTrue(open.hasSubscribers());
        // FIXME subject has subscribers because of the open window
        assertTrue(close.hasSubscribers());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).window(Flowable.just(2), Functions.justFunction(Flowable.never())));
    }

    @Test
    public void reentrant() {
        final FlowableProcessor<Integer> ps = PublishProcessor.<Integer>create();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                    ps.onComplete();
                }
            }
        };

        ps.window(BehaviorProcessor.createDefault(1), Functions.justFunction(Flowable.never()))
        .flatMap(new Function<Flowable<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Flowable<Integer> v) throws Exception {
                return v;
            }
        })
        .subscribe(to);

        ps.onNext(1);

        to
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void badSourceCallable() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> o) throws Exception {
                return o.window(Flowable.just(1), Functions.justFunction(Flowable.never()));
            }
        }, false, 1, 1, (Object[])null);
    }

    @Test
    public void boundarySelectorNormal() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> to = source.window(start, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test();

        start.onNext(0);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onNext(4);

        start.onNext(1);

        source.onNext(5);
        source.onNext(6);

        end.onNext(1);

        start.onNext(2);

        TestHelper.emit(source, 7, 8);

        to.assertResult(1, 2, 3, 4, 5, 5, 6, 6, 7, 8);
    }

    @Test
    public void startError() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> to = source.window(start, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test();

        start.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("Source has observers!", source.hasSubscribers());
        assertFalse("Start has observers!", start.hasSubscribers());
        assertFalse("End has observers!", end.hasSubscribers());
    }

    @Test
    public void endError() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> to = source.window(start, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test();

        start.onNext(1);
        end.onError(new TestException());

        to.assertFailure(TestException.class);

        assertFalse("Source has observers!", source.hasSubscribers());
        assertFalse("Start has observers!", start.hasSubscribers());
        assertFalse("End has observers!", end.hasSubscribers());
    }

    @Test
    public void mainError() {
        Flowable.<Integer>error(new TestException())
        .window(Flowable.never(), Functions.justFunction(Flowable.just(1)))
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }
}
