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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;
import rx.util.Exceptions;

/**
 * Returns an Iterable that blocks until the Observable emits another item, then returns that item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
 */
public final class OperationNext {

    public static <T> Iterable<T> next(final Observable<? extends T> items) {

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

        private final NextObserver<? extends T> observer;

        private NextIterator(NextObserver<? extends T> observer) {
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

    private static class NextObserver<T> implements Observer<Notification<? extends T>> {
        private final BlockingQueue<Notification<? extends T>> buf = new ArrayBlockingQueue<Notification<? extends T>>(1);
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
        public void onNext(Notification<? extends T> args) {

            if (waiting.getAndSet(false) || !args.isOnNext()) {
                Notification<? extends T> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Notification<? extends T> concurrentItem = buf.poll();

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
            Notification<? extends T> lastItem = buf.peek();
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
            Notification<? extends T> next = buf.take();

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
            Subject<String, String> obs = PublishSubject.create();

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.onNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.onNext("two");
            assertEquals("two", next.get());

            assertTrue(it.hasNext());

            obs.onCompleted();

            assertFalse(it.hasNext());
        }

        @Test(expected = TestException.class)
        public void testOnError() throws Throwable {
            Subject<String, String> obs = PublishSubject.create();;

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.onNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.onError(new TestException());

            try {
                next.get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
        }

        @Test
        public void testOnErrorViaHasNext() throws Throwable {
            Subject<String, String> obs = PublishSubject.create();

            Iterator<String> it = next(obs).iterator();

            assertTrue(it.hasNext());

            Future<String> next = nextAsync(it);
            Thread.sleep(100);
            obs.onNext("one");
            assertEquals("one", next.get());

            assertTrue(it.hasNext());

            next = nextAsync(it);
            Thread.sleep(100);
            obs.onError(new TestException());

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
            final int COUNT = 30;
            final CountDownLatch timeHasPassed = new CountDownLatch(COUNT);
            final AtomicBoolean running = new AtomicBoolean(true);
            final AtomicInteger count = new AtomicInteger(0);
            final Observable<Integer> obs = Observable.create(new OnSubscribeFunc<Integer>() {

                @Override
                public Subscription onSubscribe(final Observer<? super Integer> o) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                while (running.get()) {
                                    o.onNext(count.incrementAndGet());
                                    timeHasPassed.countDown();
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

            // wait for some time (if times out we are blocked somewhere so fail ... set very high for very slow, constrained machines)
            timeHasPassed.await(8000, TimeUnit.MILLISECONDS);

            assertTrue(it.hasNext());
            int c = it.next();

            assertTrue("c should not just be the next in sequence", c != (b + 1));
            assertTrue("expected that c [" + c + "] is higher than or equal to " + COUNT, c >= COUNT);

            assertTrue(it.hasNext());

            // shut down the thread
            running.set(false);

            finished.await();

            assertFalse(it.hasNext());

            System.out.println("a: " + a + " b: " + b + " c: " + c);
        }

    }

}
