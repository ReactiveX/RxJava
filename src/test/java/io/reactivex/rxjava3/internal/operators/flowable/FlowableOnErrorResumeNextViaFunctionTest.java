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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableOnErrorResumeNextViaFunctionTest extends RxJavaTest {

    @Test
    public void resumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<>();
        Flowable<String> w = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                subscriber.onNext("one");
                subscriber.onError(new Throwable("injected failure"));
                subscriber.onNext("two");
                subscriber.onNext("three");
            }
        });

        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Flowable.just("twoResume", "threeResume");
            }

        };
        Flowable<String> flowable = w.onErrorResumeNext(resume);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, Mockito.never()).onNext("two");
        verify(subscriber, Mockito.never()).onNext("three");
        verify(subscriber, times(1)).onNext("twoResume");
        verify(subscriber, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    @Test
    public void resumeNextWithAsyncExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<>();
        Subscription s = mock(Subscription.class);
        TestFlowable w = new TestFlowable(s, "one");
        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Flowable.just("twoResume", "threeResume");
            }

        };
        Flowable<String> flowable = Flowable.unsafeCreate(w).onErrorResumeNext(resume);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, Mockito.never()).onNext("two");
        verify(subscriber, Mockito.never()).onNext("three");
        verify(subscriber, times(1)).onNext("twoResume");
        verify(subscriber, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError.
     */
    @Test
    public void functionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestFlowable w = new TestFlowable(s, "one");
        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Flowable<String> flowable = Flowable.unsafeCreate(w).onErrorResumeNext(resume);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(subscriber, times(1)).onNext("one");

        // we should have received an onError call on the Observer since the resume function threw an exception
        verify(subscriber, times(1)).onError(any(Throwable.class));
        verify(subscriber, times(0)).onComplete();
    }

    @Test
    public void mapResumeAsyncNext() {
        // Trigger multiple failures
        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");

        // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
        //  rx.operator incl onErrorResumeNextViaFlowable)
        w = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Flowable<String> flowable = w.onErrorResumeNext(new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                return Flowable.just("twoResume", "threeResume").subscribeOn(Schedulers.computation());
            }

        });

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        TestSubscriber<String> ts = new TestSubscriber<>(subscriber, Long.MAX_VALUE);
        flowable.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);

        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, Mockito.never()).onNext("two");
        verify(subscriber, Mockito.never()).onNext("three");
        verify(subscriber, times(1)).onNext("twoResume");
        verify(subscriber, times(1)).onNext("threeResume");
    }

    private static class TestFlowable implements Publisher<String> {

        final String[] values;
        Thread t;

        TestFlowable(Subscription s, String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            System.out.println("TestFlowable subscribed to ...");
            subscriber.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestFlowable thread");
                        for (String s : values) {
                            System.out.println("TestFlowable onNext: " + s);
                            subscriber.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }

            });
            System.out.println("starting TestFlowable thread");
            t.start();
            System.out.println("done starting TestFlowable thread");
        }

    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(0, 100000)
                .onErrorResumeNext(new Function<Throwable, Flowable<Integer>>() {

                    @Override
                    public Flowable<Integer> apply(Throwable t1) {
                        return Flowable.just(1);
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    int c;

                    @Override
                    public Integer apply(Integer t1) {
                        if (c++ <= 1) {
                            // slow
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        return t1;
                    }

                })
                .subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void normalBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.onErrorResumeNext(new Function<Throwable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Throwable v) {
                return Flowable.range(3, 2);
            }
        }).subscribe(ts);

        ts.request(2);

        pp.onNext(1);
        pp.onNext(2);
        pp.onError(new TestException("Forced failure"));

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(2);

        ts.assertValues(1, 2, 3, 4);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void badOtherSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return Flowable.error(new IOException())
                        .onErrorResumeNext(Functions.justFunction(f));
            }
        }, false, 1, 1, 1);
    }

}
