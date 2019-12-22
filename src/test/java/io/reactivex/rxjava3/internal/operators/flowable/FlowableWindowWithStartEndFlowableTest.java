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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableWindowWithStartEndFlowableTest extends RxJavaTest {

    private TestScheduler scheduler;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void flowableBasedOpenerAndCloser() {
        final List<String> list = new ArrayList<>();
        final List<List<String>> lists = new ArrayList<>();

        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                push(subscriber, "one", 10);
                push(subscriber, "two", 60);
                push(subscriber, "three", 110);
                push(subscriber, "four", 160);
                push(subscriber, "five", 210);
                complete(subscriber, 500);
            }
        });

        Flowable<Object> openings = Flowable.unsafeCreate(new Publisher<Object>() {
            @Override
            public void subscribe(Subscriber<? super Object> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                push(subscriber, new Object(), 50);
                push(subscriber, new Object(), 200);
                complete(subscriber, 250);
            }
        });

        Function<Object, Flowable<Object>> closer = new Function<Object, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Object opening) {
                return Flowable.unsafeCreate(new Publisher<Object>() {
                    @Override
                    public void subscribe(Subscriber<? super Object> subscriber) {
                        subscriber.onSubscribe(new BooleanSubscription());
                        push(subscriber, new Object(), 100);
                        complete(subscriber, 101);
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

    private List<String> list(String... args) {
        List<String> list = new ArrayList<>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    private <T> void push(final Subscriber<T> subscriber, final T value, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void complete(final Subscriber<?> subscriber, int delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.onComplete();
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
    public void noUnsubscribeAndNoLeak() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        PublishProcessor<Integer> open = PublishProcessor.create();
        final PublishProcessor<Integer> close = PublishProcessor.create();

        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<>();

        source.window(open, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return close;
            }
        })
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .subscribe(ts);

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
    public void unsubscribeAll() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        PublishProcessor<Integer> open = PublishProcessor.create();
        final PublishProcessor<Integer> close = PublishProcessor.create();

        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<>();

        source.window(open, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return close;
            }
        })
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .subscribe(ts);

        open.onNext(1);

        assertTrue(open.hasSubscribers());
        assertTrue(close.hasSubscribers());

        ts.cancel();

        // Disposing the outer sequence stops the opening of new windows
        assertFalse(open.hasSubscribers());
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

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
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
        .subscribe(ts);

        ps.onNext(1);

        ts
        .awaitDone(1, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void boundarySelectorNormal() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> ts = source.window(start, new Function<Integer, Flowable<Integer>>() {
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

        ts.assertResult(1, 2, 3, 4, 5, 5, 6, 6, 7, 8);
    }

    @Test
    public void startError() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> ts = source.window(start, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test();

        start.onError(new TestException());

        ts.assertFailure(TestException.class);

        assertFalse("Source has observers!", source.hasSubscribers());
        assertFalse("Start has observers!", start.hasSubscribers());
        assertFalse("End has observers!", end.hasSubscribers());
    }

    @Test
    public void endError() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> start = PublishProcessor.create();
        final PublishProcessor<Integer> end = PublishProcessor.create();

        TestSubscriber<Integer> ts = source.window(start, new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return end;
            }
        })
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test();

        start.onNext(1);
        end.onError(new TestException());

        ts.assertFailure(TestException.class);

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

    @Test
    public void windowCloseIngoresCancel() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            BehaviorProcessor.createDefault(1)
            .window(BehaviorProcessor.createDefault(1), new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer f) throws Exception {
                    return new Flowable<Integer>() {
                        @Override
                        protected void subscribeActual(
                                Subscriber<? super Integer> s) {
                            s.onSubscribe(new BooleanSubscription());
                            s.onNext(1);
                            s.onNext(2);
                            s.onError(new TestException());
                        }
                    };
                }
            })
            .doOnNext(new Consumer<Flowable<Integer>>() {
                @Override
                public void accept(Flowable<Integer> w) throws Throwable {
                    w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
                }
            })
            .test()
            .assertValueCount(1)
            .assertNoErrors()
            .assertNotComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static Flowable<Integer> flowableDisposed(final AtomicBoolean ref) {
        return Flowable.just(1).concatWith(Flowable.<Integer>never())
                .doOnCancel(new Action() {
                    @Override
                    public void run() throws Exception {
                        ref.set(true);
                    }
                });
    }

    @Test
    public void mainAndBoundaryDisposeOnNoWindows() {
        AtomicBoolean mainDisposed = new AtomicBoolean();
        AtomicBoolean openDisposed = new AtomicBoolean();
        final AtomicBoolean closeDisposed = new AtomicBoolean();

        flowableDisposed(mainDisposed)
        .window(flowableDisposed(openDisposed), new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) throws Exception {
                return flowableDisposed(closeDisposed);
            }
        })
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> w) throws Throwable {
                w.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer()); // avoid abandonment
            }
        })
        .to(TestHelper.<Flowable<Integer>>testConsumer())
        .assertSubscribed()
        .assertNoErrors()
        .assertNotComplete()
        .cancel();

        assertTrue(mainDisposed.get());
        assertTrue(openDisposed.get());
        assertTrue(closeDisposed.get());
    }

    @Test
    public void mainWindowMissingBackpressure() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        TestSubscriber<Flowable<Integer>> ts = source.window(boundary, Functions.justFunction(Flowable.never()))
        .test(0L)
        ;

        ts.assertEmpty();

        boundary.onNext(1);

        ts.assertFailure(MissingBackpressureException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(boundary.hasSubscribers());
    }

    @Test
    public void cancellingWindowCancelsUpstream() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.window(Flowable.just(1).concatWith(Flowable.<Integer>never()), Functions.justFunction(Flowable.never()))
        .take(1)
        .flatMap(new Function<Flowable<Integer>, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Flowable<Integer> w) throws Throwable {
                return w.take(1);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts
        .assertResult(1);

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());
    }

    @Test
    public void windowAbandonmentCancelsUpstream() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final AtomicReference<Flowable<Integer>> inner = new AtomicReference<>();

        TestSubscriber<Flowable<Integer>> ts = pp.window(Flowable.<Integer>just(1).concatWith(Flowable.<Integer>never()),
                Functions.justFunction(Flowable.never()))
        .doOnNext(new Consumer<Flowable<Integer>>() {
            @Override
            public void accept(Flowable<Integer> v) throws Throwable {
                inner.set(v);
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        ts
        .assertValueCount(1)
        ;

        pp.onNext(1);

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        ts
        .assertValueCount(1)
        .assertNoErrors()
        .assertNotComplete();

        assertFalse("Processor still has subscribers!", pp.hasSubscribers());

        inner.get().test().assertResult();
    }

    @Test
    public void closingIndicatorFunctionCrash() {

        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        TestSubscriber<Flowable<Integer>> ts = source.window(boundary, new Function<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Integer end) throws Throwable {
                throw new TestException();
            }
        })
        .test()
        ;

        ts.assertEmpty();

        boundary.onNext(1);

        ts.assertFailure(TestException.class);

        assertFalse(source.hasSubscribers());
        assertFalse(boundary.hasSubscribers());

    }
}
