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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class CompletableDoOnTest extends RxJavaTest {

    @Test
    public void successAcceptThrows() {
        Completable.complete().doOnEvent(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorAcceptThrows() {
        TestObserverEx<Void> to = Completable.error(new TestException("Outer")).doOnEvent(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .to(TestHelper.<Void>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }

    @Test
    public void doOnDisposeCalled() {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();

        assertFalse(atomicBoolean.get());

        Completable.complete()
            .doOnDispose(new Action() {
                @Override
                public void run() throws Exception {
                    atomicBoolean.set(true);
                }
            })
            .test()
            .assertResult()
            .dispose();

        assertTrue(atomicBoolean.get());
    }

    @Test
    public void onSubscribeCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable bs = Disposable.empty();

            new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    observer.onSubscribe(bs);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }
            .doOnSubscribe(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable d) throws Exception {
                    throw new TestException("First");
                }
            })
            .to(TestHelper.<Void>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            assertTrue(bs.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
