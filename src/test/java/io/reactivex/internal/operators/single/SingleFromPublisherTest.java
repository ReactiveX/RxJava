/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.single;

import java.util.NoSuchElementException;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;

public class SingleFromPublisherTest {

    @Test
    public void just() {
        Single.fromPublisher(Flowable.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Single.fromPublisher(Flowable.range(1, 3))
        .test()
        .assertFailure(IndexOutOfBoundsException.class);
    }

    @Test
    public void empty() {
        Single.fromPublisher(Flowable.empty())
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void error() {
        Single.fromPublisher(Flowable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Single.fromPublisher(pp).test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(Single.fromPublisher(Flowable.never()));
    }
}
