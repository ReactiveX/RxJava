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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.util.CrashingMappedIterable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleZipIterableTest extends RxJavaTest {

    final Function<Object[], Object> addString = new Function<Object[], Object>() {
        @Override
        public Object apply(Object[] a) throws Exception {
            return Arrays.toString(a);
        }
    };

    @Test
    public void firstError() {
        Single.zip(Arrays.asList(Single.error(new TestException()), Single.just(1)), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void secondError() {
        Single.zip(Arrays.asList(Single.just(1), Single.<Integer>error(new TestException())), addString)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Object> to = Single.zip(Arrays.asList(pp.single(0), pp.single(0)), addString)
        .test();

        assertTrue(pp.hasSubscribers());

        to.dispose();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void zipperThrows() {
        Single.zip(Arrays.asList(Single.just(1), Single.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] b) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void zipperReturnsNull() {
        Single.zip(Arrays.asList(Single.just(1), Single.just(2)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void middleError() {
        PublishProcessor<Integer> pp0 = PublishProcessor.create();
        PublishProcessor<Integer> pp1 = PublishProcessor.create();

        TestObserver<Object> to = Single.zip(
                Arrays.asList(pp0.single(0), pp1.single(0), pp0.single(0)), addString)
        .test();

        pp1.onError(new TestException());

        assertFalse(pp0.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishProcessor<Integer> pp0 = PublishProcessor.create();
                final PublishProcessor<Integer> pp1 = PublishProcessor.create();

                final TestObserver<Object> to = Single.zip(
                        Arrays.asList(pp0.single(0), pp1.single(0)), addString)
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

                TestHelper.race(r1, r2);

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
        Single.zip(new CrashingMappedIterable<>(1, 100, 100, new Function<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }), addString)
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextThrows() {
        Single.zip(new CrashingMappedIterable<>(100, 20, 100, new Function<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }), addString)
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextThrows() {
        Single.zip(new CrashingMappedIterable<>(100, 100, 5, new Function<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        }), addString)
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Single.zip(Arrays.asList(null, Single.just(1)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableTwoIsNull() {
        Single.zip(Arrays.asList(Single.just(1), null), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @Test
    public void emptyIterable() {
        Single.zip(Collections.<SingleSource<Integer>>emptyList(), new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(Object[] a) throws Exception {
                return a;
            }
        })
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void oneIterable() {
        Single.zip(Collections.singleton(Single.just(1)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return (Integer)a[0] + 1;
            }
        })
        .test()
        .assertResult(2);
    }

    @Test
    public void singleSourceZipperReturnsNull() {
        Single.zip(Arrays.asList(Single.just(1)), Functions.justFunction(null))
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The zipper returned a null value");
    }

    @Test
    public void singleSourcesInIterable() {
        SingleSource<Integer> source = new SingleSource<Integer>() {
            @Override
            public void subscribe(SingleObserver<? super Integer> observer) {
                Single.just(1).subscribe(observer);
            }
        };

        Single.zip(Arrays.asList(source, source), new Function<Object[], Integer>() {
            @Override
            public Integer apply(Object[] t) throws Throwable {
                return 2;
            }
        })
        .test()
        .assertResult(2);
    }
}
