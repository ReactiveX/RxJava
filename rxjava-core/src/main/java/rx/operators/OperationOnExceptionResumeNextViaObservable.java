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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Instruct an Observable to pass control to another Observable rather than invoking
 * <code>onError</code> if it encounters an error of type {@link java.lang.Exception}.
 * <p>
 * This differs from {@link Observable#onErrorResumeNext} in that this one does not handle {@link java.lang.Throwable} or {@link java.lang.Error} but lets those continue through.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorResumeNext.png">
 * <p>
 * By default, when an Observable encounters an error that prevents it from emitting the expected
 * item to its Observer, the Observable invokes its Observer's <code>onError</code> method, and
 * then quits without invoking any more of its Observer's methods. The onErrorResumeNext operation
 * changes this behavior. If you pass an Observable (resumeSequence) to onErrorResumeNext, if the
 * source Observable encounters an error, instead of invoking its Observer's <code>onError</code>
 * method, it will instead relinquish control to this new Observable, which will invoke the
 * Observer's <code>onNext</code> method if it is able to do so. In such a case, because no
 * Observable necessarily invokes <code>onError</code>, the Observer may never know that an error
 * happened.
 * <p>
 * You can use this to prevent errors from propagating or to supply fallback data should errors be
 * encountered.
 */
public final class OperationOnExceptionResumeNextViaObservable<T> {

    public static <T> Func1<Observer<T>, Subscription> onExceptionResumeNextViaObservable(Observable<T> originalSequence, Observable<T> resumeSequence) {
        return new OnExceptionResumeNextViaObservable<T>(originalSequence, resumeSequence);
    }

    private static class OnExceptionResumeNextViaObservable<T> implements Func1<Observer<T>, Subscription> {

        private final Observable<T> resumeSequence;
        private final Observable<T> originalSequence;

        public OnExceptionResumeNextViaObservable(Observable<T> originalSequence, Observable<T> resumeSequence) {
            this.resumeSequence = resumeSequence;
            this.originalSequence = originalSequence;
        }

        public Subscription call(final Observer<T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();

            // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
            final AtomicReference<SafeObservableSubscription> subscriptionRef = new AtomicReference<SafeObservableSubscription>(subscription);

            // subscribe to the original Observable and remember the subscription
            subscription.wrap(originalSequence.subscribe(new Observer<T>() {
                public void onNext(T value) {
                    // forward the successful calls unless resumed
                    if (subscriptionRef.get() == subscription)
                        observer.onNext(value);
                }

                /**
                 * When we receive java.lang.Exception, instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
                 * For Throwable and Error we pass thru.
                 */
                public void onError(Throwable ex) {
                    if (ex instanceof Exception) {
                        /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                        SafeObservableSubscription currentSubscription = subscriptionRef.get();
                        // check that we have not been unsubscribed and not already resumed before we can process the error
                        if (currentSubscription == subscription) {
                            /* error occurred, so switch subscription to the 'resumeSequence' */
                            SafeObservableSubscription innerSubscription = new SafeObservableSubscription(resumeSequence.subscribe(observer));
                            /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                            if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                                // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                                // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                                innerSubscription.unsubscribe();
                            }
                        }
                    } else {
                        observer.onError(ex);
                    }
                }

                public void onCompleted() {
                    // forward the successful calls unless resumed
                    if (subscriptionRef.get() == subscription)
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
        public void testResumeNextWithException() {
            Subscription s = mock(Subscription.class);
            // Trigger failure on second element
            TestObservable w = new TestObservable(s, "one", "EXCEPTION", "two", "three");
            Observable<String> resume = Observable.from("twoResume", "threeResume");
            Observable<String> observable = Observable.create(onExceptionResumeNextViaObservable(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, times(1)).onNext("twoResume");
            verify(aObserver, times(1)).onNext("threeResume");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
            verifyNoMoreInteractions(aObserver);
        }

        @Test
        public void testResumeNextWithRuntimeException() {
            Subscription s = mock(Subscription.class);
            // Trigger failure on second element
            TestObservable w = new TestObservable(s, "one", "RUNTIMEEXCEPTION", "two", "three");
            Observable<String> resume = Observable.from("twoResume", "threeResume");
            Observable<String> observable = Observable.create(onExceptionResumeNextViaObservable(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, times(1)).onNext("twoResume");
            verify(aObserver, times(1)).onNext("threeResume");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
            verifyNoMoreInteractions(aObserver);
        }

        @Test
        public void testThrowablePassesThru() {
            Subscription s = mock(Subscription.class);
            // Trigger failure on second element
            TestObservable w = new TestObservable(s, "one", "THROWABLE", "two", "three");
            Observable<String> resume = Observable.from("twoResume", "threeResume");
            Observable<String> observable = Observable.create(onExceptionResumeNextViaObservable(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onNext("twoResume");
            verify(aObserver, never()).onNext("threeResume");
            verify(aObserver, times(1)).onError(any(Throwable.class));
            verify(aObserver, never()).onCompleted();
            verifyNoMoreInteractions(aObserver);
        }

        @Test
        public void testErrorPassesThru() {
            Subscription s = mock(Subscription.class);
            // Trigger failure on second element
            TestObservable w = new TestObservable(s, "one", "ERROR", "two", "three");
            Observable<String> resume = Observable.from("twoResume", "threeResume");
            Observable<String> observable = Observable.create(onExceptionResumeNextViaObservable(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onNext("twoResume");
            verify(aObserver, never()).onNext("threeResume");
            verify(aObserver, times(1)).onError(any(Throwable.class));
            verify(aObserver, never()).onCompleted();
            verifyNoMoreInteractions(aObserver);
        }

        @Test
        public void testMapResumeAsyncNext() {
            Subscription sr = mock(Subscription.class);
            // Trigger multiple failures
            Observable<String> w = Observable.from("one", "fail", "two", "three", "fail");
            // Resume Observable is async
            TestObservable resume = new TestObservable(sr, "twoResume", "threeResume");

            // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
            //  rx.operator incl onErrorResumeNextViaObservable)
            w = w.map(new Func1<String, String>() {
                public String call(String s) {
                    if ("fail".equals(s))
                        throw new RuntimeException("Forced Failure");
                    System.out.println("BadMapper:" + s);
                    return s;
                }
            });

            Observable<String> observable = Observable.create(onExceptionResumeNextViaObservable(w, resume));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            try {
                // if the thread gets started (which it shouldn't if it's working correctly)
                if (resume.t != null) {
                    resume.t.join();
                }
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, times(1)).onNext("twoResume");
            verify(aObserver, times(1)).onNext("threeResume");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
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
                                if ("EXCEPTION".equals(s))
                                    throw new Exception("Forced Exception");
                                else if ("RUNTIMEEXCEPTION".equals(s))
                                    throw new RuntimeException("Forced RuntimeException");
                                else if ("ERROR".equals(s))
                                    throw new Error("Forced Error");
                                else if ("THROWABLE".equals(s))
                                    throw new Throwable("Forced Throwable");
                                System.out.println("TestObservable onNext: " + s);
                                observer.onNext(s);
                            }
                            System.out.println("TestObservable onCompleted");
                            observer.onCompleted();
                        } catch (Throwable e) {
                            System.out.println("TestObservable onError: " + e);
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
