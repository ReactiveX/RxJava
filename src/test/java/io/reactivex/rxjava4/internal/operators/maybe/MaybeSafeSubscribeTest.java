/*
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

package io.reactivex.rxjava4.internal.operators.maybe;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class MaybeSafeSubscribeTest extends RxJavaTest {

    @Test
    public void normalSuccess() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);

            Maybe.just(1)
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onSuccess(1);
            order.verifyNoMoreInteractions();

            assertTrue(errors.isEmpty(), "" + errors);
        });
    }

    @Test
    public void normalError() throws Throwable  {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);

            Maybe.<Integer>error(new TestException())
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onError(any(TestException.class));
            order.verifyNoMoreInteractions();

            assertTrue(errors.isEmpty(), "" + errors);
        });
    }

    @Test
    public void normalEmpty() throws Throwable  {
        withErrorTracking(_ -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);

            Maybe.<Integer>empty()
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onComplete();
            order.verifyNoMoreInteractions();
        });
    }

    @Test
    public void onSubscribeCrash() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);
            doThrow(new TestException()).when(consumer).onSubscribe(any());

            Disposable d = Disposable.empty();

            new Maybe<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(@NonNull MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(d);
                    // none of the following should arrive at the consumer
                    observer.onSuccess(1);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            }
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verifyNoMoreInteractions();

            assertTrue(d.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            TestHelper.assertUndeliverable(errors, 1, IOException.class);
        });
    }

    @Test
    public void onSuccessCrash() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);
            doThrow(new TestException()).when(consumer).onSuccess(any());

            new Maybe<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(@NonNull MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onSuccess(1);
                }
            }
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onSuccess(1);
            order.verifyNoMoreInteractions();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void onErrorCrash() throws Throwable  {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);
            doThrow(new TestException()).when(consumer).onError(any());

            new Maybe<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(@NonNull MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    // none of the following should arrive at the consumer
                    observer.onError(new IOException());
                }
            }
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onError(any(IOException.class));
            order.verifyNoMoreInteractions();

            TestHelper.assertError(errors, 0, CompositeException.class);

            CompositeException compositeException = (CompositeException)errors.getFirst();
            TestHelper.assertError(compositeException.getExceptions(), 0, IOException.class);
            TestHelper.assertError(compositeException.getExceptions(), 1, TestException.class);
        });
    }

    @Test
    public void onCompleteCrash() throws Throwable  {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            MaybeObserver<Integer> consumer = mock(MaybeObserver.class);
            doThrow(new TestException()).when(consumer).onComplete();

            new Maybe<Integer>() /* NFI */ {
                @Override
                protected void subscribeActual(@NonNull MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    // none of the following should arrive at the consumer
                    observer.onComplete();
                }
            }
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onComplete();
            order.verifyNoMoreInteractions();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }
}
