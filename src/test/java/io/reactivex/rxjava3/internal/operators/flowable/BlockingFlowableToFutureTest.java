/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;

public class BlockingFlowableToFutureTest {

    @Test
    public void toFuture() throws InterruptedException, ExecutionException {
        Flowable<String> obs = Flowable.just("one");
        Future<String> f = obs.toFuture();
        assertEquals("one", f.get());
    }

    @Test
    public void toFutureList() throws InterruptedException, ExecutionException {
        Flowable<String> obs = Flowable.just("one", "two", "three");
        Future<List<String>> f = obs.toList().toFuture();
        assertEquals("one", f.get().get(0));
        assertEquals("two", f.get().get(1));
        assertEquals("three", f.get().get(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void exceptionWithMoreThanOneElement() throws Throwable {
        Flowable<String> obs = Flowable.just("one", "two");
        Future<String> f = obs.toFuture();
        try {
            // we expect an exception since there are more than 1 element
            f.get();
            fail("Should have thrown!");
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void toFutureWithException() {
        Flowable<String> obs = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                subscriber.onNext("one");
                subscriber.onError(new TestException());
            }
        });

        Future<String> f = obs.toFuture();
        try {
            f.get();
            fail("expected exception");
        } catch (Throwable e) {
            assertEquals(TestException.class, e.getCause().getClass());
        }
    }

    @Test(expected = CancellationException.class)
    public void getAfterCancel() throws Exception {
        Flowable<String> obs = Flowable.never();
        Future<String> f = obs.toFuture();
        boolean cancelled = f.cancel(true);
        assertTrue(cancelled);  // because OperationNeverComplete never does
        f.get();                // Future.get() docs require this to throw
    }

    @Test(expected = CancellationException.class)
    public void getWithTimeoutAfterCancel() throws Exception {
        Flowable<String> obs = Flowable.never();
        Future<String> f = obs.toFuture();
        boolean cancelled = f.cancel(true);
        assertTrue(cancelled);  // because OperationNeverComplete never does
        f.get(Long.MAX_VALUE, TimeUnit.NANOSECONDS);    // Future.get() docs require this to throw
    }

    @Test(expected = NoSuchElementException.class)
    public void getWithEmptyFlowable() throws Throwable {
        Flowable<String> obs = Flowable.empty();
        Future<String> f = obs.toFuture();
        try {
            f.get();
        }
        catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
