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
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.util.CrashingMappedIterable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class MaybeZipIterableTest {

    final Function<Object[], Object> addString = new Function<Object[], Object>() {
        @Override
        public Object apply(Object[] a) throws Exception {
            return Arrays.toString(a);
        }
    };

    @SuppressWarnings("unchecked")
    @Test
    public void firstError() {
        Maybe.zip(Arrays.asList(Maybe.error(new TestException()), Maybe.just(1)), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void secondError() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.<Integer>error(new TestException())), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Object> to = Maybe.zip(Arrays.asList(pp.singleElement(), pp.singleElement()), addString)
        .test();

        assertTrue(pp.hasSubscribers());

        to.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipperThrows() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] b) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipperReturnsNull() {
        Maybe.zip(Arrays.asList(Maybe.just(1), Maybe.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void middleError() {
        PublishProcessor<Integer> pp0 = PublishProcessor.create();
        PublishProcessor<Integer> pp1 = PublishProcessor.create();

        TestObserver<Object> to = Maybe.zip(
                Arrays.asList(pp0.singleElement(), pp1.singleElement(), pp0.singleElement()), addString)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp0 = PublishProcessor.create();
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();

                final TestObserver<Object> to = Maybe.zip(
                        Arrays.asList(pp0.singleElement(), pp1.singleElement()), addString)
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

    @Test
    public void iteratorThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(1, 100, 100, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(100, 20, 100, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextThrows() {
        Maybe.zip(new CrashingMappedIterable<Maybe<Integer>>(100, 100, 5, new Function<Integer, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        }), addString)
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Maybe.zip(Arrays.asList(null, Maybe.just(1)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableTwoIsNull() {
        Maybe.zip(Arrays.asList(Maybe.just(1), null), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void singleSourceZipperReturnsNull() {
        Maybe.zipArray(Functions.justFunction(null), Maybe.just(1))
        .test()
        .assertFailureAndMessage(NullPointerException.class, "The zipper returned a null value");
    }
}
