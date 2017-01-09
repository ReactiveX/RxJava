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

package io.reactivex.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;

public class MaybeDelayOtherTest {

    @Test
    public void justWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.just(1)
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void justWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.just(1)
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        ts.assertResult(1);
    }


    @Test
    public void justWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.just(1)
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "Other");
    }

    @Test
    public void emptyWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>empty()
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertResult();
    }


    @Test
    public void emptyWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>empty()
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        ts.assertResult();
    }

    @Test
    public void emptyWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>empty()
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "Other");
    }

    @Test
    public void errorWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "Main");
    }

    @Test
    public void errorWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "Main");
    }

    @Test
    public void errorWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).test();

        ts.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        ts.assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));
        assertEquals(2, list.size());

        TestHelper.assertError(list, 0, TestException.class, "Main");
        TestHelper.assertError(list, 1, TestException.class, "Other");
    }

    @Test
    public void withCompletableDispose() {
        TestHelper.checkDisposed(Completable.complete().andThen(Maybe.just(1)));
    }

    @Test
    public void withCompletableDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToMaybe(new Function<Completable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Completable c) throws Exception {
                return c.andThen(Maybe.just(1));
            }
        });
    }

    @Test
    public void withOtherPublisherDispose() {
        TestHelper.checkDisposed(Maybe.just(1).delay(Flowable.just(1)));
    }
}
