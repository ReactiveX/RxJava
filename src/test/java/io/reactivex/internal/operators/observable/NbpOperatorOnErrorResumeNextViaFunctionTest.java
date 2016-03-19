/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorOnErrorResumeNextViaFunctionTest {

    @Test
    public void testResumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Observable<String> w = Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                NbpObserver.onNext("one");
                NbpObserver.onError(new Throwable("injected failure"));
                NbpObserver.onNext("two");
                NbpObserver.onNext("three");
            }
        });

        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Observable.just("twoResume", "threeResume");
            }

        };
        Observable<String> NbpObservable = w.onErrorResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable.subscribe(NbpObserver);

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    @Test
    public void testResumeNextWithAsyncExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one");
        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Observable.just("twoResume", "threeResume");
            }

        };
        Observable<String> o = Observable.create(w).onErrorResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();

        o.subscribe(NbpObserver);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError
     */
    @Test
    public void testFunctionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one");
        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Observable<String> o = Observable.create(w).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        o.subscribe(NbpObserver);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(NbpObserver, times(1)).onNext("one");

        // we should have received an onError call on the NbpObserver since the resume function threw an exception
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, times(0)).onComplete();
    }

    /**
     * Test that we receive the onError if an exception is thrown from an operator that
     * does not have manual try/catch handling like map does.
     */
    @Test
    @Ignore("Failed operator may leave the child NbpSubscriber in an inconsistent state which prevents further error delivery.")
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperator() {
        TestObserver<String> ts = new TestObserver<String>();
        Observable.just(1).lift(new NbpOperator<String, Integer>() {

            @Override
            public Observer<? super Integer> apply(Observer<? super String> t1) {
                throw new RuntimeException("failed");
            }

        }).onErrorResumeNext(new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Observable.just("success");
                } else {
                    return Observable.error(t1);
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
        TestObserver<String> ts = new TestObserver<String>();
        Observable.just(1).lift(new NbpOperator<String, Integer>() {

            @Override
            public Observer<? super Integer> apply(final Observer<? super String> t1) {
                return new Observer<Integer>() {

                    @Override
                    public void onSubscribe(Disposable s) {
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

        }).onErrorResumeNext(new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Observable.just("success");
                } else {
                    return Observable.error(t1);
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
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");

        // Introduce map function that fails intermittently (Map does not prevent this when the NbpObserver is a
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

        Observable<String> o = w.onErrorResumeNext(new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                return Observable.just("twoResume", "threeResume").subscribeOn(Schedulers.computation());
            }
            
        });

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        o.subscribe(ts);
        ts.awaitTerminalEvent();

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
    }

    private static class TestObservable implements NbpOnSubscribe<String> {

        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.values = values;
        }

        @Override
        public void accept(final Observer<? super String> NbpObserver) {
            System.out.println("TestObservable subscribed to ...");
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            NbpObserver.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        NbpObserver.onError(e);
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
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(0, 100000)
                .onErrorResumeNext(new Function<Throwable, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> apply(Throwable t1) {
                        return Observable.just(1);
                    }

                })
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