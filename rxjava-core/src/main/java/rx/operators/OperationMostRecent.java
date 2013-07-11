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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.Exceptions;

/**
 * Returns an Iterable that always returns the item most recently emitted by an Observable, or a
 * seed value if no item has yet been emitted.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
 */
public final class OperationMostRecent {

    public static <T> Iterable<T> mostRecent(final Observable<T> source, T initialValue) {

        MostRecentObserver<T> mostRecentObserver = new MostRecentObserver<T>(initialValue);
        final MostRecentIterator<T> nextIterator = new MostRecentIterator<T>(mostRecentObserver);

        source.subscribe(mostRecentObserver);

        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return nextIterator;
            }
        };

    }

    private static class MostRecentIterator<T> implements Iterator<T> {

        private final MostRecentObserver<T> observer;

        private MostRecentIterator(MostRecentObserver<T> observer) {
            this.observer = observer;
        }

        @Override
        public boolean hasNext() {
            return !observer.isCompleted();
        }

        @Override
        public T next() {
            if (observer.getException() != null) {
                throw Exceptions.propagate(observer.getException());
            }
            return observer.getRecentValue();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Read only iterator");
        }
    }

    private static class MostRecentObserver<T> implements Observer<T> {
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicReference<T> value;
        private final AtomicReference<Exception> exception = new AtomicReference<Exception>();

        private MostRecentObserver(T value) {
            this.value = new AtomicReference<T>(value);
        }

        @Override
        public void onCompleted() {
            completed.set(true);
        }

        @Override
        public void onError(Exception e) {
            exception.set(e);
        }

        @Override
        public void onNext(T args) {
            value.set(args);
        }

        public boolean isCompleted() {
            return completed.get();
        }

        public Exception getException() {
            return exception.get();
        }

        public T getRecentValue() {
            return value.get();
        }

    }

    public static class UnitTest {
        @Test
        public void testMostRecent() {
            Subscription s = mock(Subscription.class);
            TestObservable observable = new TestObservable(s);

            Iterator<String> it = mostRecent(observable, "default").iterator();

            assertTrue(it.hasNext());
            assertEquals("default", it.next());
            assertEquals("default", it.next());

            observable.sendOnNext("one");
            assertTrue(it.hasNext());
            assertEquals("one", it.next());
            assertEquals("one", it.next());

            observable.sendOnNext("two");
            assertTrue(it.hasNext());
            assertEquals("two", it.next());
            assertEquals("two", it.next());

            observable.sendOnCompleted();
            assertFalse(it.hasNext());

        }

        @Test(expected = TestException.class)
        public void testMostRecentWithException() {
            Subscription s = mock(Subscription.class);
            TestObservable observable = new TestObservable(s);

            Iterator<String> it = mostRecent(observable, "default").iterator();

            assertTrue(it.hasNext());
            assertEquals("default", it.next());
            assertEquals("default", it.next());

            observable.sendOnError(new TestException());
            assertTrue(it.hasNext());

            it.next();
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
            private static final long serialVersionUID = 1L;
        }

    }

}
