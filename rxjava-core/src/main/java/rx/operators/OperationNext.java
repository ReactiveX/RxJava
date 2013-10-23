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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.Schedulers;
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
        private T next;
        private boolean hasNext = true;
        private boolean isNextConsumed = true;

        private NextIterator(NextObserver<? extends T> observer) {
            this.observer = observer;
        }

        @Override
        public boolean hasNext() {
            // Since an iterator should not be used in different thread,
            // so we do not need any synchronization.
            if(hasNext == false) {
                // the iterator has reached the end.
                return false;
            }
            if(isNextConsumed == false) {
                // next has not been used yet.
                return true;
            }
            return moveToNext();
        }

        private boolean moveToNext() {
            try {
                Notification<? extends T> nextNotification = observer.takeNext();
                if(nextNotification.isOnNext()) {
                    isNextConsumed = false;
                    next = nextNotification.getValue();
                    return true;
                }
                // If an observable is completed or fails,
                // hasNext() always return false.
                hasNext = false;
                if(nextNotification.isOnCompleted()) {
                    return false;
                }
                if(nextNotification.isOnError()) {
                    throw Exceptions.propagate(nextNotification.getThrowable());
                }
                throw new IllegalStateException("Should not reach here");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw Exceptions.propagate(e);
            }
        }

        @Override
        public T next() {
            if(hasNext()) {
                isNextConsumed = true;
                return next;
            }
            else {
                throw new NoSuchElementException("No more elements");
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
                    if (concurrentItem != null && !concurrentItem.isOnNext()) {
                        toOffer = concurrentItem;
                    }
                }
            }

        }

        public Notification<? extends T> takeNext() throws InterruptedException {
            waiting.set(true);
            return buf.take();
        }

    }

    public static class UnitTest {

        private void fireOnNextInNewThread(final Subject<String, String> o, final String value) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    o.onNext(value);
                }
            }.start();
        }

        private void fireOnErrorInNewThread(final Subject<String, String> o) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    o.onError(new TestException());
                }
            }.start();
        }


        @Test
        public void testNext() {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();
            fireOnNextInNewThread(obs, "one");
            assertTrue(it.hasNext());
            assertEquals("one", it.next());

            fireOnNextInNewThread(obs, "two");
            assertTrue(it.hasNext());
            assertEquals("two", it.next());

            obs.onCompleted();
            assertFalse(it.hasNext());
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }

            // If the observable is completed, hasNext always returns false and next always throw a NoSuchElementException.
            assertFalse(it.hasNext());
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }
        }

        @Test
        public void testNextWithError() {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();
            fireOnNextInNewThread(obs, "one");
            assertTrue(it.hasNext());
            assertEquals("one", it.next());

            fireOnErrorInNewThread(obs);
            try {
                it.hasNext();
                fail("Expected an TestException");
            }
            catch(TestException e) {
                // successful
            }

            // After the observable fails, hasNext always returns false and next always throw a NoSuchElementException.
            assertFalse(it.hasNext());
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }
        }

        @Test
        public void testNextWithEmpty() {
           Observable<String> obs = Observable.<String>empty().observeOn(Schedulers.newThread());
           Iterator<String> it = next(obs).iterator();

           assertFalse(it.hasNext());
           try {
               it.next();
               fail("At the end of an iterator should throw a NoSuchElementException");
           }
           catch(NoSuchElementException e){
           }

           // If the observable is completed, hasNext always returns false and next always throw a NoSuchElementException.
           assertFalse(it.hasNext());
           try {
               it.next();
               fail("At the end of an iterator should throw a NoSuchElementException");
           }
           catch(NoSuchElementException e){
           }
        }

        @Test
        public void testOnError() throws Throwable {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();

            obs.onError(new TestException());
            try {
                it.hasNext();
                fail("Expected an TestException");
            }
            catch(TestException e) {
                // successful
            }

            // After the observable fails, hasNext always returns false and next always throw a NoSuchElementException.
            assertFalse(it.hasNext());
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }
        }

        @Test
        public void testOnErrorInNewThread() {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();

            fireOnErrorInNewThread(obs);

            try {
                it.hasNext();
                fail("Expected an TestException");
            }
            catch(TestException e) {
                // successful
            }

            // After the observable fails, hasNext always returns false and next always throw a NoSuchElementException.
            assertFalse(it.hasNext());
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }
        }

        @Test
        public void testNextWithOnlyUsingNextMethod() {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();
            fireOnNextInNewThread(obs, "one");
            assertEquals("one", it.next());

            fireOnNextInNewThread(obs, "two");
            assertEquals("two", it.next());

            obs.onCompleted();
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
            }
        }

        @Test
        public void testNextWithCallingHasNextMultipleTimes() {
            Subject<String, String> obs = PublishSubject.create();
            Iterator<String> it = next(obs).iterator();
            fireOnNextInNewThread(obs, "one");
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
            assertEquals("one", it.next());

            obs.onCompleted();
            try {
                it.next();
                fail("At the end of an iterator should throw a NoSuchElementException");
            }
            catch(NoSuchElementException e){
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
            int d = it.next();
            assertTrue(d > c);

            // shut down the thread
            running.set(false);

            finished.await();

            assertFalse(it.hasNext());

            System.out.println("a: " + a + " b: " + b + " c: " + c);
        }

    }

}
