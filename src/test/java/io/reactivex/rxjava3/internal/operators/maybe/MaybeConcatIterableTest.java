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

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.util.CrashingMappedIterable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeConcatIterableTest extends RxJavaTest {

    @Test
    public void take() {
        Maybe.concat(Arrays.asList(Maybe.just(1), Maybe.just(2), Maybe.just(3)))
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void iteratorThrows() {
        Maybe.concat(new Iterable<MaybeSource<Object>>() {
            @Override
            public Iterator<MaybeSource<Object>> iterator() {
                throw new TestException("iterator()");
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void error() {
        Maybe.concat(Arrays.asList(Maybe.just(1), Maybe.<Integer>error(new TestException()), Maybe.just(3)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void successCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestSubscriber<Integer> ts = Maybe.concat(Arrays.asList(pp.singleElement()))
            .test();

            pp.onNext(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void hasNextThrows() {
        Maybe.concat(new CrashingMappedIterable<>(100, 1, 100, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return Maybe.just(1);
            }
        }))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextThrows() {
        Maybe.concat(new CrashingMappedIterable<>(100, 100, 1, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return Maybe.just(1);
            }
        }))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void nextReturnsNull() {
        Maybe.concat(new CrashingMappedIterable<>(100, 100, 100, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return null;
            }
        }))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void noSubsequentSubscription() {
        final int[] calls = { 0 };

        Maybe<Integer> source = Maybe.create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onSuccess(1);
            }
        });

        Maybe.concat(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void noSubsequentSubscriptionDelayError() {
        final int[] calls = { 0 };

        Maybe<Integer> source = Maybe.create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onSuccess(1);
            }
        });

        Maybe.concatDelayError(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }
}
