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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.*;

public class MaybeDelayOtherTest extends RxJavaTest {

    @Test
    public void justWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.just(1)
        .delay(pp).test();

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        to.assertResult(1);
    }

    @Test
    public void justWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.just(1)
        .delay(pp).test();

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        to.assertResult(1);
    }

    @Test
    public void justWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.just(1)
        .delay(pp).to(TestHelper.<Integer>testConsumer());

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        to.assertFailureAndMessage(TestException.class, "Other");
    }

    @Test
    public void emptyWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.<Integer>empty()
        .delay(pp).test();

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void emptyWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.<Integer>empty()
        .delay(pp).test();

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void emptyWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.<Integer>empty()
        .delay(pp).to(TestHelper.<Integer>testConsumer());

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        to.assertFailureAndMessage(TestException.class, "Other");
    }

    @Test
    public void errorWithOnNext() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).to(TestHelper.<Integer>testConsumer());

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        assertFalse(pp.hasSubscribers());

        to.assertFailureAndMessage(TestException.class, "Main");
    }

    @Test
    public void errorWithOnComplete() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).to(TestHelper.<Integer>testConsumer());

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onComplete();

        assertFalse(pp.hasSubscribers());

        to.assertFailureAndMessage(TestException.class, "Main");
    }

    @Test
    public void errorWithOnError() {
        PublishProcessor<Object> pp = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.<Integer>error(new TestException("Main"))
        .delay(pp).to(TestHelper.<Integer>testConsumer());

        to.assertEmpty();

        assertTrue(pp.hasSubscribers());

        pp.onError(new TestException("Other"));

        assertFalse(pp.hasSubscribers());

        to.assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));
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

    @Test
    public void withOtherPublisherDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Maybe<Integer> c) throws Exception {
                return c.delay(Flowable.never());
            }
        });
    }

    @Test
    public void otherPublisherNextSlipsThrough() {
        Maybe.just(1).delay(new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onNext(1);
                s.onNext(2);
            }
        })
        .test()
        .assertResult(1);
    }
}
