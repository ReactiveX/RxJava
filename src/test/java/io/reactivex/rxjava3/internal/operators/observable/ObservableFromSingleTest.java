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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.operators.QueueFuseable;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.testsupport.TestObserverEx;

public class ObservableFromSingleTest extends RxJavaTest {

    @Test
    public void success() {
        Observable.fromSingle(Single.just(1).hide())
        .test()
        .assertResult(1);
    }

    @Test
    public void error() {
        Observable.fromSingle(Single.error(new TestException()).hide())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelComposes() {
        SingleSubject<Integer> ms = SingleSubject.create();

        TestObserver<Integer> to = Observable.fromSingle(ms)
        .test();

        to.assertEmpty();

        assertTrue(ms.hasObservers());

        to.dispose();

        assertFalse(ms.hasObservers());
    }

    @Test
    public void asyncFusion() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.ASYNC);

        Observable.fromSingle(Single.just(1))
        .subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1);
    }

    @Test
    public void syncFusionRejected() {
        TestObserverEx<Integer> to = new TestObserverEx<>();
        to.setInitialFusionMode(QueueFuseable.SYNC);

        Observable.fromSingle(Single.just(1))
        .subscribe(to);

        to
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .assertResult(1);
    }
}
