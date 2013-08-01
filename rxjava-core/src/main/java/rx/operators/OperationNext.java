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
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.Exceptions;
import rx.util.functions.Func1;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
 */
public final class OperationNext {

    public static <T> Iterable<T> next(final Observable<T> items) {

        NextObserver<T> nextObserver = new NextObserver<T>();
        final NextIterator<T> nextIterator = new NextIterator<T>(nextObserver);

        items.materialize().subscribe(nextObserver);

        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return nextIterator;
            }
        };

    }

    private static class NextIterator<T> implements Iterator<T> {

        private final NextObserver<T> observer;

        private NextIterator(NextObserver<T> observer) {
            this.observer = observer;
        }

        @Override
        public boolean hasNext() {
            return !observer.isCompleted(false);
        }

        @Override
        public T next() {
            if (observer.isCompleted(true)) {
                throw new IllegalStateException("Observable is completed");
            }

            observer.await();

            try {
                return observer.takeNext();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw Exceptions.propagate(e);
            }

        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    private static class NextObserver<T> implements Observer<Notification<T>> {
        private final BlockingQueue<Notification<T>> buf = new ArrayBlockingQueue<Notification<T>>(1);
        private final AtomicBoolean waiting = new AtomicBoolean(false);

        @Override
        public void onCompleted() {
            // ignore
        }

        @Override
        public void onError(Throwable e) {
            // ignore
        }

        @Override
        public void onNext(Notification<T> args) {

            if (waiting.getAndSet(false) || !args.isOnNext()) {
                Notification<T> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Notification<T> concurrentItem = buf.poll();

                    // in case if we won race condition with onComplete/onError method
                    if (!concurrentItem.isOnNext()) {
                        toOffer = concurrentItem;
                    }
                }
            }

        }

        public void await() {
            waiting.set(true);
        }

        public boolean isCompleted(boolean rethrowExceptionIfExists) {
            Notification<T> lastItem = buf.peek();
            if (lastItem == null) {
                return false;
            }

            if (lastItem.isOnError()) {
                if (rethrowExceptionIfExists) {
                    throw Exceptions.propagate(lastItem.getThrowable());
                } else {
                    return true;
                }
            }

            return lastItem.isOnCompleted();
        }

        public T takeNext() throws InterruptedException {
            Notification<T> next = buf.take();

            if (next.isOnError()) {
                throw Exceptions.propagate(next.getThrowable());
            }

            if (next.isOnCompleted()) {
                throw new IllegalStateException("Observable is completed");
            }

            return next.getValue();

        }

    }

    public static class UnitTest {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();

        @Test
        public void testNext() throws Throwable {
            Subscription s = mock(Subscription.class);
            final TestObservable obs = new TestObservable(s);

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnNext("two");
            assertEquals("two", next.get());

            assertTrue(it.hasNext());

            obs.sendOnCompleted();

            assertFalse(it.hasNext());
        }

        @Test(expected = TestException.class)
        public void testOnError() throws Throwable {
            Subscription s = mock(Subscription.class);
            final TestObservable obs = new TestObservable(s);

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnError(new TestException());

            try {
                next.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }

        @Test
        public void testOnErrorViaHasNext() throws Throwable {
            Subscription s = mock(Subscription.class);
            final TestObservable obs = new TestObservable(s);

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.sendOnError(new TestException());

            // this should not throw an exception but instead just return false
            try {
                assertFalse(it.hasNext());
            } catch (Throwable e) {
                fail("should not have received exception");
                e.printStackTrace();
            }
        }

        private Future<String> nextAsync(final Iterator<String> it) throws Throwable {

            return executor.submit(new Callable<String>() {

                @Override
                public String call() throws Exception {
                    return it.next();
                }
            });
        }

        private static class TestObservable extends Observable<String> {

            Observer<String> observer = null;
            Subscription s;

            public TestObservable(Subscription s) {
                this.s = s;
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
            public void sendOnError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return s;
            }

        }

        @SuppressWarnings("serial")
        private static class TestException extends RuntimeException {

        }

        /**
         * Confirm that no buffering or blocking of the Observable onNext calls occurs and it just grabs the next emitted value.
         * 
         * This results in output such as => a: 1 b: 2 c: 89
         * 
         * @throws Throwable
         */
        @Test
        public void testNoBufferingOrBlockingOfSequence() throws Throwable {
            final CountDownLatch finished = new CountDownLatch(1);
            final AtomicBoolean running = new AtomicBoolean(true);
            final AtomicInteger count = new AtomicInteger(0);
            final Observable<Integer> obs = Observable.create(new Func1<Observer<Integer>, Subscription>() {

                @Override
                public Subscription call(final Observer<Integer> o) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                while (running.get()) {
                                    o.onNext(count.incrementAndGet());
                                    Thread.sleep(0, 100);
                                }
                                o.onCompleted();
                            } catch (Throwable e) {
                                o.onError(e);
                            } finally {
                                finished.countDown();
                            }
                        }
                    }).start();
                    return Subscriptions.empty();
                }

            });

            Iterator<Integer> it = next(obs).iterator();

            assertTrue(it.hasNext());
            int a = it.next();
            assertTrue(it.hasNext());
            int b = it.next();
            // we should have a different value
            assertTrue("a and b should be different", a != b);

            // wait for some time
            Thread.sleep(100);
            // make sure the counter in the observable has increased beyond b
            while (count.get() <= (b + 10)) {
                Thread.sleep(100);
            }

            assertTrue(it.hasNext());
            int expectedHigherThan = count.get();
            int c = it.next();

            assertTrue("c should not just be the next in sequence", c != (b + 1));
            assertTrue("expected that c [" + c + "] is higher than " + expectedHigherThan, c > expectedHigherThan);

            assertTrue(it.hasNext());

            // shut down the thread
            running.set(false);

            finished.await();

            assertFalse(it.hasNext());

            System.out.println("a: " + a + " b: " + b + " c: " + c);
        }

    }

}
