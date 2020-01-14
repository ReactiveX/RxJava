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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class FlowableNullTests extends RxJavaTest {

    Flowable<Integer> just1 = Flowable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test
    public void ambIterableIteratorNull() {
        Flowable.amb(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Flowable.combineLatestDelayError(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Flowable.concat(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @Test
    public void ambIterableOneIsNull() {
        Flowable.amb(Arrays.asList(Flowable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestSubscriber<Object> ts = new TestSubscriber<>();
        Flowable.fromFuture(f).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Flowable.fromIterable(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @Test
    public void generateConsumerStateNullAllowed() {
        BiConsumer<Integer, Emitter<Integer>> generator = new BiConsumer<Integer, Emitter<Integer>>() {
            @Override
            public void accept(Integer s, Emitter<Integer> o) {
                o.onComplete();
            }
        };
        Flowable.generate(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return null;
            }
        }, generator).blockingSubscribe();
    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Flowable.generate(new Supplier<Object>() {
            @Override
            public Object get() {
                return null;
            }
        }, new BiFunction<Object, Emitter<Object>, Object>() {
            @Override
            public Object apply(Object s, Emitter<Object> o) {
                o.onComplete(); return s;
            }
        }).blockingSubscribe();
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Flowable> clazz = Flowable.class;
        for (int argCount = 1; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, Object.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, 1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("just", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Flowable.merge(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Flowable.mergeDelayError(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Flowable.zip(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2IteratorNull() {
        Flowable.zip(new Iterable<Publisher<Object>>() {
            @Override
            public Iterator<Publisher<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) {
                return 1;
            }
        }, true, 128).blockingLast();
    }

    //*************************************************************
    // Instance methods
    //*************************************************************

    @Test(expected = NullPointerException.class)
    public void concatMapIterableIteratorNull() {
        just1.concatMapIterable(new Function<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> apply(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test
    public void distinctUntilChangedFunctionReturnsNull() {
        Flowable.range(1, 2).distinctUntilChanged(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) {
                return null;
            }
        }).test().assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIteratorNull() {
        just1.flatMapIterable(new Function<Integer, Iterable<Object>>() {
            @Override
            public Iterable<Object> apply(Integer v) {
                return new Iterable<Object>() {
                    @Override
                    public Iterator<Object> iterator() {
                        return null;
                    }
                };
            }
        }).blockingSubscribe();
    }

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            Flowable.error(new TestException()).onErrorResumeNext(new Function<Throwable, Publisher<Object>>() {
                @Override
                public Publisher<Object> apply(Throwable e) {
                    return null;
                }
            }).blockingSubscribe();
            fail("Should have thrown");
        } catch (CompositeException ex) {
            List<Throwable> errors = ex.getExceptions();
            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, NullPointerException.class);
            assertEquals(2, errors.size());
        }
    }

    @Test
    public void onErrorReturnFunctionReturnsNull() {
        try {
            Flowable.error(new TestException()).onErrorReturn(new Function<Throwable, Object>() {
                @Override
                public Object apply(Throwable e) {
                    return null;
                }
            }).blockingSubscribe();
            fail("Should have thrown");
        } catch (CompositeException ex) {
            List<Throwable> errors = TestHelper.compositeList(ex);

            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, NullPointerException.class, "The valueSupplier returned a null value");
        }
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableIteratorNull() {
        just1.startWithIterable(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test
    public void toMapValueSelectorReturnsNull() {
        just1.toMap(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) {
                return v;
            }
        }, new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test
    public void toMultiMapValueSelectorReturnsNullAllowed() {
        just1.toMap(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) {
                return v;
            }
        }, new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableIteratorNull() {
        just1.zipWith(new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return null;
            }
        }, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer a, Object b) {
                return 1;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Flowable.combineLatestDelayError(new Iterable<Flowable<Object>>() {
            @Override
            public Iterator<Flowable<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }
}
