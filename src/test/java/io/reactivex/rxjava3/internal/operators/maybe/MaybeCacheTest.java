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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeCacheTest extends RxJavaTest {

    @Test
    public void offlineSuccess() {
        Maybe<Integer> source = Maybe.just(1).cache();
        assertEquals(1, source.blockingGet().intValue());

        source.test()
        .assertResult(1);
    }

    @Test
    public void offlineError() {
        Maybe<Integer> source = Maybe.<Integer>error(new TestException()).cache();

        try {
            source.blockingGet();
            fail("Should have thrown");
        } catch (TestException ex) {
            // expected
        }

        source.test()
        .assertFailure(TestException.class);
    }

    @Test
    public void offlineComplete() {
        Maybe<Integer> source = Maybe.<Integer>empty().cache();

        assertNull(source.blockingGet());

        source.test()
        .assertResult();
    }

    @Test
    public void onlineSuccess() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasSubscribers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> to = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasSubscribers());

        source.test(true).assertEmpty();

        to.assertEmpty();

        pp.onNext(1);
        pp.onComplete();

        to.assertResult(1);

        source.test().assertResult(1);

        source.test(true).assertEmpty();
    }

    @Test
    public void onlineError() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasSubscribers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> to = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasSubscribers());

        source.test(true).assertEmpty();

        to.assertEmpty();

        pp.onError(new TestException());

        to.assertFailure(TestException.class);

        source.test().assertFailure(TestException.class);

        source.test(true).assertEmpty();
    }

    @Test
    public void onlineComplete() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        assertFalse(pp.hasSubscribers());

        assertNotNull(((MaybeCache<Integer>)source).source.get());

        TestObserver<Integer> to = source.test();

        assertNull(((MaybeCache<Integer>)source).source.get());

        assertTrue(pp.hasSubscribers());

        source.test(true).assertEmpty();

        to.assertEmpty();

        pp.onComplete();

        to.assertResult();

        source.test().assertResult();

        source.test(true).assertEmpty();
    }

    @Test
    public void crossCancelOnSuccess() {

        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                ts.cancel();
            }
        });

        source.toFlowable().subscribe(ts);

        pp.onNext(1);
        pp.onComplete();

        ts.assertEmpty();
    }

    @Test
    public void crossCancelOnError() {

        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(Functions.emptyConsumer(), new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                ts.cancel();
            }
        });

        source.toFlowable().subscribe(ts);

        pp.onError(new TestException());

        ts.assertEmpty();
    }

    @Test
    public void crossCancelOnComplete() {

        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        PublishProcessor<Integer> pp = PublishProcessor.create();

        Maybe<Integer> source = pp.singleElement().cache();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), new Action() {
            @Override
            public void run() throws Exception {
                ts.cancel();
            }
        });

        source.toFlowable().subscribe(ts);

        pp.onComplete();

        ts.assertEmpty();
    }

    @Test
    public void addAddRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            final Maybe<Integer> source = pp.singleElement().cache();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    source.test();
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void removeRemoveRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            final Maybe<Integer> source = pp.singleElement().cache();

            final TestObserver<Integer> to1 = source.test();
            final TestObserver<Integer> to2 = source.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    to1.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to2.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void doubleDispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        final Maybe<Integer> source = pp.singleElement().cache();

        final Disposable[] dout = { null };

        source.subscribe(new MaybeObserver<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                dout[0] = d;
            }

            @Override
            public void onSuccess(Integer value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        });

        dout[0].dispose();
        dout[0].dispose();
    }
}
