/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableFromSupplierTest extends RxJavaTest {

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotInvokeFuncUntilSubscription() throws Throwable {
        Supplier<Object> func = mock(Supplier.class);

        when(func.get()).thenReturn(new Object());

        Observable<Object> fromSupplierObservable = Observable.fromSupplier(func);

        verifyNoInteractions(func);

        fromSupplierObservable.subscribe();

        verify(func).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallOnNextAndOnCompleted() throws Throwable {
        Supplier<String> func = mock(Supplier.class);

        when(func.get()).thenReturn("test_value");

        Observable<String> fromSupplierObservable = Observable.fromSupplier(func);

        Observer<Object> observer = TestHelper.mockObserver();

        fromSupplierObservable.subscribe(observer);

        verify(observer).onNext("test_value");
        verify(observer).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallOnError() throws Throwable {
        Supplier<Object> func = mock(Supplier.class);

        Throwable throwable = new IllegalStateException("Test exception");
        when(func.get()).thenThrow(throwable);

        Observable<Object> fromSupplierObservable = Observable.fromSupplier(func);

        Observer<Object> observer = TestHelper.mockObserver();

        fromSupplierObservable.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
        verify(observer).onError(throwable);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNotDeliverResultIfSubscriberUnsubscribedBeforeEmission() throws Throwable {
        Supplier<String> func = mock(Supplier.class);

        final CountDownLatch funcLatch = new CountDownLatch(1);
        final CountDownLatch observerLatch = new CountDownLatch(1);

        when(func.get()).thenAnswer(new Answer<String>() {
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

        Observable<String> fromSupplierObservable = Observable.fromSupplier(func);

        Observer<Object> observer = TestHelper.mockObserver();

        TestObserver<String> outer = new TestObserver<>(observer);

        fromSupplierObservable
                .subscribeOn(Schedulers.computation())
                .subscribe(outer);

        // Wait until func will be invoked
        observerLatch.await();

        // Unsubscribing before emission
        outer.dispose();

        // Emitting result
        funcLatch.countDown();

        // func must be invoked
        verify(func).get();

        // Observer must not be notified at all
        verify(observer).onSubscribe(any(Disposable.class));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldAllowToThrowCheckedException() {
        final Exception checkedException = new Exception("test exception");

        Observable<Object> fromSupplierObservable = Observable.fromSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                throw checkedException;
            }
        });

        Observer<Object> observer = TestHelper.mockObserver();

        fromSupplierObservable.subscribe(observer);

        verify(observer).onSubscribe(any(Disposable.class));
        verify(observer).onError(checkedException);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void fusedFlatMapExecution() {
        final int[] calls = { 0 };

        Observable.just(1).flatMap(new Function<Integer, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Observable.fromSupplier(new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
                        return ++calls[0];
                    }
                });
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void fusedFlatMapExecutionHidden() {
        final int[] calls = { 0 };

        Observable.just(1).hide().flatMap(new Function<Integer, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Observable.fromSupplier(new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
                        return ++calls[0];
                    }
                });
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void fusedFlatMapNull() {
        Observable.just(1).flatMap(new Function<Integer, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Observable.fromSupplier(new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
                        return null;
                    }
                });
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void fusedFlatMapNullHidden() {
        Observable.just(1).hide().flatMap(new Function<Integer, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(Integer v)
                    throws Exception {
                return Observable.fromSupplier(new Supplier<Object>() {
                    @Override
                    public Object get() throws Exception {
                        return null;
                    }
                });
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void disposedOnArrival() {
        final int[] count = { 0 };
        Observable.fromSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
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

        Observable.fromSupplier(new Supplier<Integer>() {
            @Override
            public Integer get() throws Exception {
                to.dispose();
                return 1;
            }
        })
                .subscribe(to);

        to.assertEmpty();
    }

    @Test
    public void disposedOnCallThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final TestObserver<Integer> to = new TestObserver<>();

            Observable.fromSupplier(new Supplier<Integer>() {
                @Override
                public Integer get() throws Exception {
                    to.dispose();
                    throw new TestException();
                }
            })
            .subscribe(to);

            to.assertEmpty();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void take() {
        Observable.fromSupplier(new Supplier<Object>() {
            @Override
            public Object get() throws Exception {
                return 1;
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }
}
