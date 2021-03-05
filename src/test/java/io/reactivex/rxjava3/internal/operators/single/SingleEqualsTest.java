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

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleEqualsTest extends RxJavaTest {

    @Test
    public void bothSucceedEqual() {
        Single.sequenceEqual(Single.just(1), Single.just(1))
        .test()
        .assertResult(true);
    }

    @Test
    public void bothSucceedNotEqual() {
        Single.sequenceEqual(Single.just(1), Single.just(2))
        .test()
        .assertResult(false);
    }

    @Test
    public void firstSucceedOtherError() {
        Single.sequenceEqual(Single.just(1), Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void firstErrorOtherSucceed() {
        Single.sequenceEqual(Single.error(new TestException()), Single.just(1))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void bothError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Single.sequenceEqual(Single.error(new TestException("One")), Single.error(new TestException("Two")))
            .to(TestHelper.<Boolean>testConsumer())
            .assertFailureAndMessage(TestException.class, "One");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
