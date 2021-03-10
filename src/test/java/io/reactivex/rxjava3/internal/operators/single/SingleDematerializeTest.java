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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleDematerializeTest extends RxJavaTest {

    @Test
    public void success() {
        Single.just(Notification.createOnNext(1))
        .dematerialize(Functions.identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void empty() {
        Single.just(Notification.<Integer>createOnComplete())
        .dematerialize(Functions.identity())
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Single.<Notification<Integer>>error(new TestException())
        .dematerialize(Functions.identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorNotification() {
        Single.just(Notification.<Integer>createOnError(new TestException()))
        .dematerialize(Functions.identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(v -> v.dematerialize((Function)Functions.identity()));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(SingleSubject.<Notification<Integer>>create().dematerialize(Functions.identity()));
    }

    @Test
    public void selectorCrash() {
        Single.just(Notification.createOnNext(1))
        .dematerialize((Function<Notification<Integer>, Notification<Integer>>) v -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorNull() {
        Single.just(Notification.createOnNext(1))
        .dematerialize(Functions.justFunction((Notification<Integer>)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void selectorDifferentType() {
        Single.just(Notification.createOnNext(1))
        .dematerialize(v -> Notification.createOnNext("Value-" + 1))
        .test()
        .assertResult("Value-1");
    }
}
