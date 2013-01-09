package org.rx.operations;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Vector;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.Notification;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * Materializes the implicit notifications of an observable sequence as explicit notification values.
 * <p>
 * In other words, converts a sequence of OnNext, OnError and OnCompleted events into a sequence of WatchableNotifications containing the OnNext, OnError and OnCompleted values.
 * <p>
 * See http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx for the Microsoft Rx equivalent.
 */
public class OperationMaterialize {

    /**
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
     */
    public static <T> IObservable<Notification<T>> materialize(final IObservable<T> sequence) {
        return new MaterializeWatchable<T>(sequence);
    }

    private static class MaterializeWatchable<T> extends AbstractIObservable<Notification<T>> {

        private final IObservable<T> sequence;

        public MaterializeWatchable(IObservable<T> sequence) {
            this.sequence = sequence;
        }

        @Override
        public IDisposable subscribe(IObserver<Notification<T>> watcher) {
            final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
            final IObserver<Notification<T>> atomicWatcher = new AtomicWatcher<Notification<T>>(watcher, subscription);

            subscription.setActual(sequence.subscribe(new IObserver<T>() {

                @Override
                public void onCompleted() {
                    atomicWatcher.onNext(new Notification<T>());
                    atomicWatcher.onCompleted();
                }

                @Override
                public void onError(Exception e) {
                    atomicWatcher.onNext(new Notification<T>(e));
                    atomicWatcher.onCompleted();
                }

                @Override
                public void onNext(T value) {
                    atomicWatcher.onNext(new Notification<T>(value));
                }

            }));

            return subscription;
        }

    }

    public static class UnitTest {
        @Test
        public void testMaterialize1() {
            // null will cause onError to be triggered before "three" can be returned
            final TestAsyncErrorWatchable o1 = new TestAsyncErrorWatchable("one", "two", null, "three");

            TestWatcher watcher = new TestWatcher();
            IObservable<Notification<String>> m = materialize(o1);
            m.subscribe(watcher);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertFalse(watcher.onError);
            assertTrue(watcher.onCompleted);
            assertEquals(3, watcher.notifications.size());
            assertEquals("one", watcher.notifications.get(0).getValue());
            assertTrue(watcher.notifications.get(0).isOnNext());
            assertEquals("two", watcher.notifications.get(1).getValue());
            assertTrue(watcher.notifications.get(1).isOnNext());
            assertEquals(NullPointerException.class, watcher.notifications.get(2).getException().getClass());
            assertTrue(watcher.notifications.get(2).isOnError());
        }

        @Test
        public void testMaterialize2() {
            final TestAsyncErrorWatchable o1 = new TestAsyncErrorWatchable("one", "two", "three");

            TestWatcher watcher = new TestWatcher();
            IObservable<Notification<String>> m = materialize(o1);
            m.subscribe(watcher);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertFalse(watcher.onError);
            assertTrue(watcher.onCompleted);
            assertEquals(4, watcher.notifications.size());
            assertEquals("one", watcher.notifications.get(0).getValue());
            assertTrue(watcher.notifications.get(0).isOnNext());
            assertEquals("two", watcher.notifications.get(1).getValue());
            assertTrue(watcher.notifications.get(1).isOnNext());
            assertEquals("three", watcher.notifications.get(2).getValue());
            assertTrue(watcher.notifications.get(2).isOnNext());
            assertTrue(watcher.notifications.get(3).isOnCompleted());
        }

        @Test
        public void testMultipleSubscribes() {
            final TestAsyncErrorWatchable o1 = new TestAsyncErrorWatchable("one", "two", null, "three");

            IObservable<Notification<String>> m = materialize(o1);

            TestWatcher watcher1 = new TestWatcher();
            m.subscribe(watcher1);

            TestWatcher watcher2 = new TestWatcher();
            m.subscribe(watcher2);

            try {
                o1.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals(3, watcher1.notifications.size());
            assertEquals(3, watcher2.notifications.size());
        }

    }

    private static class TestWatcher implements IObserver<Notification<String>> {

        boolean onCompleted = false;
        boolean onError = false;
        List<Notification<String>> notifications = new Vector<Notification<String>>();

        @Override
        public void onCompleted() {
            this.onCompleted = true;
        }

        @Override
        public void onError(Exception e) {
            this.onError = true;
        }

        @Override
        public void onNext(Notification<String> value) {
            this.notifications.add(value);
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
}
