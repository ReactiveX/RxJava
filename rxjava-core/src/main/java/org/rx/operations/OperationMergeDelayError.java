package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.CompositeException;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * Same functionality as OperationMerge except that onError events will be skipped so that all onNext calls are passed on until all sequences finish with onComplete or onError, and then the first onError received (if any) will be passed on.
 * <p>
 * This allows retrieving all successful onNext calls without being blocked by an onError early in a sequence.
 * <p>
 * NOTE: If this is used on an infinite stream it will never call onError and effectively will swallow errors.
 */
/* package */class OperationMergeDelayError {

    /**
     * Flattens the observable sequences from the list of IObservables into one observable sequence without any transformation and delays any onError calls until after all sequences have called onError or onComplete so as to allow all successful
     * onNext calls to be received.
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of flattening the output from the list of IObservables.
     * @see http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx
     */
    public static <T> IObservable<T> mergeDelayError(final IObservable<IObservable<T>> sequences) {
        // wrap in a Watchable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new AbstractIObservable<T>() {

            @Override
            public IDisposable subscribe(IObserver<T> watcher) {
                AtomicWatchableSubscription s = new AtomicWatchableSubscription();
                s.setActual(new MergeDelayErrorObservable<T>(sequences).subscribe(new AtomicWatcher<T>(watcher, s)));
                return s;
            }
        };
    }

    public static <T> IObservable<T> mergeDelayError(final IObservable<T>... sequences) {
        return mergeDelayError(new AbstractIObservable<IObservable<T>>() {
            private volatile boolean unsubscribed = false;

            @Override
            public IDisposable subscribe(IObserver<IObservable<T>> observer) {
                for (IObservable<T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }
                return new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        });
    }

    public static <T> IObservable<T> mergeDelayError(final List<IObservable<T>> sequences) {
        return mergeDelayError(new AbstractIObservable<IObservable<T>>() {

            private volatile boolean unsubscribed = false;

            @Override
            public IDisposable subscribe(IObserver<IObservable<T>> observer) {
                for (IObservable<T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        });
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Watchable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static final class MergeDelayErrorObservable<T> extends AbstractIObservable<T> {
        private final IObservable<IObservable<T>> sequences;
        private final Subscription ourSubscription = new Subscription();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private volatile boolean parentCompleted = false;
        private final ConcurrentHashMap<ChildWatcher, ChildWatcher> childWatchers = new ConcurrentHashMap<ChildWatcher, ChildWatcher>();
        private final ConcurrentHashMap<ChildWatcher, IDisposable> childSubscriptions = new ConcurrentHashMap<ChildWatcher, IDisposable>();
        // onErrors we received that will be delayed until everything is completed and then sent
        private ConcurrentLinkedQueue<Exception> onErrorReceived = new ConcurrentLinkedQueue<Exception>();

        private MergeDelayErrorObservable(IObservable<IObservable<T>> sequences) {
            this.sequences = sequences;
        }

        public IDisposable subscribe(IObserver<T> actualWatcher) {
            /**
             * Subscribe to the parent Watchable to get to the children Watchables
             */
            sequences.subscribe(new ParentWatcher(actualWatcher));

            /* return our subscription to allow unsubscribing */
            return ourSubscription;
        }

        /**
         * Manage the internal subscription with a thread-safe means of stopping/unsubscribing so we don't unsubscribe twice.
         * <p>
         * Also has the stop() method returning a boolean so callers know if their thread "won" and should perform further actions.
         */
        private class Subscription implements IDisposable {

            @Override
            public void unsubscribe() {
                stop();
            }

            public boolean stop() {
                // try setting to false unless another thread beat us
                boolean didSet = stopped.compareAndSet(false, true);
                if (didSet) {
                    // this thread won the race to stop, so unsubscribe from the actualSubscription
                    for (IDisposable _s : childSubscriptions.values()) {
                        _s.unsubscribe();
                    }
                    return true;
                } else {
                    // another thread beat us
                    return false;
                }
            }
        }

        /**
         * Subscribe to the top level Watchable to receive the sequence of Watchable<T> children.
         * 
         * @param <T>
         */
        private class ParentWatcher implements IObserver<IObservable<T>> {
            private final IObserver<T> actualWatcher;

            public ParentWatcher(IObserver<T> actualWatcher) {
                this.actualWatcher = actualWatcher;
            }

            @Override
            public void onCompleted() {
                parentCompleted = true;
                // this *can* occur before the children are done, so if it does we won't send onCompleted
                // but will let the child worry about it
                // if however this completes and there are no children processing, then we will send onCompleted

                if (childWatchers.size() == 0) {
                    if (!stopped.get()) {
                        if (ourSubscription.stop()) {
                            if (onErrorReceived.size() == 1) {
                                // an onError was received from 1 ChildWatcher so we now send it as a delayed error
                                actualWatcher.onError(onErrorReceived.peek());
                            } else if (onErrorReceived.size() > 1) {
                                // an onError was received from more than 1 ChildWatcher so we now send it as a delayed error
                                actualWatcher.onError(new CompositeException(onErrorReceived));
                            } else {
                                // no delayed error so send onCompleted
                                actualWatcher.onCompleted();
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                actualWatcher.onError(e);
            }

            @Override
            public void onNext(IObservable<T> childWatchable) {
                if (stopped.get()) {
                    // we won't act on any further items
                    return;
                }

                if (childWatchable == null) {
                    throw new IllegalArgumentException("Watchable<T> can not be null.");
                }

                /**
                 * For each child Watchable we receive we'll subscribe with a separate Watcher
                 * that will each then forward their sequences to the actualWatcher.
                 * <p>
                 * We use separate child watchers for each sequence to simplify the onComplete/onError handling so each sequence has its own lifecycle.
                 */
                ChildWatcher _w = new ChildWatcher(actualWatcher);
                childWatchers.put(_w, _w);
                IDisposable _subscription = childWatchable.subscribe(_w);
                // remember this watcher and the subscription from it
                childSubscriptions.put(_w, _subscription);
            }
        }

        /**
         * Subscribe to each child Watchable<T> and forward their sequence of data to the actualWatcher
         * 
         */
        private class ChildWatcher implements IObserver<T> {

            private final IObserver<T> actualWatcher;
            private volatile boolean finished = false;

            public ChildWatcher(IObserver<T> actualWatcher) {
                this.actualWatcher = actualWatcher;
            }

            @Override
            public void onCompleted() {
                // remove self from map of watchers
                childWatchers.remove(this);
                // if there are now 0 watchers left, so if the parent is also completed we send the onComplete to the actualWatcher
                // if the parent is not complete that means there is another sequence (and child watcher) to come
                if (!stopped.get()) {
                    finishWatcher();
                }
            }

            @Override
            public void onError(Exception e) {
                if (!stopped.get()) {
                    onErrorReceived.add(e);
                    // mark this ChildWatcher as done
                    childWatchers.remove(this);
                    // but do NOT forward to actualWatcher as we want other ChildWatchers to continue until completion
                    // and we'll delay the sending of onError until all others are done

                    // we mark finished==true as a safety to ensure that if further calls to onNext occur we ignore them
                    finished = true;

                    // check for whether the parent is completed and if so then perform the 'finishing' actions
                    finishWatcher();
                }
            }

            /**
             * onComplete and onError when called need to check for the parent being complete and if so send the onCompleted or onError to the actualWatcher.
             * <p>
             * This does NOT get invoked if synchronous execution occurs, but will when asynchronously executing.
             * <p>
             * TestCase testErrorDelayed4WithThreading specifically tests this use case.
             */
            private void finishWatcher() {
                if (childWatchers.size() == 0 && parentCompleted) {
                    if (ourSubscription.stop()) {
                        // this thread 'won' the race to unsubscribe/stop so let's send onError or onCompleted
                        if (onErrorReceived.size() == 1) {
                            // an onError was received from 1 ChildWatcher so we now send it as a delayed error
                            actualWatcher.onError(onErrorReceived.peek());
                        } else if (onErrorReceived.size() > 1) {
                            // an onError was received from more than 1 ChildWatcher so we now send it as a delayed error
                            actualWatcher.onError(new CompositeException(onErrorReceived));
                        } else {
                            // no delayed error so send onCompleted
                            actualWatcher.onCompleted();
                        }
                    }
                }
            }

            @Override
            public void onNext(T args) {
                // in case the Watchable is poorly behaved and doesn't listen to the unsubscribe request
                // we'll ignore anything that comes in after we've unsubscribed or an onError has been received and delayed
                if (!stopped.get() && !finished) {
                    actualWatcher.onNext(args);
                }
            }

        }
    }

    public static class UnitTest {
        @Mock
        IObserver<String> stringObserver;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testErrorDelayed1() {
            final IObservable<String> o1 = new TestErrorWatchable("four", null, "six"); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final IObservable<String> o2 = new TestErrorWatchable("one", "two", "three");

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2);
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
        }

        @Test
        public void testErrorDelayed2() {
            final IObservable<String> o1 = new TestErrorWatchable("one", "two", "three");
            final IObservable<String> o2 = new TestErrorWatchable("four", null, "six"); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final IObservable<String> o3 = new TestErrorWatchable("seven", "eight", null);
            final IObservable<String> o4 = new TestErrorWatchable("nine");

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2, o3, o4);
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed3() {
            final IObservable<String> o1 = new TestErrorWatchable("one", "two", "three");
            final IObservable<String> o2 = new TestErrorWatchable("four", "five", "six");
            final IObservable<String> o3 = new TestErrorWatchable("seven", "eight", null);
            final IObservable<String> o4 = new TestErrorWatchable("nine");

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2, o3, o4);
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed4() {
            final IObservable<String> o1 = new TestErrorWatchable("one", "two", "three");
            final IObservable<String> o2 = new TestErrorWatchable("four", "five", "six");
            final IObservable<String> o3 = new TestErrorWatchable("seven", "eight");
            final IObservable<String> o4 = new TestErrorWatchable("nine", null);

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2, o3, o4);
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed4WithThreading() {
            final TestAsyncErrorWatchable o1 = new TestAsyncErrorWatchable("one", "two", "three");
            final TestAsyncErrorWatchable o2 = new TestAsyncErrorWatchable("four", "five", "six");
            final TestAsyncErrorWatchable o3 = new TestAsyncErrorWatchable("seven", "eight");
            // throw the error at the very end so no onComplete will be called after it
            final TestAsyncErrorWatchable o4 = new TestAsyncErrorWatchable("nine", null);

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2, o3, o4);
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
                o3.t.join();
                o4.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testCompositeErrorDelayed1() {
            final IObservable<String> o1 = new TestErrorWatchable("four", null, "six"); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final IObservable<String> o2 = new TestErrorWatchable("one", "two", null);

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2);
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(CompositeException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(0)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
        }

        @Test
        public void testCompositeErrorDelayed2() {
            final IObservable<String> o1 = new TestErrorWatchable("four", null, "six"); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final IObservable<String> o2 = new TestErrorWatchable("one", "two", null);

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2);
            CaptureWatcher w = new CaptureWatcher();
            m.subscribe(w);

            assertNotNull(w.e);
            if (w.e instanceof CompositeException) {
                assertEquals(2, ((CompositeException) w.e).getExceptions().size());
                w.e.printStackTrace();
            } else {
                fail("Expecting CompositeException");
            }

        }

        /**
         * The unit tests below are from OperationMerge and should ensure the normal merge functionality is correct.
         */

        @Test
        public void testMergeObservableOfObservables() {
            final IObservable<String> o1 = new TestSynchronousWatchable();
            final IObservable<String> o2 = new TestSynchronousWatchable();

            IObservable<IObservable<String>> observableOfObservables = new AbstractIObservable<IObservable<String>>() {

                @Override
                public IDisposable subscribe(IObserver<IObservable<String>> observer) {
                    // simulate what would happen in an observable
                    observer.onNext(o1);
                    observer.onNext(o2);
                    observer.onCompleted();

                    return new IDisposable() {

                        @Override
                        public void unsubscribe() {
                            // unregister ... will never be called here since we are executing synchronously
                        }

                    };
                }

            };
            IObservable<String> m = mergeDelayError(observableOfObservables);
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testMergeArray() {
            final IObservable<String> o1 = new TestSynchronousWatchable();
            final IObservable<String> o2 = new TestSynchronousWatchable();

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2);
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMergeList() {
            final IObservable<String> o1 = new TestSynchronousWatchable();
            final IObservable<String> o2 = new TestSynchronousWatchable();
            List<IObservable<String>> listOfObservables = new ArrayList<IObservable<String>>();
            listOfObservables.add(o1);
            listOfObservables.add(o2);

            IObservable<String> m = mergeDelayError(listOfObservables);
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testUnSubscribe() {
            TestWatchable tA = new TestWatchable();
            TestWatchable tB = new TestWatchable();

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(tA, tB);
            IDisposable s = m.subscribe(stringObserver);

            tA.sendOnNext("Aone");
            tB.sendOnNext("Bone");
            s.unsubscribe();
            tA.sendOnNext("Atwo");
            tB.sendOnNext("Btwo");
            tA.sendOnCompleted();
            tB.sendOnCompleted();

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("Aone");
            verify(stringObserver, times(1)).onNext("Bone");
            assertTrue(tA.unsubscribed);
            assertTrue(tB.unsubscribed);
            verify(stringObserver, never()).onNext("Atwo");
            verify(stringObserver, never()).onNext("Btwo");
            verify(stringObserver, never()).onCompleted();
        }

        @Test
        public void testMergeArrayWithThreading() {
            final TestASynchronousWatchable o1 = new TestASynchronousWatchable();
            final TestASynchronousWatchable o2 = new TestASynchronousWatchable();

            @SuppressWarnings("unchecked")
            IObservable<String> m = mergeDelayError(o1, o2);
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        private static class TestSynchronousWatchable extends AbstractIObservable<String> {

            @Override
            public IDisposable subscribe(IObserver<String> observer) {

                observer.onNext("hello");
                observer.onCompleted();

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static class TestASynchronousWatchable extends AbstractIObservable<String> {
            Thread t;

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        observer.onNext("hello");
                        observer.onCompleted();
                    }

                });
                t.start();

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {

                    }

                };
            }
        }

        /**
         * A Watchable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
         */
        private static class TestWatchable extends AbstractIObservable<String> {

            IObserver<String> observer = null;
            volatile boolean unsubscribed = false;
            IDisposable s = new IDisposable() {

                @Override
                public void unsubscribe() {
                    unsubscribed = true;

                }

            };

            public TestWatchable() {
            }

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            @SuppressWarnings("unused")
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                this.observer = observer;
                return s;
            }
        }

        private static class TestErrorWatchable extends AbstractIObservable<String> {

            String[] valuesToReturn;

            TestErrorWatchable(String... values) {
                valuesToReturn = values;
            }

            @Override
            public IDisposable subscribe(IObserver<String> observer) {
                boolean errorThrown = false;
                for (String s : valuesToReturn) {
                    if (s == null) {
                        System.out.println("throwing exception");
                        observer.onError(new NullPointerException());
                        errorThrown = true;
                        // purposefully not returning here so it will continue calling onNext
                        // so that we also test that we handle bad sequences like this
                    } else {
                        observer.onNext(s);
                    }
                }
                if (!errorThrown) {
                    observer.onCompleted();
                }

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static class TestAsyncErrorWatchable extends AbstractIObservable<String> {

            String[] valuesToReturn;

            TestAsyncErrorWatchable(String... values) {
                valuesToReturn = values;
            }

            Thread t;

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (String s : valuesToReturn) {
                            if (s == null) {
                                System.out.println("throwing exception");
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {

                                }
                                observer.onError(new NullPointerException());
                                return;
                            } else {
                                observer.onNext(s);
                            }
                        }
                        System.out.println("subscription complete");
                        observer.onCompleted();
                    }

                });
                t.start();

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {

                    }

                };
            }
        }

        private static class CaptureWatcher implements IObserver<String> {
            volatile Exception e;

            @Override
            public void onCompleted() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onError(Exception e) {
                this.e = e;
            }

            @Override
            public void onNext(String args) {
                // TODO Auto-generated method stub

            }

        }
    }
}
