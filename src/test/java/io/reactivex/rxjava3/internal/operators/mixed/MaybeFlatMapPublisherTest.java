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

package io.reactivex.rxjava3.internal.operators.mixed;

import static org.junit.Assert.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.MaybeSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeFlatMapPublisherTest extends RxJavaTest {

    @Test
    public void cancelMain() {
        MaybeSubject<Integer> ms = MaybeSubject.create();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = ms.flatMapPublisher(Functions.justFunction(pp))
                .test();

        assertTrue(ms.hasObservers());
        assertFalse(pp.hasSubscribers());

        ts.cancel();

        assertFalse(ms.hasObservers());
        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void cancelOther() {
        MaybeSubject<Integer> ms = MaybeSubject.create();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = ms.flatMapPublisher(Functions.justFunction(pp))
                .test();

        assertTrue(ms.hasObservers());
        assertFalse(pp.hasSubscribers());

        ms.onSuccess(1);

        assertFalse(ms.hasObservers());
        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(ms.hasObservers());
        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mapperCrash() {
        Maybe.just(1).flatMapPublisher(new Function<Integer, Publisher<? extends Object>>() {
            @Override
            public Publisher<? extends Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToFlowable(new Function<Maybe<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Maybe<Object> m) throws Exception {
                return m.flatMapPublisher(Functions.justFunction(Flowable.never()));
            }
        });
    }
}
