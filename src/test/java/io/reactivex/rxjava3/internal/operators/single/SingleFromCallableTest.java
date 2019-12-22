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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleFromCallableTest extends RxJavaTest {

    @Test
    public void fromCallableValue() {
        Single.fromCallable(new Callable<Integer>() {
            @Override public Integer call() throws Exception {
                return 5;
            }
        })
        .test()
        .assertResult(5);
    }

    @Test
    public void fromCallableError() {
        Single.fromCallable(new Callable<Integer>() {
            @Override public Integer call() throws Exception {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void fromCallableNull() {
        Single.fromCallable(new Callable<Integer>() {
            @Override public Integer call() throws Exception {
                return null;
            }
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The callable returned a null value");
    }

    @Test
    public void fromCallableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return atomicInteger.incrementAndGet();
            }
        };

        Single.fromCallable(callable)
                .test()
                .assertResult(1);

        assertEquals(1, atomicInteger.get());

        Single.fromCallable(callable)
                .test()
                .assertResult(2);

        assertEquals(2, atomicInteger.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotInvokeFuncUntilSubscription() throws Exception {
        Callable<Object> func = mock(Callable.class);

        when(func.call()).thenReturn(new Object());

        Single<Object> fromCallableSingle = Single.fromCallable(func);

        verifyNoInteractions(func);

        fromCallableSingle.subscribe();

        verify(func).call();
    }

    @Test
    public void noErrorLoss() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final CountDownLatch cdl1 = new CountDownLatch(1);
            final CountDownLatch cdl2 = new CountDownLatch(1);

            TestObserver<Integer> to = Single.fromCallable(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    cdl1.countDown();
                    cdl2.await(5, TimeUnit.SECONDS);
                    return 1;
                }
            }).subscribeOn(Schedulers.single()).test();

            assertTrue(cdl1.await(5, TimeUnit.SECONDS));

            to.dispose();

            int timeout = 10;

            while (timeout-- > 0 && errors.isEmpty()) {
                Thread.sleep(100);
            }

            TestHelper.assertUndeliverable(errors, 0, InterruptedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
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

        Single<String> fromCallableObservable = Single.fromCallable(func);

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
    public void shouldAllowToThrowCheckedException() {
        final Exception checkedException = new Exception("test exception");

        Single<Object> fromCallableObservable = Single.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw checkedException;
            }
        });

        SingleObserver<Object> observer = TestHelper.mockSingleObserver();

        fromCallableObservable.subscribe(observer);

        verify(observer).onSubscribe(any(Disposable.class));
        verify(observer).onError(checkedException);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void disposedOnArrival() {
        final int[] count = { 0 };
        Single.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                count[0]++;
                return 1;
            }
        })
                .test(true)
                .assertEmpty();

        assertEquals(0, count[0]);
    }

    @Test
    public void disposedOnCall() {
        final TestObserver<Integer> to = new TestObserver<>();

        Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                to.dispose();
                return 1;
            }
        })
                .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void toObservableTake() {
        Single.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return 1;
            }
        })
                .toObservable()
                .take(1)
                .test()
                .assertResult(1);
    }

    @Test
    public void toObservableAndBack() {
        Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1;
            }
        })
                .toObservable()
                .singleOrError()
                .test()
                .assertResult(1);
    }
}
