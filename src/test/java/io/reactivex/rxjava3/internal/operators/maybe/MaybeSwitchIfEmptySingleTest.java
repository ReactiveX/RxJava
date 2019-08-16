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

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.HasUpstreamMaybeSource;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class MaybeSwitchIfEmptySingleTest extends RxJavaTest {

    @Test
    public void nonEmpty() {
        Maybe.just(1).switchIfEmpty(Single.just(2)).test().assertResult(1);
    }

    @Test
    public void empty() {
        Maybe.<Integer>empty().switchIfEmpty(Single.just(2)).test().assertResult(2);
    }

    @Test
    public void error() {
        Maybe.<Integer>error(new TestException()).switchIfEmpty(Single.just(2))
        .test().assertFailure(TestException.class);
    }

    @Test
    public void errorOther() {
        Maybe.empty().switchIfEmpty(Single.<Integer>error(new TestException()))
        .test().assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Integer> to = pp.singleElement().switchIfEmpty(Single.just(2)).test();

        assertTrue(pp.hasSubscribers());

        to.dispose();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().switchIfEmpty(Single.just(2)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybeToSingle(new Function<Maybe<Integer>, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Maybe<Integer> f) throws Exception {
                return f.switchIfEmpty(Single.just(2));
            }
        });
    }

    @Test
    public void emptyCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestObserver<Integer> to = pp.singleElement().switchIfEmpty(Single.just(2)).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void source() {
        assertSame(Maybe.empty(),
                ((HasUpstreamMaybeSource)(Maybe.<Integer>empty().switchIfEmpty(Single.just(1)))).source()
        );
    }
}
