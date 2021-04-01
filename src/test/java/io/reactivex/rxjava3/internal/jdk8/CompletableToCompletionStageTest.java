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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableToCompletionStageTest extends RxJavaTest {

    @Test
    public void complete() throws Exception {
        Object v = Completable.complete()
                .toCompletionStage(null)
                .toCompletableFuture()
                .get();

        assertNull(v);
    }

    @Test
    public void completableFutureCancels() throws Exception {
        CompletableSubject source = CompletableSubject.create();

        CompletableFuture<Object> cf = source
                .toCompletionStage(null)
                .toCompletableFuture();

        assertTrue(source.hasObservers());

        cf.cancel(true);

        assertTrue(cf.isCancelled());

        assertFalse(source.hasObservers());
    }

    @Test
    public void completableManualCompleteCancels() throws Exception {
        CompletableSubject source = CompletableSubject.create();

        CompletableFuture<Object> cf = source
                .toCompletionStage(null)
                .toCompletableFuture();

        assertTrue(source.hasObservers());

        cf.complete(1);

        assertTrue(cf.isDone());
        assertFalse(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasObservers());

        assertEquals(1, cf.get());
    }

    @Test
    public void completableManualCompleteExceptionallyCancels() throws Exception {
        CompletableSubject source = CompletableSubject.create();

        CompletableFuture<Object> cf = source
                .toCompletionStage(null)
                .toCompletableFuture();

        assertTrue(source.hasObservers());

        cf.completeExceptionally(new TestException());

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasObservers());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void error() throws Exception {
        CompletableFuture<Object> cf = Completable.error(new TestException())
                .toCompletionStage(null)
                .toCompletableFuture();

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void sourceIgnoresCancel() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Object v = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .toCompletionStage(null)
            .toCompletableFuture()
            .get();

            assertNull(v);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void doubleOnSubscribe() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Object v = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onSubscribe(Disposable.empty());
                    observer.onComplete();
                }
            }
            .toCompletionStage(null)
            .toCompletableFuture()
            .get();

            assertNull(v);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        });
    }
}
