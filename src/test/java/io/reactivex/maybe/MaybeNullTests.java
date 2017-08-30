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

package io.reactivex.maybe;


import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOperator;
import io.reactivex.MaybeSource;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;

import static org.junit.Assert.assertTrue;

public class MaybeNullTests {

    Maybe<Integer> just1 = Maybe.just(1);

    Maybe<Integer> error = Maybe.error(new TestException());

    @Test(expected = NullPointerException.class)
    public void createNull() {
        Maybe.unsafeCreate(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Maybe.defer(null);
    }

    @Test(expected = NullPointerException.class)
    public void deferReturnsNull() {
        Maybe.defer(Functions.<Maybe<Object>>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierNull() {
        Maybe.error((Callable<Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierReturnsNull() {
        Maybe.error(Functions.<Throwable>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Maybe.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Maybe.fromCallable(null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureNull() {
        Maybe.fromFuture((Future<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedFutureNull() {
        Maybe.fromFuture(null, 1, TimeUnit.SECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedUnitNull() {
        Maybe.fromFuture(new FutureTask<Object>(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        }), 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void justNull() {
        Maybe.just(null);
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableNull() {
        Maybe.merge((Iterable<Maybe<Integer>>)null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Maybe.merge(Arrays.asList(null, just1)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mergeMaybeNull() {
        Maybe.merge((Maybe<Maybe<Integer>>)null);
    }
    
    @Test(expected = NullPointerException.class)
    public void timerUnitNull() {
        Maybe.timer(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timerSchedulerNull() {
        Maybe.timer(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void usingResourceSupplierNull() {
        Maybe.using(null, new Function<Object, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Object d) {
                return just1;
            }
        }, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingMaybeSupplierNull() {
        Maybe.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, null, Functions.emptyConsumer());
    }

    @Test(expected = NullPointerException.class)
    public void usingMaybeSupplierReturnsNull() {
        Maybe.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function<Object, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Object d) {
                return null;
            }
        }, Functions.emptyConsumer()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void usingDisposeNull() {
        Maybe.using(new Callable<Object>() {
            @Override
            public Object call() {
                return 1;
            }
        }, new Function<Object, Maybe<Integer>>() {
            @Override
            public Maybe<Integer> apply(Object d) {
                return just1;
            }
        }, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableNull() {
        Maybe.zip((Iterable<Maybe<Integer>>)null, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Maybe.zip(new Iterable<Maybe<Object>>() {
            @Override
            public Iterator<Maybe<Object>> iterator() {
                return null;
            }
        }, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Maybe.zip(Arrays.asList(null, just1), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneFunctionNull() {
        Maybe.zip(Arrays.asList(just1, just1), null).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableOneFunctionReturnsNull() {
        Maybe.zip(Arrays.asList(just1, just1), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return null;
            }
        }).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Maybe> clazz = Maybe.class;
        for (int argCount = 3; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount + 1];
                Arrays.fill(params, MaybeSource.class);
                Class<?> fniClass = Class.forName("io.reactivex.functions.Function" + argCount);
                params[argCount] = fniClass;

                Object[] values = new Object[argCount + 1];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;
                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object o, Method m, Object[] a) throws Throwable {
                        return 1;
                    }
                });

                Method m = clazz.getMethod("zip", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, new InvocationHandler() {
                    @Override
                    public Object invoke(Object o, Method m1, Object[] a) throws Throwable {
                        return null;
                    }
                });
                try {
                    ((Maybe<Object>)m.invoke(null, values)).blockingGet();
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

            }

            Class<?>[] params = new Class[argCount + 1];
            Arrays.fill(params, MaybeSource.class);
            Class<?> fniClass = Class.forName("io.reactivex.functions.Function" + argCount);
            params[argCount] = fniClass;

            Object[] values = new Object[argCount + 1];
            Arrays.fill(values, just1);
            values[argCount] = null;

            Method m = clazz.getMethod("zip", params);

            try {
                m.invoke(null, values);
                Assert.fail("No exception for argCount " + argCount + " / zipper function ");
            } catch (InvocationTargetException ex) {
                if (!(ex.getCause() instanceof NullPointerException)) {
                    Assert.fail("Unexpected exception for argCount " + argCount + " / zipper function: " + ex);
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void zip2FirstNull() {
        Maybe.zip(null, just1, new BiFunction<Object, Integer, Object>() {
            @Override
            public Object apply(Object a, Integer b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zip2SecondNull() {
        Maybe.zip(just1, null, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zip2ZipperNull() {
        Maybe.zip(just1, just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zip2ZipperReturnsdNull() {
        Maybe.zip(just1, null, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer a, Object b) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipArrayNull() {
        Maybe.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, (Maybe<Integer>[])null);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipIterableTwoIsNull() {
        Maybe.zip(Arrays.asList(just1, null), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        })
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Maybe.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return 1;
            }
        }, just1, null)
        .blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayFunctionNull() {
        Maybe.zipArray(null, just1, just1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = NullPointerException.class)
    public void zipArrayFunctionReturnsNull() {
        Maybe.zipArray(new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] v) {
                return null;
            }
        }, just1, just1).blockingGet();
    }

    //**************************************************
    // Instance methods
    //**************************************************

    @Test(expected = NullPointerException.class)
    public void ambWithNull() {
        just1.ambWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        just1.compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void castNull() {
        just1.cast(null);
    }

    @Test(expected = NullPointerException.class)
    public void concatWith() {
        just1.concatWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void delayUnitNull() {
        just1.delay(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void delaySchedulerNull() {
        just1.delay(1, TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSubscribeNull() {
        just1.doOnSubscribe(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnSuccess() {
        just1.doOnSuccess(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnError() {
        error.doOnError(null);
    }

    @Test(expected = NullPointerException.class)
    public void doOnDisposeNull() {
        just1.doOnDispose(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNull() {
        just1.flatMap(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap(new Function<Integer, Maybe<Object>>() {
            @Override
            public Maybe<Object> apply(Integer v) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapPublisherNull() {
        just1.flatMapPublisher(null);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapPublisherFunctionReturnsNull() {
        just1.flatMapPublisher(new Function<Integer, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Integer v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        just1.lift(null);
    }

    @Test(expected = NullPointerException.class)
    public void liftFunctionReturnsNull() {
        just1.lift(new MaybeOperator<Object, Integer>() {
            @Override
            public MaybeObserver<? super Integer> apply(MaybeObserver<? super Object> s) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void containsNull() {
        just1.contains(null);
    }

    @Test(expected = NullPointerException.class)
    public void mergeWithNull() {
        just1.mergeWith(null);
    }

    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        just1.observeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnSupplierNull() {
        just1.onErrorReturn((Function<Throwable, Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnValueNull() {
        error.onErrorReturnItem(null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextMaybeNull() {
        error.onErrorResumeNext((Maybe<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionNull() {
        error.onErrorResumeNext((Function<Throwable, Maybe<Integer>>)null);
    }

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            error.onErrorResumeNext(new Function<Throwable, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Throwable e) {
                    return null;
                }
            }).blockingGet();
        } catch (CompositeException ex) {
            assertTrue(ex.toString(), ex.getExceptions().get(1) instanceof NullPointerException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenNull() {
        error.repeatWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        error.repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> v) {
                return null;
            }
        }).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void repeatUntilNull() {
        error.repeatUntil(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryBiPreducateNull() {
        error.retry((BiPredicate<Integer, Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void retryPredicateNull() {
        error.retry((Predicate<Throwable>)null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenNull() {
        error.retryWhen(null);
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        error.retryWhen(new Function<Flowable<? extends Throwable>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<? extends Throwable> e) {
                return null;
            }
        }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void subscribeConsumerNull() {
        just1.subscribe((Consumer<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeSingeSubscriberNull() {
        just1.subscribe((MaybeObserver<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnSuccessNull() {
        just1.subscribe(null, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) { }
        });
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnErrorNull() {
        just1.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) { }
        }, null);
    }
    @Test(expected = NullPointerException.class)
    public void subscribeSubscriberNull() {
        just1.toFlowable().subscribe((Subscriber<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        just1.subscribeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutUnitNull() {
        just1.timeout(1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSchedulerNull() {
        just1.timeout(1, TimeUnit.SECONDS, (Scheduler)null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutOther2Null() {
        just1.timeout(1, TimeUnit.SECONDS, (Maybe<Integer>)null);
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        just1.to(null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithNull() {
        just1.zipWith(null, new BiFunction<Integer, Object, Object>() {
            @Override
            public Object apply(Integer a, Object b) {
                return 1;
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFunctionNull() {
        just1.zipWith(just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFunctionReturnsNull() {
        just1.zipWith(just1, new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) {
                return null;
            }
        }).blockingGet();
    }
}
