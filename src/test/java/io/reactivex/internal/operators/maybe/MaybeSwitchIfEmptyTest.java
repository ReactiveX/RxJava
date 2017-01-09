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

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeSwitchIfEmptyTest {

    @Test
    public void nonEmpty() {
        Maybe.just(1).switchIfEmpty(Maybe.just(2)).test().assertResult(1);
    }

    @Test
    public void empty() {
        Maybe.<Integer>empty().switchIfEmpty(Maybe.just(2)).test().assertResult(2);
    }

    @Test
    public void defaultIfEmptyNonEmpty() {
        Maybe.just(1).defaultIfEmpty(2).test().assertResult(1);
    }

    @Test
    public void defaultIfEmptyEmpty() {
        Maybe.<Integer>empty().defaultIfEmpty(2).test().assertResult(2);
    }

    @Test
    public void error() {
        Maybe.<Integer>error(new TestException()).switchIfEmpty(Maybe.just(2))
        .test().assertFailure(TestException.class);
    }

    @Test
    public void errorOther() {
        Maybe.empty().switchIfEmpty(Maybe.<Integer>error(new TestException()))
        .test().assertFailure(TestException.class);
    }

    @Test
    public void emptyOtherToo() {
        Maybe.empty().switchIfEmpty(Maybe.empty())
        .test().assertResult();
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Integer> ts = pp.singleElement().switchIfEmpty(Maybe.just(2)).test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }


    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.singleElement().switchIfEmpty(Maybe.just(2)));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Integer>, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Maybe<Integer> f) throws Exception {
                return f.switchIfEmpty(Maybe.just(2));
            }
        });
    }

    @Test
    public void emptyCancelRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> pp = PublishProcessor.create();

            final TestObserver<Integer> ts = pp.singleElement().switchIfEmpty(Maybe.just(2)).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    pp.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2, Schedulers.single());
        }
    }
}
