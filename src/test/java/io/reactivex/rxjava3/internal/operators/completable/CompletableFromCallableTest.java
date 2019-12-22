/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableFromCallableTest extends RxJavaTest {
    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Completable.fromCallable(null);
    }

    @Test
    public void fromCallable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCallableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        };

        Completable.fromCallable(callable)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Completable.fromCallable(callable)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromCallableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable completable = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        });

        assertEquals(0, atomicInteger.get());

        completable
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCallableThrows() {
        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotDeliverResultIfSubscriberUnsubscribedBeforeEmission() throws Exception {
        Callable<String> func = mock(Callable.class);

        final CountDownLatch funcLatch = new CountDownLatch(1);
        final CountDownLatch observerLatch = new CountDownLatch(1);

        when(func.call()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                observerLatch.countDown();

                try {
                    funcLatch.await();
                } catch (InterruptedException e) {
                    // It's okay, unsubscription causes Thread interruption

                    // Restoring interruption status of the Thread
                    Thread.currentThread().interrupt();
                }

                return "should_not_be_delivered";
            }
        });

        Completable fromCallableObservable = Completable.fromCallable(func);

        Observer<Object> observer = TestHelper.mockObserver();

        TestObserver<String> outer = new TestObserver<>(observer);

        fromCallableObservable
                .subscribeOn(Schedulers.computation())
                .subscribe(outer);

        // Wait until func will be invoked
        observerLatch.await();

        // Unsubscribing before emission
        outer.dispose();

        // Emitting result
        funcLatch.countDown();

        // func must be invoked
        verify(func).call();

        // Observer must not be notified at all
        verify(observer).onSubscribe(any(Disposable.class));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void fromActionErrorsDisposed() {
        final AtomicInteger calls = new AtomicInteger();
        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                calls.incrementAndGet();
                throw new TestException();
            }
        })
        .test(true)
        .assertEmpty();

        assertEquals(1, calls.get());
    }
}
