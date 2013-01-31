/**
 * Copyright 2013 Netflix, Inc.
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.CompositeException;
import rx.util.functions.Func1;

public final class OperationOnErrorResumeNextViaFunction<T> {

    public static <T> Func1<Observer<T>, Subscription> onErrorResumeNextViaFunction(Observable<T> originalSequence, Func1<Exception, Observable<T>> resumeFunction) {
        return new OnErrorResumeNextViaFunction<T>(originalSequence, resumeFunction);
    }

    private static class OnErrorResumeNextViaFunction<T> implements Func1<Observer<T>, Subscription> {

        private final Func1<Exception, Observable<T>> resumeFunction;
        private final Observable<T> originalSequence;

        public OnErrorResumeNextViaFunction(Observable<T> originalSequence, Func1<Exception, Observable<T>> resumeFunction) {
            this.resumeFunction = resumeFunction;
            this.originalSequence = originalSequence;
        }

        public Subscription call(final Observer<T> observer) {
            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<AtomicObservableSubscription> subscriptionRef = new AtomicReference<AtomicObservableSubscription>(new AtomicObservableSubscription());

            // subscribe to the original Observable and remember the subscription
            subscriptionRef.get().wrap(new AtomicObservableSubscription(originalSequence.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // forward the successful calls
                    observer.onNext(value);
                }

                /**
                 * Instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
                 */
                public void onError(Exception ex) {
                    /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                    AtomicObservableSubscription currentSubscription = subscriptionRef.get();
                    // check that we have not been unsubscribed before we can process the error
                    if (currentSubscription != null) {
                        try {
                            Observable<T> resumeSequence = resumeFunction.call(ex);
                            /* error occurred, so switch subscription to the 'resumeSequence' */
                            AtomicObservableSubscription innerSubscription = new AtomicObservableSubscription(resumeSequence.subscribe(observer));
                            /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                            if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                                // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                                // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                                innerSubscription.unsubscribe();
                            }
                        } catch (Exception e) {
                            // the resume function failed so we need to call onError
                            // I am using CompositeException so that both exceptions can be seen
                            observer.onError(new CompositeException("OnErrorResume function failed", Arrays.asList(ex, e)));
                        }
                    }
                }

                public void onCompleted() {
                    // forward the successful calls
                    observer.onCompleted();
                }
            })));

            return new Subscription() {
                public void unsubscribe() {
                    // this will get either the original, or the resumeSequence one and unsubscribe on it
                    Subscription s = subscriptionRef.getAndSet(null);
                    if (s != null) {
                        s.unsubscribe();
                    }
                }
            };
        }
    }

    public static class UnitTest {

        @Test
        public void testResumeNextWithSynchronousExecution() {
            final AtomicReference<Exception> receivedException = new AtomicReference<Exception>();
            Observable<String> w = Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onError(new Exception("injected failure"));
                    return Observable.noOpSubscription();
                }
            });

            Func1<Exception, Observable<String>> resume = new Func1<Exception, Observable<String>>() {

                @Override
                public Observable<String> call(Exception t1) {
                    receivedException.set(t1);
                    return Observable.toObservable("twoResume", "threeResume");
                }

            };
            Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, times(1)).onNext("twoResume");
            verify(aObserver, times(1)).onNext("threeResume");
            assertNotNull(receivedException.get());
        }

        @Test
        public void testResumeNextWithAsyncExecution() {
            final AtomicReference<Exception> receivedException = new AtomicReference<Exception>();
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one");
            Func1<Exception, Observable<String>> resume = new Func1<Exception, Observable<String>>() {

                @Override
                public Observable<String> call(Exception t1) {
                    receivedException.set(t1);
                    return Observable.toObservable("twoResume", "threeResume");
                }

            };
            Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, times(1)).onNext("twoResume");
            verify(aObserver, times(1)).onNext("threeResume");
            assertNotNull(receivedException.get());
        }

        /**
         * Test that when a function throws an exception this is propagated through onError
         */
        @Test
        public void testFunctionThrowsError() {
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one");
            Func1<Exception, Observable<String>> resume = new Func1<Exception, Observable<String>>() {

                @Override
                public Observable<String> call(Exception t1) {
                    throw new RuntimeException("exception from function");
                }

            };
            Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            // we should get the "one" value before the error
            verify(aObserver, times(1)).onNext("one");

            // we should have received an onError call on the Observer since the resume function threw an exception
            verify(aObserver, times(1)).onError(any(Exception.class));
            verify(aObserver, times(0)).onCompleted();
        }

        private static class TestObservable extends Observable<String> {

            final Subscription s;
            final String[] values;
            Thread t = null;

            public TestObservable(Subscription s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                System.out.println("TestObservable subscribed to ...");
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
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }

                });
                System.out.println("starting TestObservable thread");
                t.start();
                System.out.println("done starting TestObservable thread");
                return s;
            }

        }
    }
}