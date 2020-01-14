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

package io.reactivex.rxjava3.observable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class ObservableNullTests extends RxJavaTest {

    Observable<Integer> just1 = Observable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test
    public void ambIterableIteratorNull() {
        Observable.amb(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Observable.combineLatest(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Observable.combineLatestDelayError(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Observable.concat(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }).blockingLast();
    }

    @Test
    public void ambIterableOneIsNull() {
        Observable.amb(Arrays.asList(Observable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestObserver<Object> to = new TestObserver<>();
        Observable.fromFuture(f).subscribe(to);
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Observable.fromIterable(new Iterable<Object>() {
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
        Observable.generate(new Supplier<Integer>() {
            @Override
            public Integer get() {
                return null;
            }
        }, generator).blockingSubscribe();
    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Observable.generate(new Supplier<Object>() {
            @Override
            public Object get() {
                return null;
            }
        }, new BiFunction<Object, Emitter<Object>, Object>() {
            @Override
            public Object apply(Object s, Emitter<Object> o) { o.onComplete(); return s; }
        }).blockingSubscribe();
    }

    public void intervalSchedulerNull() {
        Observable.interval(1, TimeUnit.SECONDS, null);
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Observable> clazz = Observable.class;
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
        Observable.merge(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Observable.mergeDelayError(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
                return null;
            }
        }, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Observable.zip(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
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
        Observable.zip(new Iterable<Observable<Object>>() {
            @Override
            public Iterator<Observable<Object>> iterator() {
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
        Observable.range(1, 2).distinctUntilChanged(new Function<Integer, Object>() {
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

}
