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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SingleDoAfterSuccessTest extends RxJavaTest {

    final List<Integer> values = new ArrayList<>();

    final Consumer<Integer> afterSuccess = e -> values.add(-e);

    final TestObserver<Integer> to = new TestObserver<Integer>() /* NFI */ {
        @Override
        public void onNext(Integer t) {
            super.onNext(t);
            SingleDoAfterSuccessTest.this.values.add(t);
        }
    };

    @Test
    public void just() {
        Single.just(1)
        .doAfterSuccess(afterSuccess)
        .subscribeWith(to)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void error() {
        Single.<Integer>error(new TestException())
        .doAfterSuccess(afterSuccess)
        .subscribeWith(to)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void justConditional() {
        Single.just(1)
        .doAfterSuccess(afterSuccess)
        .filter(Functions.alwaysTrue())
        .subscribeWith(to)
        .assertResult(1);

        assertEquals(Arrays.asList(1, -1), values);
    }

    @Test
    public void errorConditional() {
        Single.<Integer>error(new TestException())
        .doAfterSuccess(afterSuccess)
        .filter(Functions.alwaysTrue())
        .subscribeWith(to)
        .assertFailure(TestException.class);

        assertTrue(values.isEmpty());
    }

    @Test
    public void consumerThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Single.just(1)
            .doAfterSuccess(_ -> {
                throw new TestException();
            })
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.<Integer>create().singleOrError().doAfterSuccess(afterSuccess));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle((Function<Single<Integer>, SingleSource<Integer>>) m -> m.doAfterSuccess(afterSuccess));
    }
}
