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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableDoOnLifecycleTest extends RxJavaTest {

    @Test
    public void empty() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
        Action onDispose = mock(Action.class);

        Completable.complete()
        .doOnLifecycle(onSubscribe, onDispose)
        .test()
        .assertResult();

        verify(onSubscribe).accept(any());
        verify(onDispose, never()).run();
    }

    @Test
    public void error() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
        Action onDispose = mock(Action.class);

        Completable.error(new TestException())
        .doOnLifecycle(onSubscribe, onDispose)
        .test()
        .assertFailure(TestException.class);

        verify(onSubscribe).accept(any());
        verify(onDispose, never()).run();
    }

    @Test
    public void onSubscribeCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
            Action onDispose = mock(Action.class);

            doThrow(new TestException("First")).when(onSubscribe).accept(any());

            Disposable bs = Disposable.empty();

            new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(bs);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }
            .doOnLifecycle(onSubscribe, onDispose)
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");

            verify(onSubscribe).accept(any());
            verify(onDispose, never()).run();
        });
    }

    @Test
    public void onDisposeCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            @SuppressWarnings("unchecked")
            Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
            Action onDispose = mock(Action.class);

            doThrow(new TestException("First")).when(onDispose).run();

            CompletableSubject cs = CompletableSubject.create();

            TestObserver<Void> to = cs
            .doOnLifecycle(onSubscribe, onDispose)
            .test();

            assertTrue(cs.hasObservers());

            to.dispose();

            assertFalse(cs.hasObservers());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "First");

            verify(onSubscribe).accept(any());
            verify(onDispose).run();
        });
    }

    @Test
    public void dispose() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
        Action onDispose = mock(Action.class);

        CompletableSubject cs = CompletableSubject.create();

        TestObserver<Void> to = cs
        .doOnLifecycle(onSubscribe, onDispose)
        .test();

        assertTrue(cs.hasObservers());

        to.dispose();

        assertFalse(cs.hasObservers());

        verify(onSubscribe).accept(any());
        verify(onDispose).run();
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(CompletableSubject.create().doOnLifecycle(d -> { }, () -> { }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable(m -> m.doOnLifecycle(d -> { }, () -> { }));
    }
}
