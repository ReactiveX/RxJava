package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


final class OperationOnErrorResumeNextViaWatchable<T> extends AbstractIObservable<T> {
    private final IObservable<T> resumeSequence;
    private final IObservable<T> originalSequence;

    OperationOnErrorResumeNextViaWatchable(IObservable<T> originalSequence, IObservable<T> resumeSequence) {
        this.resumeSequence = resumeSequence;
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
                    /* error occurred, so switch subscription to the 'resumeSequence' */
                    AtomicWatchableSubscription innerSubscription = new AtomicWatchableSubscription(resumeSequence.subscribe(observer));
                    /* we changed the sequence, so also change the subscription to the one of the 'resumeSequence' instead */
                    if (!subscriptionRef.compareAndSet(currentSubscription, innerSubscription)) {
                        // we failed to set which means 'subscriptionRef' was set to NULL via the unsubscribe below
                        // so we want to immediately unsubscribe from the resumeSequence we just subscribed to
                        innerSubscription.unsubscribe();
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
            IDisposable s = mock(IDisposable.class);
            TestWatchable w = new TestWatchable(s, "one");
            IObservable<String> resume = WatchableExtensions.toWatchable("twoResume", "threeResume");
            IObservable<String> watchable = new OperationOnErrorResumeNextViaWatchable<String>(w, resume);

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