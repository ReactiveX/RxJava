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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.MissingBackpressureException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableOnBackpressureErrorTest extends RxJavaTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).toFlowable(BackpressureStrategy.ERROR));
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Observable.just(1).toFlowable(BackpressureStrategy.ERROR));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable((Function<Flowable<Object>, Publisher<Object>>) FlowableOnBackpressureError::new);
    }

    @Test
    public void badSource() {
        TestHelper.<Integer>checkBadSourceFlowable(FlowableOnBackpressureError::new, false, 1, 1, 1);
    }

    @Test
    public void overflowCancels() {
        PublishSubject<Integer> ps = PublishSubject.create();

        TestSubscriber<Integer> ts = ps.toFlowable(BackpressureStrategy.ERROR)
        .test(0L);

        assertTrue(ps.hasObservers());

        ps.onNext(1);

        assertFalse(ps.hasObservers());

        ts.assertFailure(MissingBackpressureException.class);
    }
}
