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

package io.reactivex.internal.operators;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Function;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorOnErrorResumeNextViaObservableTest {

    @Test
    public void testResumeNext() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onErrorResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        try {
            f.t.join();
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
    }

    @Test
    public void testMapResumeAsyncNext() {
        Subscription sr = mock(Subscription.class);
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
        Observable<String> resume = Observable.create(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s))
                    throw new RuntimeException("Forced Failure");
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Observable<String> observable = w.onErrorResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);

        try {
            f.t.join();
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
    }
    
    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribe() {
        Observable<String> testObservable = Observable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> observable = testObservable.onErrorResumeNext(resume);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("resume");
    }
    
    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribeAsync() {
        Observable<String> testObservable = Observable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> observable = testObservable.subscribeOn(Schedulers.io()).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        TestSubscriber<String> ts = new TestSubscriber<>(observer, Long.MAX_VALUE);
        observable.subscribe(ts);

        ts.awaitTerminalEvent();
        
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("resume");
    }

    private static class TestObservable implements Publisher<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("fail".equals(s))
                                throw new RuntimeException("Forced Failure");
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        System.out.println("TestObservable onCompleted");
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
    public void testBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(0, 100000)
                .onErrorResumeNext(Observable.just(1))
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    int c = 0;

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
}