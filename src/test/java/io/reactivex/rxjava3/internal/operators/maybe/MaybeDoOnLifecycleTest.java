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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeDoOnLifecycleTest extends RxJavaTest {

    @Test
    public void success() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
        Action onDispose = mock(Action.class);

        Maybe.just(1)
        .doOnLifecycle(onSubscribe, onDispose)
        .test()
        .assertResult(1);

        verify(onSubscribe).accept(any());
        verify(onDispose, never()).run();
    }

    @Test
    public void empty() throws Throwable {
        @SuppressWarnings("unchecked")
        Consumer<? super Disposable> onSubscribe = mock(Consumer.class);
        Action onDispose = mock(Action.class);

        Maybe.empty()
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

        Maybe.error(new TestException())
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

            new Maybe<Integer>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super Integer> observer) {
                    observer.onSubscribe(bs);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                    observer.onSuccess(1);
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

            MaybeSubject<Integer> ms = MaybeSubject.create();

            TestObserver<Integer> to = ms
            .doOnLifecycle(onSubscribe, onDispose)
            .test();

            assertTrue(ms.hasObservers());

            to.dispose();

            assertFalse(ms.hasObservers());

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

        MaybeSubject<Integer> ms = MaybeSubject.create();

        TestObserver<Integer> to = ms
        .doOnLifecycle(onSubscribe, onDispose)
        .test();

        assertTrue(ms.hasObservers());

        to.dispose();

        assertFalse(ms.hasObservers());

        verify(onSubscribe).accept(any());
        verify(onDispose).run();
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(MaybeSubject.create().doOnLifecycle(d -> { }, () -> { }));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(m -> m.doOnLifecycle(d -> { }, () -> { }));
    }
}
