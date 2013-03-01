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

import org.junit.Test;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.Exceptions;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


/**
 * Samples the next value (blocking without buffering) from in an observable sequence.
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
            return !observer.isCompleted();
        }

        @Override
        public T next() {
            if (observer.isCompleted()) {
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
        public void onError(Exception e) {
            // ignore
        }

        @Override
        public void onNext(Notification<T> args) {

            if (waiting.getAndSet(false) || !args.isOnNext()) {
                Notification<T> toOffer = args;
                while (!buf.offer(toOffer)) {
                    Notification<T> poll = buf.poll();

                    // in case if we won race condition with onComplete/onError method
                    if (!poll.isOnNext()) {
                        toOffer = poll;
                    }
                }
            }

        }

        public void await() {
            waiting.set(true);
        }

        public boolean isCompleted() {
            Notification<T> lastItem = buf.peek();
            if (lastItem == null) {
                return false;
            }

            if (lastItem.isOnError()) {
                throw Exceptions.propagate(lastItem.getException());
            }

            return lastItem.isOnCompleted();
        }

        public T takeNext() throws InterruptedException {
            Notification<T> next = buf.take();

            if (next.isOnError()) {
                throw Exceptions.propagate(next.getException());
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
        public void testNext() throws Exception {
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

        private Future<String> nextAsync(final Iterator<String> it) throws Exception {

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
            @SuppressWarnings("unused")
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return s;
            }

        }

        private static class TestException extends RuntimeException {

        }


    }

}
