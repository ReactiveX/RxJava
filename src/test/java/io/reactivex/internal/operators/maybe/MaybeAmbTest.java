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
import java.util.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeAmbTest {

    @Test
    public void ambLots() {
        List<Maybe<Integer>> ms = new ArrayList<Maybe<Integer>>();

        for (int i = 0; i < 32; i++) {
            ms.add(Maybe.<Integer>never());
        }

        ms.add(Maybe.just(1));

        Maybe.amb(ms)
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambFirstDone() {
        Maybe.amb(Arrays.asList(Maybe.just(1), Maybe.just(2)))
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dispose() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        to.dispose();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp0 = PublishProcessor.create();
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();

                final TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp0.singleElement(), pp1.singleElement()))
                .test();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        pp0.onError(ex);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        pp1.onError(ex);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }
}
