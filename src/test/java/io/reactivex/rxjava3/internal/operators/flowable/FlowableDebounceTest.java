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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableDebounceTimed.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableDebounceTest extends RxJavaTest {

    private TestScheduler scheduler;
    private Subscriber<String> Subscriber;
    private Scheduler.Worker innerScheduler;

    @Before
    public void before() {
        scheduler = new TestScheduler();
        Subscriber = TestHelper.mockSubscriber();
        innerScheduler = scheduler.createWorker();
    }

    @Test
    public void debounceWithCompleted() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                publishNext(subscriber, 100, "one");    // Should be skipped since "two" will arrive before the timeout expires.
                publishNext(subscriber, 400, "two");    // Should be published since "three" will arrive after the timeout expires.
                publishNext(subscriber, 900, "three");   // Should be skipped since onComplete will arrive before the timeout expires.
                publishCompleted(subscriber, 1000);     // Should be published as soon as the timeout expires.
            }
        });

        Flowable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(Subscriber);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(Subscriber);
        // must go to 800 since it must be 400 after when two is sent, which is at 400
        scheduler.advanceTimeTo(800, TimeUnit.MILLISECONDS);
        inOrder.verify(Subscriber, times(1)).onNext("two");
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(Subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void debounceNeverEmits() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                // all should be skipped since they are happening faster than the 200ms timeout
                publishNext(subscriber, 100, "a");    // Should be skipped
                publishNext(subscriber, 200, "b");    // Should be skipped
                publishNext(subscriber, 300, "c");    // Should be skipped
                publishNext(subscriber, 400, "d");    // Should be skipped
                publishNext(subscriber, 500, "e");    // Should be skipped
                publishNext(subscriber, 600, "f");    // Should be skipped
                publishNext(subscriber, 700, "g");    // Should be skipped
                publishNext(subscriber, 800, "h");    // Should be skipped
                publishCompleted(subscriber, 900);     // Should be published as soon as the timeout expires.
            }
        });

        Flowable<String> sampled = source.debounce(200, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(Subscriber);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(Subscriber);
        inOrder.verify(Subscriber, times(0)).onNext(anyString());
        scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
        inOrder.verify(Subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void debounceWithError() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                Exception error = new TestException();
                publishNext(subscriber, 100, "one");    // Should be published since "two" will arrive after the timeout expires.
                publishNext(subscriber, 600, "two");    // Should be skipped since onError will arrive before the timeout expires.
                publishError(subscriber, 700, error);   // Should be published as soon as the timeout expires.
            }
        });

        Flowable<String> sampled = source.debounce(400, TimeUnit.MILLISECONDS, scheduler);
        sampled.subscribe(Subscriber);

        scheduler.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        InOrder inOrder = inOrder(Subscriber);
        // 100 + 400 means it triggers at 500
        scheduler.advanceTimeTo(500, TimeUnit.MILLISECONDS);
        inOrder.verify(Subscriber).onNext("one");
        scheduler.advanceTimeTo(701, TimeUnit.MILLISECONDS);
        inOrder.verify(Subscriber).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    private <T> void publishCompleted(final Subscriber<T> subscriber, long delay) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.onComplete();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishError(final Subscriber<T> subscriber, long delay, final Exception error) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.onError(error);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private <T> void publishNext(final Subscriber<T> subscriber, final long delay, final T value) {
        innerScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                subscriber.onNext(value);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Test
    public void debounceSelectorNormal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> debouncer = PublishProcessor.create();
        Function<Integer, Flowable<Integer>> debounceSel = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return debouncer;
            }
        };

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        source.debounce(debounceSel).subscribe(subscriber);

        source.onNext(1);
        debouncer.onNext(1);

        source.onNext(2);
        source.onNext(3);
        source.onNext(4);

        debouncer.onNext(2);

        source.onNext(5);
        source.onComplete();

        inOrder.verify(subscriber).onNext(1);
        inOrder.verify(subscriber).onNext(4);
        inOrder.verify(subscriber).onNext(5);
        inOrder.verify(subscriber).onComplete();

        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceSelectorFuncThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        Function<Integer, Flowable<Integer>> debounceSel = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        source.debounce(debounceSel).subscribe(subscriber);

        source.onNext(1);

        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onComplete();
        verify(subscriber).onError(any(TestException.class));
    }

    @Test
    public void debounceSelectorFlowableThrows() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        Function<Integer, Flowable<Integer>> debounceSel = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return Flowable.error(new TestException());
            }
        };

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        source.debounce(debounceSel).subscribe(subscriber);

        source.onNext(1);

        verify(subscriber, never()).onNext(any());
        verify(subscriber, never()).onComplete();
        verify(subscriber).onError(any(TestException.class));
    }

    @Test
    public void debounceTimedLastIsNotLost() {
        PublishProcessor<Integer> source = PublishProcessor.create();

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        source.debounce(100, TimeUnit.MILLISECONDS, scheduler).subscribe(subscriber);

        source.onNext(1);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(subscriber).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceSelectorLastIsNotLost() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> debouncer = PublishProcessor.create();

        Function<Integer, Flowable<Integer>> debounceSel = new Function<Integer, Flowable<Integer>>() {

            @Override
            public Flowable<Integer> apply(Integer t1) {
                return debouncer;
            }
        };

        Subscriber<Object> subscriber = TestHelper.mockSubscriber();

        source.debounce(debounceSel).subscribe(subscriber);

        source.onNext(1);
        source.onComplete();

        debouncer.onComplete();

        verify(subscriber).onNext(1);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));
    }

    @Test
    public void debounceWithTimeBackpressure() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        TestSubscriberEx<Integer> subscriber = new TestSubscriberEx<>();
        Flowable.merge(
                Flowable.just(1),
                Flowable.just(2).delay(10, TimeUnit.MILLISECONDS, scheduler)
        ).debounce(20, TimeUnit.MILLISECONDS, scheduler).take(1).subscribe(subscriber);

        scheduler.advanceTimeBy(30, TimeUnit.MILLISECONDS);

        subscriber.assertValue(2);
        subscriber.assertTerminated();
        subscriber.assertNoErrors();
    }

    @Test
    public void debounceDefaultScheduler() throws Exception {

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.range(1, 1000).debounce(1, TimeUnit.SECONDS).subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertValue(1000);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void debounceDefault() throws Exception {

        Flowable.just(1).debounce(1, TimeUnit.SECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().debounce(1, TimeUnit.SECONDS, new TestScheduler()));

        TestHelper.checkDisposed(PublishProcessor.create().debounce(Functions.justFunction(Flowable.never())));

        Disposable d = new FlowableDebounceTimed.DebounceEmitter<>(1, 1, null);
        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onComplete();
                    subscriber.onNext(1);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .debounce(1, TimeUnit.SECONDS, new TestScheduler())
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceSelector() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.debounce(new Function<Integer, Flowable<Long>>() {
                    @Override
                    public Flowable<Long> apply(Integer v) throws Exception {
                        return Flowable.timer(1, TimeUnit.SECONDS);
                    }
                });
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(final Flowable<Integer> f) throws Exception {
                return Flowable.just(1).debounce(new Function<Integer, Flowable<Integer>>() {
                    @Override
                    public Flowable<Integer> apply(Integer v) throws Exception {
                        return f;
                    }
                });
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void debounceWithEmpty() {
        Flowable.just(1).debounce(Functions.justFunction(Flowable.empty()))
        .test()
        .assertResult(1);
    }

    @Test
    public void backpressureNoRequest() {
        Flowable.just(1)
        .debounce(Functions.justFunction(Flowable.timer(1, TimeUnit.MILLISECONDS)))
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void backpressureNoRequestTimed() {
        Flowable.just(1)
        .debounce(1, TimeUnit.MILLISECONDS)
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.debounce(Functions.justFunction(Flowable.never()));
            }
        });
    }

    @Test
    public void disposeInOnNext() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        BehaviorProcessor.createDefault(1)
        .debounce(new Function<Integer, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Integer o) throws Exception {
                ts.cancel();
                return Flowable.never();
            }
        })
        .subscribeWith(ts)
        .assertEmpty();

        assertTrue(ts.isCancelled());
    }

    @Test
    public void disposedInOnComplete() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                ts.cancel();
                subscriber.onComplete();
            }
        }
        .debounce(Functions.justFunction(Flowable.never()))
        .subscribeWith(ts)
        .assertEmpty();
    }

    @Test
    public void emitLate() {
        final AtomicReference<Subscriber<? super Integer>> ref = new AtomicReference<>();

        TestSubscriber<Integer> ts = Flowable.range(1, 2)
        .debounce(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer o) throws Exception {
                if (o != 1) {
                    return Flowable.never();
                }
                return new Flowable<Integer>() {
                    @Override
                    protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                        subscriber.onSubscribe(new BooleanSubscription());
                        ref.set(subscriber);
                    }
                };
            }
        })
        .test();

        ref.get().onNext(1);

        ts
        .assertResult(2);
    }

    @Test
    public void badRequestReported() {
        TestHelper.assertBadRequestReported(Flowable.never().debounce(Functions.justFunction(Flowable.never())));
    }

    @Test
    public void timedDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f)
                    throws Exception {
                return f.debounce(1, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    public void timedDisposedIgnoredBySource() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(
                    org.reactivestreams.Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                ts.cancel();
                s.onNext(1);
                s.onComplete();
            }
        }
        .debounce(1, TimeUnit.SECONDS)
        .subscribe(ts);
    }

    @Test
    public void timedBadRequest() {
        TestHelper.assertBadRequestReported(Flowable.never().debounce(1, TimeUnit.SECONDS));
    }

    @Test
    public void timedLateEmit() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        DebounceTimedSubscriber<Integer> sub = new DebounceTimedSubscriber<>(
                ts, 1, TimeUnit.SECONDS, new TestScheduler().createWorker());

        sub.onSubscribe(new BooleanSubscription());

        DebounceEmitter<Integer> de = new DebounceEmitter<>(1, 50, sub);
        de.emit();
        de.emit();

        ts.assertEmpty();
    }

    @Test
    public void timedError() {
        Flowable.error(new TestException())
        .debounce(1, TimeUnit.SECONDS)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void debounceOnEmpty() {
        Flowable.empty().debounce(new Function<Object, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Object o) {
                return Flowable.just(new Object());
            }
        }).subscribe();
    }
}
