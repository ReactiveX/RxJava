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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.testsupport.*;

public class SingleDoOnTerminateTest extends RxJavaTest {

    @Test(expected = NullPointerException.class)
    public void doOnTerminate() {
        Single.just(1).doOnTerminate(null);
    }

    @Test
    public void doOnTerminateSuccess() {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();

        Single.just(1).doOnTerminate(new Action() {
            @Override
            public void run() throws Exception {
                atomicBoolean.set(true);
            }
        })
        .test()
        .assertResult(1);

        assertTrue(atomicBoolean.get());
    }

    @Test
    public void doOnTerminateError() {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        Single.error(new TestException()).doOnTerminate(new Action() {
            @Override
            public void run() {
                atomicBoolean.set(true);
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertTrue(atomicBoolean.get());
    }

    @Test
    public void doOnTerminateSuccessCrash() {
        Single.just(1).doOnTerminate(new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnTerminateErrorCrash() {
        TestObserverEx<Object> to = Single.error(new TestException("Outer")).doOnTerminate(new Action() {
            @Override
            public void run() {
                throw new TestException("Inner");
            }
        })
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        TestHelper.assertError(errors, 0, TestException.class, "Outer");
        TestHelper.assertError(errors, 1, TestException.class, "Inner");
    }
}
