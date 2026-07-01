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

package io.reactivex.rxjava4.internal.operators.single;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SingleBlockingSubscribeTest extends RxJavaTest{

    @Test
    public void noArgSuccess() {
        Single.just(1)
        .blockingSubscribe();
    }

    @Test
    public void noArgSuccessAsync() {
        Single.just(1)
        .delay(100, TimeUnit.MILLISECONDS)
        .blockingSubscribe();
    }

    @Test
    public void noArgError() throws Throwable {
        withErrorTracking(errors -> {
            Single.error(new TestException())
            .blockingSubscribe();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void noArgErrorAsync() throws Throwable {
        withErrorTracking(errors -> {
            Single.error(new TestException())
            .delay(100, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
            .blockingSubscribe();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void oneArgSuccess() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<Integer> success = mock(Consumer.class);

        Single.just(1)
        .blockingSubscribe(success);

        verify(success).accept(1);
    }

    @Test
    public void oneArgSuccessAsync() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<Integer> success = mock(Consumer.class);

        Single.just(1)
        .delay(50, TimeUnit.MILLISECONDS)
        .blockingSubscribe(success);

        verify(success).accept(1);
    }

    @Test
    public void oneArgSuccessFails() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            doThrow(new TestException()).when(success).accept(any());

            Single.just(1)
            .blockingSubscribe(success);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            verify(success).accept(1);
        });
    }

    @Test
    public void oneArgError() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);

            Single.<Integer>error(new TestException())
            .blockingSubscribe(success);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            verify(success, never()).accept(any());
        });
    }

    @Test
    public void oneArgErrorAsync() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);

            Single.<Integer>error(new TestException())
            .delay(50, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
            .blockingSubscribe(success);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            verify(success, never()).accept(any());
        });
    }

    @Test
    public void twoArgSuccess() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<Integer> success = mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<? super Throwable> consumer = mock(Consumer.class);

        Single.just(1)
        .blockingSubscribe(success, consumer);

        verify(success).accept(1);
        verify(consumer, never()).accept(any());
    }

    @Test
    public void twoArgSuccessAsync() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<Integer> success = mock(Consumer.class);
        @SuppressWarnings("unchecked")
        Consumer<? super Throwable> consumer = mock(Consumer.class);

        Single.just(1)
        .delay(50, TimeUnit.MILLISECONDS)
        .blockingSubscribe(success, consumer);

        verify(success).accept(any());
        verify(consumer, never()).accept(any());
    }

    @Test
    public void twoArgSuccessFails() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            doThrow(new TestException()).when(success).accept(any());
            @SuppressWarnings("unchecked")
            Consumer<? super Throwable> consumer = mock(Consumer.class);

            Single.just(1)
            .blockingSubscribe(success, consumer);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            verify(success).accept(any());
            verify(consumer, never()).accept(any());
        });
    }

    @Test
    public void twoArgError() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<? super Throwable> consumer = mock(Consumer.class);

            Single.<Integer>error(new TestException())
            .blockingSubscribe(success, consumer);

            assertTrue(errors.isEmpty(), "" + errors);

            verify(success, never()).accept(any());
            verify(consumer).accept(any(TestException.class));
        });
    }

    @Test
    public void twoArgErrorAsync() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<? super Throwable> consumer = mock(Consumer.class);

            Single.<Integer>error(new TestException())
            .delay(50, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
            .blockingSubscribe(success, consumer);

            assertTrue(errors.isEmpty(), "" + errors);

            verify(success, never()).accept(any());
            verify(consumer).accept(any(TestException.class));
        });
    }

    @Test
    public void twoArgErrorFails() throws Throwable {
        withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<? super Throwable> consumer = mock(Consumer.class);
            doThrow(new TestException()).when(consumer).accept(any());

            Single.<Integer>error(new TestException())
            .delay(50, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
            .blockingSubscribe(success, consumer);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            verify(success, never()).accept(any());
            verify(consumer).accept(any(TestException.class));
        });
    }

    @Test
    public void twoArgInterrupted() throws Throwable {
        withErrorTracking(errors -> {
            Action onDispose = mock(Action.class);

            @SuppressWarnings("unchecked")
            Consumer<Integer> success = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<? super Throwable> consumer = mock(Consumer.class);

            Thread.currentThread().interrupt();

            Single.<Integer>never()
            .doOnDispose(onDispose)
            .blockingSubscribe(success, consumer);

            assertTrue(errors.isEmpty(), "" + errors);

            verify(onDispose).run();
            verify(success, never()).accept(any());
            verify(consumer).accept(any(InterruptedException.class));
        });
    }

    @Test
    public void observerSuccess() {
        TestObserver<Integer> to = new TestObserver<>();

        Single.just(1)
        .blockingSubscribe(to);

        to.assertResult(1);
    }

    @Test
    public void observerSuccessAsync() {
        TestObserver<Integer> to = new TestObserver<>();

        Single.just(1)
        .delay(50, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
        .blockingSubscribe(to);

        to.assertResult(1);
    }

    @Test
    public void observerError() {
        TestObserver<Object> to = new TestObserver<>();

        Single.error(new TestException())
        .blockingSubscribe(to);

        to.assertFailure(TestException.class);
    }

    @Test
    public void observerErrorAsync() {
        TestObserver<Object> to = new TestObserver<>();

        Single.error(new TestException())
        .delay(50, TimeUnit.MILLISECONDS, Schedulers.computation(), true)
        .blockingSubscribe(to);

        to.assertFailure(TestException.class);
    }

    @Test
    public void observerDispose() throws Throwable {
        Action onDispose = mock(Action.class);

        TestObserver<Object> to = new TestObserver<>();
        to.dispose();

        Single.never()
        .doOnDispose(onDispose)
        .blockingSubscribe(to);

        to.assertEmpty();

        verify(onDispose).run();
    }

    @Test
    public void ovserverInterrupted() throws Throwable {
        withErrorTracking(errors -> {
            Action onDispose = mock(Action.class);

            TestObserver<Object> to = new TestObserver<>();

            Thread.currentThread().interrupt();

            Single.never()
            .doOnDispose(onDispose)
            .blockingSubscribe(to);

            assertTrue(errors.isEmpty(), "" + errors);

            verify(onDispose).run();
            to.assertFailure(InterruptedException.class);
        });
    }
}
