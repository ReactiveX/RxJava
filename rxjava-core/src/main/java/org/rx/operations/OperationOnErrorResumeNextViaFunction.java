package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.rx.functions.Func1;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.CompositeException;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IObserver;

final class OperationOnErrorResumeNextViaFunction<T> extends AbstractIObservable<T> {
    private final Func1<IObservable<T>, Exception> resumeFunction;
    private final IObservable<T> originalSequence;

    OperationOnErrorResumeNextViaFunction(IObservable<T> originalSequence, Func1<IObservable<T>, Exception> resumeFunction) {
        this.resumeFunction = resumeFunction;
        this.originalSequence = originalSequence;
    }

    public IDisposable subscribe(IObserver<T> watcher) {
        final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
        final IObserver<T> observer = new AtomicWatcher<T>(watcher, subscription);

        // AtomicReference since we'll be accessing/modifying this across threads so we can switch it if needed
        final AtomicReference<AtomicWatchableSubscription> subscriptionRef = new AtomicReference<AtomicWatchableSubscription>(subscription);

        // subscribe to the original Watchable and remember the subscription
        subscription.setActual(originalSequence.subscribe(new IObserver<T>() {
            public void onNext(T value) {
                // forward the successful calls
                observer.onNext(value);
            }

            /**
             * Instead of passing the onError forward, we intercept and "resume" with the resumeSequence.
             */
            public void onError(Exception ex) {
                /* remember what the current subscription is so we can determine if someone unsubscribes concurrently */
                AtomicWatchableSubscription currentSubscription = subscriptionRef.get();
                // check that we have not been unsubscribed before we can process the error
                if (currentSubscription != null) {
                    try {
                        IObservable<T> resumeSequence = resumeFunction.call(ex);
                        /* error occurred, so switch subscription to the 'resumeSequence' */
                        AtomicWatchableSubscription innerSubscription = new AtomicWatchableSubscription(resumeSequence.subscribe(observer));
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
        }));

        return new IDisposable() {
            public void unsubscribe() {
                // this will get either the original, or the resumeSequence one and unsubscribe on it
                IDisposable s = subscriptionRef.getAndSet(null);
                if (s != null) {
                    s.unsubscribe();
                }
            }
        };
    }

    public static class UnitTest {

        @Test
        public void testResumeNext() {
            final AtomicReference<Exception> receivedException = new AtomicReference<Exception>();
            IDisposable s = mock(IDisposable.class);
            TestWatchable w = new TestWatchable(s, "one");
            Func1<IObservable<String>, Exception> resume = new Func1<IObservable<String>, Exception>() {

                @Override
                public IObservable<String> call(Exception t1) {
                    receivedException.set(t1);
                    return WatchableExtensions.toWatchable("twoResume", "threeResume");
                }

            };
            IObservable<String> watchable = new OperationOnErrorResumeNextViaFunction<String>(w, resume);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            watchable.subscribe(aWatcher);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, never()).onNext("two");
            verify(aWatcher, never()).onNext("three");
            verify(aWatcher, times(1)).onNext("twoResume");
            verify(aWatcher, times(1)).onNext("threeResume");
            assertNotNull(receivedException.get());
        }

        /**
         * Test that when a function throws an exception this is propagated through onError
         */
        @Test
        public void testFunctionThrowsError() {
            IDisposable s = mock(IDisposable.class);
            TestWatchable w = new TestWatchable(s, "one");
            Func1<IObservable<String>, Exception> resume = new Func1<IObservable<String>, Exception>() {

                @Override
                public IObservable<String> call(Exception t1) {
                    throw new RuntimeException("exception from function");
                }

            };
            IObservable<String> watchable = new OperationOnErrorResumeNextViaFunction<String>(w, resume);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            watchable.subscribe(aWatcher);

            try {
                w.t.join();
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }

            // we should get the "one" value before the error
            verify(aWatcher, times(1)).onNext("one");

            // we should have received an onError call on the watcher since the resume function threw an exception
            verify(aWatcher, times(1)).onError(any(Exception.class));
            verify(aWatcher, times(0)).onCompleted();
        }

        private static class TestWatchable extends AbstractIObservable<String> {

            final IDisposable s;
            final String[] values;
            Thread t = null;

            public TestWatchable(IDisposable s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                System.out.println("TestWatchable subscribed to ...");
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("running TestWatchable thread");
                            for (String s : values) {
                                System.out.println("TestWatchable onNext: " + s);
                                observer.onNext(s);
                            }
                            throw new RuntimeException("Forced Failure");
                        } catch (Exception e) {
                            observer.onError(e);
                        }
                    }

                });
                System.out.println("starting TestWatchable thread");
                t.start();
                System.out.println("done starting TestWatchable thread");
                return s;
            }

        }
    }
}