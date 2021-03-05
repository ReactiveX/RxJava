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

import java.io.IOException;

import org.junit.Test;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.*;

public class SingleOnErrorCompleteTest {

    @Test
    public void normal() {
        Single.just(1)
        .onErrorComplete()
        .test()
        .assertResult(1);
    }

    @Test
    public void error() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Single.error(new TestException())
            .onErrorComplete()
            .test()
            .assertResult();

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void errorMatches() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Single.error(new TestException())
            .onErrorComplete(error -> error instanceof TestException)
            .test()
            .assertResult();

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void errorNotMatches() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Single.error(new IOException())
            .onErrorComplete(error -> error instanceof TestException)
            .test()
            .assertFailure(IOException.class);

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void errorPredicateCrash() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            TestObserverEx<Object> to = Single.error(new IOException())
            .onErrorComplete(error -> { throw new TestException(); })
            .subscribeWith(new TestObserverEx<>())
            .assertFailure(CompositeException.class);

            TestHelper.assertError(to, 0, IOException.class);
            TestHelper.assertError(to, 1, TestException.class);

            assertTrue("" + errors, errors.isEmpty());
        });
    }

    @Test
    public void dispose() {
        SingleSubject<Integer> ss = SingleSubject.create();

        TestObserver<Integer> to = ss
                .onErrorComplete()
                .test();

        assertTrue("No subscribers?!", ss.hasObservers());

        to.dispose();

        assertFalse("Still subscribers?!", ss.hasObservers());
    }

    @Test
    public void onSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(f -> f.onErrorComplete());
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(SingleSubject.create().onErrorComplete());
    }
}
