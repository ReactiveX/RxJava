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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ObservableForEachTest extends RxJavaTest {

    @Test
    public void forEachWile() {
        final List<Object> list = new ArrayList<>();

        Observable.range(1, 5)
        .doOnNext(list::add)
        .forEachWhile(v -> v < 3);

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void forEachWileWithError() {
        final List<Object> list = new ArrayList<>();

        Observable.range(1, 5).concatWith(Observable.<Integer>error(new TestException()))
        .doOnNext(list::add)
        .forEachWhile(_ -> true, _ -> list.add(100));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(f -> f.forEachWhile(Functions.alwaysTrue()), false, 1, 1, (Object[])null);
    }

    @Test
    public void dispose() {
        PublishSubject<Integer> ps = PublishSubject.create();

        Disposable d = ps.forEachWhile(Functions.alwaysTrue());

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }

    @Test
    public void whilePredicateThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.just(1).forEachWhile(_ -> {
                throw new TestException();
            });

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
            Throwable c = errors.get(0).getCause();
            assertTrue("" + c, c instanceof TestException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void whileErrorThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.<Integer>error(new TestException("Outer"))
            .forEachWhile(Functions.alwaysTrue(), _ -> {
                throw new TestException("Inner");
            });

            TestHelper.assertError(errors, 0, CompositeException.class);

            List<Throwable> ce = TestHelper.compositeList(errors.get(0));

            TestHelper.assertError(ce, 0, TestException.class, "Outer");
            TestHelper.assertError(ce, 1, TestException.class, "Inner");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void whileCompleteThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.just(1).forEachWhile(Functions.alwaysTrue(), Functions.emptyConsumer(),
                    () -> {
                        throw new TestException();
                    });

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

}
