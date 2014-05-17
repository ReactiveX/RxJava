/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class OperatorOnErrorResumeNextViaFunctionTest {

    @Test
    public void testResumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Observable<String> w = Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new Throwable("injected failure"));
            }
        });

        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                receivedException.set(t1);
                return Observable.from("twoResume", "threeResume");
            }

        };
        Observable<String> observable = w.onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
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
        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                receivedException.set(t1);
                return Observable.from("twoResume", "threeResume");
            }

        };
        Observable<String> observable = Observable.create(w).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError
     */
    @Test
    public void testFunctionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one");
        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Observable<String> observable = Observable.create(w).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
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
        verify(observer, times(0)).onCompleted();
    }

    /**
     * Test that we receive the onError if an exception is thrown from an operator that
     * does not have manual try/catch handling like map does.
     */
    @Test
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperator() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(1).lift(new Operator<String, Integer>() {

            @Override
            public Subscriber<? super Integer> call(Subscriber<? super String> t1) {
                throw new RuntimeException("failed");
            }

        }).onErrorResumeNext(new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Observable.from("success");
                } else {
                    return Observable.error(t1);
                }
            }

        }).subscribe(ts);

        ts.assertTerminalEvent();
        System.out.println(ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList("success"));
    }

    /**
     * Test that we receive the onError if an exception is thrown from an operator that
     * does not have manual try/catch handling like map does.
     */
    @Test
    public void testOnErrorResumeReceivesErrorFromPreviousNonProtectedOperatorOnNext() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.from(1).lift(new Operator<String, Integer>() {

            @Override
            public Subscriber<? super Integer> call(Subscriber<? super String> t1) {
                return new Subscriber<Integer>() {

                    @Override
                    public void onCompleted() {
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

        }).onErrorResumeNext(new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                if (t1.getMessage().equals("failed")) {
                    return Observable.from("success");
                } else {
                    return Observable.error(t1);
                }
            }

        }).subscribe(ts);

        ts.assertTerminalEvent();
        System.out.println(ts.getOnNextEvents());
        ts.assertReceivedOnNext(Arrays.asList("success"));
    }

    private static class TestObservable implements Observable.OnSubscribe<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void call(final Subscriber<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.add(s);
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
}
