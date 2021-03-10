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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import java.util.List;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class SingleDoOnTest extends RxJavaTest {

    @Test
    public void doOnDispose() {
        final int[] count = { 0 };

        Single.never().doOnDispose(() -> count[0]++).test(true);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnError() {
        final Object[] event = { null };

        Single.error(new TestException()).doOnError(e -> event[0] = e)
        .test();

        assertTrue(event[0].toString(), event[0] instanceof TestException);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };

        Single.never().doOnSubscribe(d -> count[0]++).test();

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSuccess() {
        final Object[] event = { null };

        Single.just(1).doOnSuccess(e -> event[0] = e)
        .test();

        assertEquals(1, event[0]);
    }

    @Test
    public void doOnSubscribeNormal() {
        final int[] count = { 0 };

        Single.just(1).doOnSubscribe(d -> count[0]++)
        .test()
        .assertResult(1);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeError() {
        final int[] count = { 0 };

        Single.error(new TestException()).doOnSubscribe(d -> count[0]++)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeJustCrash() {

        Single.just(1).doOnSubscribe(d -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSubscribeErrorCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Single.error(new TestException("Outer")).doOnSubscribe(d -> {
                throw new TestException("Inner");
            })
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "Inner");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Outer");
        } finally {
            RxJavaPlugins.reset();
        }

    }

    @Test
    public void onErrorSuccess() {
        final int[] call = { 0 };

        Single.just(1)
        .doOnError(v -> call[0]++)
        .test()
        .assertResult(1);

        assertEquals(0, call[0]);
    }

    @Test
    public void onErrorCrashes() {
        TestObserverEx<Object> to = Single.error(new TestException("Outer"))
        .doOnError(v -> {
            throw new TestException("Inner");
        })
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnEventThrowsSuccess() {
        Single.just(1)
        .doOnEvent((v, e) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnEventThrowsError() {
        TestObserverEx<Integer> to = Single.<Integer>error(new TestException("Main"))
        .doOnEvent((v, e) -> {
            throw new TestException("Inner");
        })
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Main");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnDisposeDispose() {
        final int[] calls = { 0 };
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().doOnDispose(() -> calls[0]++));

        assertEquals(1, calls[0]);
    }

    @Test
    public void doOnDisposeSuccess() {
        final int[] calls = { 0 };

        Single.just(1)
        .doOnDispose(() -> calls[0]++)
        .test()
        .assertResult(1);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeError() {
        final int[] calls = { 0 };

        Single.error(new TestException())
        .doOnDispose(() -> calls[0]++)
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, calls[0]);
    }

    @Test
    public void doOnDisposeDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(s -> s.doOnDispose(Functions.EMPTY_ACTION));
    }

    @Test
    public void doOnDisposeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            PublishSubject<Integer> ps = PublishSubject.create();

            ps.singleOrError().doOnDispose(() -> {
                throw new TestException();
            })
            .test()
            .dispose();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doOnSuccessErrors() {
        final int[] call = { 0 };

        Single.error(new TestException())
        .doOnSuccess(v -> call[0]++)
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, call[0]);
    }

    @Test
    public void doOnSuccessCrash() {
        Single.just(1)
        .doOnSuccess(v -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposable.empty();

            new Single<Integer>() {
                @Override
                protected void subscribeActual(@NonNull SingleObserver<? super Integer> observer) {
                    observer.onSubscribe(bs);
                    observer.onError(new TestException("Second"));
                    observer.onSuccess(1);
                }
            }
            .doOnSubscribe(d -> {
                throw new TestException("First");
            })
            .to(TestHelper.<Integer>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
