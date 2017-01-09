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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;
import org.mockito.Mockito;
import org.reactivestreams.Subscription;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class ObservableOnErrorResumeNextViaFunctionTest {

    @Test
    public void testResumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Observable<String> w = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext("one");
                observer.onError(new Throwable("injected failure"));
                observer.onNext("two");
                observer.onNext("three");
            }
        });

        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Observable.just("twoResume", "threeResume");
            }

        };
        Observable<String> observable = w.onErrorResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();

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
        TestObservable w = new TestObservable(s, "one");
        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                receivedException.set(t1);
                return Observable.just("twoResume", "threeResume");
            }

        };
        Observable<String> o = Observable.unsafeCreate(w).onErrorResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();

        o.subscribe(observer);

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
        TestObservable w = new TestObservable(s, "one");
        Function<Throwable, Observable<String>> resume = new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Observable<String> o = Observable.unsafeCreate(w).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        DefaultObserver<String> observer = mock(DefaultObserver.class);
        o.subscribe(observer);

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
    @Ignore("Failed operator may leave the child Observer in an inconsistent state which prevents further error delivery.")
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperator() {
        TestObserver<String> ts = new TestObserver<String>();
        Observable.just(1).lift(new ObservableOperator<String, Integer>() {

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
        Observable.just(1).lift(new ObservableOperator<String, Integer>() {

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

        // Introduce map function that fails intermittently (Map does not prevent this when the Observer is a
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

        Observable<String> o = w.onErrorResumeNext(new Function<Throwable, Observable<String>>() {

            @Override
            public Observable<String> apply(Throwable t1) {
                return Observable.just("twoResume", "threeResume").subscribeOn(Schedulers.computation());
            }

        });

        @SuppressWarnings("unchecked")
        DefaultObserver<String> observer = mock(DefaultObserver.class);

        TestObserver<String> ts = new TestObserver<String>(observer);
        o.subscribe(ts);
        ts.awaitTerminalEvent();

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    static class TestObservable implements ObservableSource<String> {

        final String[] values;
        Thread t;

        TestObservable(Subscription s, String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.onSubscribe(Disposables.empty());
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
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
    public void badOtherSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> o) throws Exception {
                return Observable.error(new IOException())
                        .onErrorResumeNext(Functions.justFunction(o));
            }
        }, false, 1, 1, 1);
    }
}
