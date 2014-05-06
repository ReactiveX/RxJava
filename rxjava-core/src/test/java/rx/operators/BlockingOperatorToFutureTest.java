/**
 * Copyright 2014 Netflix, Inc.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rx.operators.BlockingOperatorToFuture.toFuture;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.TestException;

public class BlockingOperatorToFutureTest {

    @Test
    public void testToFuture() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one");
        Future<String> f = toFuture(obs);
        assertEquals("one", f.get());
    }

    @Test
    public void testToFutureList() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one", "two", "three");
        Future<List<String>> f = toFuture(obs.toList());
        assertEquals("one", f.get().get(0));
        assertEquals("two", f.get().get(1));
        assertEquals("three", f.get().get(2));
    }

    @Test(expected = ExecutionException.class)
    public void testExceptionWithMoreThanOneElement() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one", "two");
        Future<String> f = toFuture(obs);
        assertEquals("one", f.get());
        // we expect an exception since there are more than 1 element
    }

    @Test
    public void testToFutureWithException() {
        Observable<String> obs = Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
            }
        });

        Future<String> f = toFuture(obs);
        try {
            f.get();
            fail("expected exception");
        } catch (Throwable e) {
            assertEquals(TestException.class, e.getCause().getClass());
        }
    }

    @Test(expected=CancellationException.class)
    public void testGetAfterCancel() throws Exception {
        Observable<String> obs = Observable.create(new OperationNeverComplete<String>());
        Future<String> f = toFuture(obs);
        boolean cancelled = f.cancel(true);
        assertTrue(cancelled);  // because OperationNeverComplete never does
        f.get();                // Future.get() docs require this to throw
    }

    @Test(expected=CancellationException.class)
    public void testGetWithTimeoutAfterCancel() throws Exception {
        Observable<String> obs = Observable.create(new OperationNeverComplete<String>());
        Future<String> f = toFuture(obs);
        boolean cancelled = f.cancel(true);
        assertTrue(cancelled);  // because OperationNeverComplete never does
        f.get(Long.MAX_VALUE, TimeUnit.NANOSECONDS);    // Future.get() docs require this to throw
    }

    /**
     * Emits no observations. Used to simulate a long-running asynchronous operation.
     */
    private static class OperationNeverComplete<T> implements Observable.OnSubscribe<T> {
        @Override
        public void call(Subscriber<? super T> unused) {
            // do nothing
        }
    }
}
