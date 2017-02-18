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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableOnErrorResumeNextViaFunctionTest {

    @Test
    public void testResumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Flowable<String> w = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext("one");
                observer.onError(new Throwable("injected failure"));
                observer.onNext("two");
                observer.onNext("three");
            }
        });

        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Flowable.just("twoResume", "threeResume");
            }

        };
        Flowable<String> observable = w.onErrorResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    @Test
    public void testResumeNextWithAsyncExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Subscription s = mock(Subscription.class);
        TestFlowable w = new TestFlowable(s, "one");
        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Flowable.just("twoResume", "threeResume");
            }

        };
        Flowable<String> observable = Flowable.unsafeCreate(w).onErrorResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError.
     */
    @Test
    public void testFunctionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestFlowable w = new TestFlowable(s, "one");
        Function<Throwable, Flowable<String>> resume = new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Flowable<String> observable = Flowable.unsafeCreate(w).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        DefaultSubscriber<String> observer = mock(DefaultSubscriber.class);
        observable.subscribe(observer);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(observer, times(1)).onNext("one");

        // we should have received an onError call on the Observer since the resume function threw an exception
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, times(0)).onComplete();
    }

    /**
     * Test that we receive the onError if an exception is thrown from an operator that
     * does not have manual try/catch handling like map does.
     */
    @Test
    @Ignore("Failed operator may leave the child subscriber in an inconsistent state which prevents further error delivery.")
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperator() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.just(1).lift(new FlowableOperator<String, Integer>() {

            @Override
            public Subscriber<? super Integer> apply(Subscriber<? super String> t1) {
                throw new RuntimeException("failed");
            }

        }).onErrorResumeNext(new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Flowable.just("success");
                } else {
                    return Flowable.error(t1);
                }
            }

        }).subscribe(ts);

        ts.assertTerminated();
        System.out.println(ts.values());
        ts.assertValue("success");
    }

    /**
     * Test that we receive the onError if an exception is thrown from an operator that
     * does not have manual try/catch handling like map does.
     */
    @Test
    @Ignore("A crashing operator may leave the downstream in an inconsistent state and not suitable for event delivery")
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperatorOnNext() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.just(1).lift(new FlowableOperator<String, Integer>() {

            @Override
            public Subscriber<? super Integer> apply(final Subscriber<? super String> t1) {
                return new FlowableSubscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription s) {
                        t1.onSubscribe(s);
                    }

                    @Override
                    public void onComplete() {
                        throw new RuntimeException("failed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        throw new RuntimeException("failed");
                    }

                    @Override
                    public void onNext(Integer t) {
                        throw new RuntimeException("failed");
                    }

                };
            }

        }).onErrorResumeNext(new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Flowable.just("success");
                } else {
                    return Flowable.error(t1);
                }
            }

        }).subscribe(ts);

        ts.assertTerminated();
        System.out.println(ts.values());
        ts.assertValue("success");
    }

    @Test
    public void testMapResumeAsyncNext() {
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

        Flowable<String> observable = w.onErrorResumeNext(new Function<Throwable, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Throwable t1) {
                return Flowable.just("twoResume", "threeResume").subscribeOn(Schedulers.computation());
            }

        });

        @SuppressWarnings("unchecked")
        DefaultSubscriber<String> observer = mock(DefaultSubscriber.class);

        TestSubscriber<String> ts = new TestSubscriber<String>(observer, Long.MAX_VALUE);
        observable.subscribe(ts);
        ts.awaitTerminalEvent();

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    private static class TestFlowable implements Publisher<String> {

        final String[] values;
        Thread t;

        TestFlowable(Subscription s, String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            System.out.println("TestFlowable subscribed to ...");
            observer.onSubscribe(new BooleanSubscription());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestFlowable thread");
                        for (String s : values) {
                            System.out.println("TestFlowable onNext: " + s);
                            observer.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestFlowable thread");
            t.start();
            System.out.println("done starting TestFlowable thread");
        }

    }

    @Test
    public void testBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
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
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void normalBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.onErrorResumeNext(new Function<Throwable, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Throwable v) {
                return Flowable.range(3, 2);
            }
        }).subscribe(ts);

        ts.request(2);

        ps.onNext(1);
        ps.onNext(2);
        ps.onError(new TestException("Forced failure"));

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
            public Object apply(Flowable<Integer> o) throws Exception {
                return Flowable.error(new IOException())
                        .onErrorResumeNext(Functions.justFunction(o));
            }
        }, false, 1, 1, 1);
    }

}
