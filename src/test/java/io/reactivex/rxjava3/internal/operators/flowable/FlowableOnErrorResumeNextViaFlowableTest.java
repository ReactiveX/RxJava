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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableOnErrorResumeNextViaFlowableTest extends RxJavaTest {

    @Test
    public void resumeNext() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        Flowable<String> w = Flowable.unsafeCreate(f);
        Flowable<String> resume = Flowable.just("twoResume", "threeResume");
        Flowable<String> flowable = w.onErrorResumeWith(resume);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        flowable.subscribe(subscriber);

        try {
            f.t.join();
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
    }

    @Test
    public void mapResumeAsyncNext() {
        Subscription sr = mock(Subscription.class);
        // Trigger multiple failures
        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
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

        Flowable<String> flowable = w.onErrorResumeWith(resume);

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        flowable.subscribe(subscriber);

        try {
            f.t.join();
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
    }

    static final class TestObservable implements Publisher<String> {

        final Subscription upstream;
        final String[] values;
        Thread t;

        TestObservable(Subscription s, String... values) {
            this.upstream = s;
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> subscriber) {
            System.out.println("TestObservable subscribed to ...");
            subscriber.onSubscribe(upstream);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("fail".equals(s)) {
                                throw new RuntimeException("Forced Failure");
                            }
                            System.out.println("TestObservable onNext: " + s);
                            subscriber.onNext(s);
                        }
                        System.out.println("TestObservable onComplete");
                        subscriber.onComplete();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        subscriber.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(0, 100000)
                .onErrorResumeWith(Flowable.just(1))
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

        pp.onErrorResumeWith(Flowable.range(3, 2)).subscribe(ts);

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

}
