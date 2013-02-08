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
import rx.Observable;
import rx.Observer;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public final class OperationForEach {

    /**
     * Accepts a sequence and a action. Applies the action to each element in
     * the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param onNext
     *            a action to apply to each item in the sequence.
     */
    public static <T> void forEach(final Observable<T> sequence, final Action1<T> onNext) {
        forEach(sequence, onNext, null, null);
    }

    /**
     * Accepts a sequence and a action. Applies the action to each element in
     * the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param onNext
     *            a action to apply to each item in the sequence.
     * @param onCompleted
     *            a action to run when sequence completes.
     */
    public static <T> void forEach(final Observable<T> sequence, final Action1<T> onNext, final Action0 onCompleted) {
        forEach(sequence, onNext, onCompleted, null);
    }

    /**
     * Accepts a sequence and a action. Applies the action to each element in
     * the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param onNext
     *            a action to apply to each item in the sequence.
     * @param onCompleted
     *            a action to run when sequence completes.
     * @param onError
     *            a action to run when an exception is thrown.
     */
    public static <T> void forEach(final Observable<T> sequence, final Action1<T> onNext, final Action0 onCompleted,
            final Action1<Exception> onError) {
        ForEachObserver<T> fe = new ForEachObserver<T>(onNext, onCompleted, onError);
        sequence.subscribe(fe);
    }

    private static final class ForEachObserver<T> implements Observer<T> {
        private final Action1<T> onNext;
        private final Action0 onCompleted;
        private final Action1 onError;

        private boolean running = true;

        private ForEachObserver(final Action1<T> onNext, final Action0 onCompleted, final Action1<Exception> onError) {
            if (onNext == null)
                throw new NullPointerException();
            this.onNext = onNext;
            this.onCompleted = onCompleted;
            this.onError = onError;
        }

        @Override
        public void onCompleted() {
            running = false;
            if (onCompleted != null) {
                onCompleted.call();
            }
        }

        @Override
        public void onError(final Exception e) {
            running = false;
            if (onError != null) {
                onError.call(e);
            }
        }

        @Override
        public void onNext(final T args) {
            if (running) {
                try {
                    onNext.call(args);
                } catch (Exception e) {
                    onError(e);
                }
            }
        }
    }

    public static class UnitTest {

        @Test
        public void testForEach() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");

            Observable<Map<String, String>> observable = Observable.toObservable(m1, m2);

            final AtomicInteger counter = new AtomicInteger();
            forEach(observable, new Action1<Map<String, String>>() {
                @Override
                public void call(final Map<String, String> stringStringMap) {
                    switch (counter.getAndIncrement()) {
                    case 0:
                        assertEquals("firstName doesn't match", "OneFirst", stringStringMap.get("firstName"));
                        assertEquals("lastName doesn't match", "OneLast", stringStringMap.get("lastName"));
                        break;
                    case 1:
                        assertEquals("firstName doesn't match", "TwoFirst", stringStringMap.get("firstName"));
                        assertEquals("lastName doesn't match", "TwoLast", stringStringMap.get("lastName"));
                        break;
                    default:
                        fail("Unknown increment");
                    }
                }
            });
            assertEquals("Number of executions didn't match expected.", 2, counter.get());
        }

        @Test
        public void testForEachEmptyObserver() {
            Observable<Map<String, String>> observable = Observable.empty();

            final AtomicInteger counter = new AtomicInteger();
            forEach(observable, new Action1<Map<String, String>>() {
                @Override
                public void call(final Map<String, String> stringStringMap) {
                    counter.incrementAndGet();
                    fail("Should not have called action");
                }
            });
            assertEquals("Number of executions didn't match expected.", 0, counter.get());
        }

        @Test
        public void testForEachWithException() {
            Observable<Integer> observable = Observable.toObservable(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            final AtomicInteger counter = new AtomicInteger();
            final AtomicReference<Exception> exception = new AtomicReference<Exception>();
            forEach(observable, new Action1<Integer>() {
                @Override
                public void call(final Integer integer) {
                    counter.incrementAndGet();
                    if (integer.equals(5)) {
                        // fail half way through
                        throw new RuntimeException("testForEachWithException");
                    }
                }
            }, null, new Action1<Exception>() {
                @Override
                public void call(final Exception e) {
                    exception.set(e);
                }
            });
            assertEquals("Number of executions didn't match expected.", 5, counter.get());
            assertNotNull(exception.get());
        }

        private Map<String, String> getMap(String prefix) {
            Map<String, String> m = new HashMap<String, String>();
            m.put("firstName", prefix + "First");
            m.put("lastName", prefix + "Last");
            return m;
        }

    }
}
