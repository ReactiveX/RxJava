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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableSafeSubscribeTest {

    @Test
    public void normalError() throws Throwable  {
        TestHelper.withErrorTracking(errors -> {
            CompletableObserver consumer = mock(CompletableObserver.class);

            Completable.error(new TestException())
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onError(any(TestException.class));
            order.verifyNoMoreInteractions();

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void normalEmpty() throws Throwable  {
        TestHelper.withErrorTracking(errors -> {
            CompletableObserver consumer = mock(CompletableObserver.class);

            Completable.complete()
            .safeSubscribe(consumer);

            InOrder order = inOrder(consumer);
            order.verify(consumer).onSubscribe(any(Disposable.class));
            order.verify(consumer).onComplete();
            order.verifyNoMoreInteractions();
        });
    }

    @Test
    public void onSubscribeCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            CompletableObserver consumer = mock(CompletableObserver.class);
            doThrow(new TestException()).when(consumer).onSubscribe(any());

            Disposable d = Disposable.empty();

            new Completable() {
                @Override
                protected void subscribeActual(@NonNull CompletableObserver observer) {
                    observer.onSubscribe(d);
                    // none of the following should arrive at the consumer
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
    public void onErrorCrash() throws Throwable  {
        TestHelper.withErrorTracking(errors -> {
            CompletableObserver consumer = mock(CompletableObserver.class);
            doThrow(new TestException()).when(consumer).onError(any());

            new Completable() {
                @Override
                protected void subscribeActual(@NonNull CompletableObserver observer) {
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

            CompositeException compositeException = (CompositeException)errors.get(0);
            TestHelper.assertError(compositeException.getExceptions(), 0, IOException.class);
            TestHelper.assertError(compositeException.getExceptions(), 1, TestException.class);
        });
    }

    @Test
    public void onCompleteCrash() throws Throwable  {
        TestHelper.withErrorTracking(errors -> {
            CompletableObserver consumer = mock(CompletableObserver.class);
            doThrow(new TestException()).when(consumer).onComplete();

            new Completable() {
                @Override
                protected void subscribeActual(@NonNull CompletableObserver observer) {
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
