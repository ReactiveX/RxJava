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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.CompositeException;
import rx.util.functions.Func1;

/**
 * Instruct an Observable to pass control to another Observable (the return value of a function)
 * rather than invoking <code>onError</code> if it encounters an error.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorResumeNext.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and
 * then quits without invoking any more of its Observer's methods. The onErrorResumeNext operation
 * changes this behavior. If you pass a function that returns an Observable (resumeFunction) to
 * onErrorResumeNext, if the source Observable encounters an error, instead of invoking its
 * Observer's <code>onError</code> method, it will instead relinquish control to this new
 * Observable, which will invoke the Observer's <code>onNext</code> method if it is able to do so.
 * In such a case, because no Observable necessarily invokes <code>onError</code>, the Observer may
 * never know that an error happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperationOnErrorResumeNextViaFunction<T> {

    public static <T> OnSubscribeFunc<T> onErrorResumeNextViaFunction(Observable<? extends T> originalSequence, Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) {
        return new OnErrorResumeNextViaFunction<T>(originalSequence, resumeFunction);
    }

    private static class OnErrorResumeNextViaFunction<T> implements OnSubscribeFunc<T> {

        private final Func1<Throwable, ? extends Observable<? extends T>> resumeFunction;
        private final Observable<? extends T> originalSequence;

        public OnErrorResumeNextViaFunction(Observable<? extends T> originalSequence, Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) {
            this.resumeFunction = resumeFunction;
            this.originalSequence = originalSequence;
        }

        public Subscription onSubscribe(final Observer<? super T> observer) {
            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<SafeObservableSubscription> subscriptionRef = new AtomicReference<SafeObservableSubscription>(new SafeObservableSubscription());

            // subscribe to the original Observable and remember the subscription
            subscriptionRef.get().wrap(new SafeObservableSubscription(originalSequence.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // forward the successful calls
                    observer.onNext(value);
                }

                /**
                 * Instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
                 */
                public void onError(Throwable ex) {
                    /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                    SafeObservableSubscription currentSubscription = subscriptionRef.get();
                    // check that we have not been unsubscribed before we can process the error
                    if (currentSubscription != null) {
                        try {
                            Observable<? extends T> resumeSequence = resumeFunction.call(ex);
                            /* error occurred, so switch subscription to the 'resumeSequence' */
                            SafeObservableSubscription innerSubscription = new SafeObservableSubscription(resumeSequence.subscribe(observer));
                            /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                            if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                                // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                                // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                                innerSubscription.unsubscribe();
                            }
                        } catch (Throwable e) {
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
            final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
            Observable<String> w = Observable.create(new OnSubscribeFunc<String>() {

                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    observer.onNext("one");
                    observer.onError(new Throwable("injected failure"));
                    return Subscriptions.empty();
                }
            });

            Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

                @Override
                public Observable<String> call(Throwable t1) {
                    receivedException.set(t1);
                    return Observable.from("twoResume", "threeResume");
                }

            };
            Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
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
            Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
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
            Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

                @Override
                public Observable<String> call(Throwable t1) {
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
            verify(aObserver, times(1)).onError(any(Throwable.class));
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
            public Subscription subscribe(final Observer<? super String> observer) {
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
                        } catch (Throwable e) {
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
