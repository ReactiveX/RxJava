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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableOnExceptionResumeNextViaFlowableTest {

    @Test
    public void testResumeNextWithException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "EXCEPTION", "two", "three");
        Flowable<String> w = Flowable.unsafeCreate(f);
        Flowable<String> resume = Flowable.just("twoResume", "threeResume");
        Flowable<String> observable = w.onExceptionResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Subscription)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testResumeNextWithRuntimeException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "RUNTIMEEXCEPTION", "two", "three");
        Flowable<String> w = Flowable.unsafeCreate(f);
        Flowable<String> resume = Flowable.just("twoResume", "threeResume");
        Flowable<String> observable = w.onExceptionResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Subscription)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testThrowablePassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "THROWABLE", "two", "three");
        Flowable<String> w = Flowable.unsafeCreate(f);
        Flowable<String> resume = Flowable.just("twoResume", "threeResume");
        Flowable<String> observable = w.onExceptionResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Subscription)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onNext("twoResume");
        verify(observer, never()).onNext("threeResume");
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testErrorPassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "ERROR", "two", "three");
        Flowable<String> w = Flowable.unsafeCreate(f);
        Flowable<String> resume = Flowable.just("twoResume", "threeResume");
        Flowable<String> observable = w.onExceptionResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer).onSubscribe((Subscription)any());
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onNext("twoResume");
        verify(observer, never()).onNext("threeResume");
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void testMapResumeAsyncNext() {
        // Trigger multiple failures
        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable("twoResume", "threeResume");
        Flowable<String> resume = Flowable.unsafeCreate(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
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

        Flowable<String> observable = w.onExceptionResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            // if the thread gets started (which it shouldn't if it's working correctly)
            if (f.t != null) {
                f.t.join();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }


    @Test
    public void testBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.range(0, 100000)
                .onExceptionResumeNext(Flowable.just(1))
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


    private static class TestObservable implements Publisher<String> {

        final String[] values;
        Thread t;

        TestObservable(String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            observer.onSubscribe(new BooleanSubscription());
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("EXCEPTION".equals(s)) {
                                throw new Exception("Forced Exception");
                            } else if ("RUNTIMEEXCEPTION".equals(s)) {
                                throw new RuntimeException("Forced RuntimeException");
                            } else if ("ERROR".equals(s)) {
                                throw new Error("Forced Error");
                            } else if ("THROWABLE".equals(s)) {
                                throw new Throwable("Forced Throwable");
                            }
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        System.out.println("TestObservable onComplete");
                        observer.onComplete();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }

    @Test
    public void normalBackpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        PublishProcessor<Integer> ps = PublishProcessor.create();

        ps.onExceptionResumeNext(Flowable.range(3, 2)).subscribe(ts);

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

}
