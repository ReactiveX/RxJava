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

/**
 * When an onError occurs the resumeFunction will be executed and it's response passed to onNext instead of calling onError.
 */
public final class OperationOnErrorReturn<T> {

    public static <T> Func1<Observer<T>, Subscription> onErrorReturn(Observable<T> originalSequence, Func1<Exception, T> resumeFunction) {
        return new OnErrorReturn<T>(originalSequence, resumeFunction);
    }

    private static class OnErrorReturn<T> implements Func1<Observer<T>, Subscription> {
        private final Func1<Exception, T> resumeFunction;
        private final Observable<T> originalSequence;

        public OnErrorReturn(Observable<T> originalSequence, Func1<Exception, T> resumeFunction) {
            this.resumeFunction = resumeFunction;
            this.originalSequence = originalSequence;
        }

        public Subscription call(final Observer<T> observer) {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<AtomicObservableSubscription> subscriptionRef = new AtomicReference<AtomicObservableSubscription>(subscription);

            // subscribe to the original Observable and remember the subscription
            subscription.wrap(originalSequence.subscribe(new Observer<T>() {
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
                            /* error occurred, so execute the function, give it the exception and call onNext with the response */
                            onNext(resumeFunction.call(ex));
                            /*
                             * we are not handling an exception thrown from this function ... should we do something?
                             * error handling within an error handler is a weird one to determine what we should do
                             * right now I'm going to just let it throw whatever exceptions occur (such as NPE)
                             * but I'm considering calling the original Observer.onError to act as if this OnErrorReturn operator didn't happen
                             */

                            /* we are now completed */
                            onCompleted();

                            /* unsubscribe since it blew up */
                            currentSubscription.unsubscribe();
                        } catch (Exception e) {
                            // the return function failed so we need to call onError
                            // I am using CompositeException so that both exceptions can be seen
                            observer.onError(new CompositeException("OnErrorReturn function failed", Arrays.asList(ex, e)));
                        }
                    }
                }

                public void onCompleted() {
                    // forward the successful calls
                    observer.onCompleted();
                }
            }));

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
        public void testResumeNext() {
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one");
            final AtomicReference<Exception> capturedException = new AtomicReference<Exception>();

            Observable<String> observable = Observable.create(onErrorReturn(w, new Func1<Exception, String>() {

                @Override
                public String call(Exception e) {
                    capturedException.set(e);
                    return "failure";
                }

            }));

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
            verify(aObserver, times(1)).onNext("failure");
            assertNotNull(capturedException.get());
        }

        /**
         * Test that when a function throws an exception this is propagated through onError
         */
        @Test
        public void testFunctionThrowsError() {
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one");
            final AtomicReference<Exception> capturedException = new AtomicReference<Exception>();

            Observable<String> observable = Observable.create(onErrorReturn(w, new Func1<Exception, String>() {

                @Override
                public String call(Exception e) {
                    capturedException.set(e);
                    throw new RuntimeException("exception from function");
                }

            }));

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
            assertNotNull(capturedException.get());
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