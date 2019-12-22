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

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeCallbackObserverTest extends RxJavaTest {

    @Test
    public void dispose() {
        MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<>(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.EMPTY_ACTION);

        Disposable d = Disposable.empty();

        mo.onSubscribe(d);

        assertFalse(mo.isDisposed());

        mo.dispose();

        assertTrue(mo.isDisposed());

        assertTrue(d.isDisposed());
    }

    @Test
    public void onSuccessCrashes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<>(
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object v) throws Exception {
                            throw new TestException();
                        }
                    },
                    Functions.emptyConsumer(),
                    Functions.EMPTY_ACTION);

            mo.onSubscribe(Disposable.empty());

            mo.onSuccess(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorCrashes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<>(
                    Functions.emptyConsumer(),
                    new Consumer<Object>() {
                        @Override
                        public void accept(Object v) throws Exception {
                            throw new TestException("Inner");
                        }
                    },
                    Functions.EMPTY_ACTION);

            mo.onSubscribe(Disposable.empty());

            mo.onError(new TestException("Outer"));

            TestHelper.assertError(errors, 0, CompositeException.class);

            List<Throwable> ce = TestHelper.compositeList(errors.get(0));

            TestHelper.assertError(ce, 0, TestException.class, "Outer");
            TestHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCrashes() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            MaybeCallbackObserver<Object> mo = new MaybeCallbackObserver<>(
                    Functions.emptyConsumer(),
                    Functions.emptyConsumer(),
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            throw new TestException();
                        }
                    });

            mo.onSubscribe(Disposable.empty());

            mo.onComplete();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorMissingShouldReportNoCustomOnError() {
        MaybeCallbackObserver<Integer> o = new MaybeCallbackObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.ON_ERROR_MISSING,
                Functions.EMPTY_ACTION);

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        MaybeCallbackObserver<Integer> o = new MaybeCallbackObserver<>(Functions.<Integer>emptyConsumer(),
                Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION);

        assertTrue(o.hasCustomOnError());
    }
}
