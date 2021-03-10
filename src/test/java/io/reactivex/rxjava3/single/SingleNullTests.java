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

package io.reactivex.rxjava3.single;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;

public class SingleNullTests extends RxJavaTest {

    Single<Integer> just1 = Single.just(1);

    Single<Integer> error = Single.error(new TestException());

    @Test
    public void ambIterableIteratorNull() {
        Single.amb((Iterable<Single<Object>>) () -> null).test().assertError(NullPointerException.class);
    }

    @Test
    public void ambIterableOneIsNull() {
        Single.amb(Arrays.asList(null, just1))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test
    public void ambArrayOneIsNull() {
        Single.ambArray(null, just1)
            .test()
            .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Single.concat((Iterable<Single<Object>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Single.concat(Arrays.asList(just1, null)).blockingSubscribe();
    }

    @Test
    public void concatNull() throws Exception {
        int maxArgs = 4;

        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 2; argCount <= maxArgs; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, SingleSource.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("concat", params);

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
    public void deferReturnsNull() {
        Single.defer(Functions.<Single<Object>>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void errorSupplierReturnsNull() {
        Single.error(Functions.<Throwable>nullSupplier()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Single.fromCallable(() -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Single.fromFuture(f).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Single.fromFuture(f, 1, TimeUnit.SECONDS).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Single.merge((Iterable<Single<Object>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Single.merge(Arrays.asList(null, just1)).blockingSubscribe();
    }

    @Test
    public void mergeNull() throws Exception {
        int maxArgs = 4;

        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 2; argCount <= maxArgs; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, SingleSource.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("merge", params);

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
    public void usingSingleSupplierReturnsNull() {
        Single.using((Supplier<Object>) () -> 1, (Function<Object, Single<Object>>) d -> null, Functions.emptyConsumer()).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Single.zip((Iterable<Single<Object>>) () -> null, (Function<Object[], Object>) v -> 1).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableOneIsNull() {
        Single.zip(Arrays.asList(null, just1), (Function<Object[], Object>) v -> 1).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableOneFunctionReturnsNull() {
        Single.zip(Arrays.asList(just1, just1), v -> null).blockingGet();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Single> clazz = Single.class;
        for (int argCount = 3; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount + 1];
                Arrays.fill(params, SingleSource.class);
                Class<?> fniClass = Class.forName("io.reactivex.rxjava3.functions.Function" + argCount);
                params[argCount] = fniClass;

                Object[] values = new Object[argCount + 1];
                Arrays.fill(values, just1);
                values[argNull - 1] = null;
                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, (o, m, a) -> 1);

                Method m = clazz.getMethod("zip", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

                values[argCount] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { fniClass }, (o, m1, a) -> null);
                try {
                    ((Single<Object>)m.invoke(null, values)).blockingGet();
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }

            }

            Class<?>[] params = new Class[argCount + 1];
            Arrays.fill(params, SingleSource.class);
            Class<?> fniClass = Class.forName("io.reactivex.rxjava3.functions.Function" + argCount);
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
    public void zipIterableTwoIsNull() {
        Single.zip(Arrays.asList(just1, null), (Function<Object[], Object>) v -> 1)
        .blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipArrayOneIsNull() {
        Single.zipArray((Function<Object[], Object>) v -> 1, just1, null)
        .blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void zipArrayFunctionReturnsNull() {
        Single.zipArray(v -> null, just1, just1).blockingGet();
    }

    //**************************************************
    // Instance methods
    //**************************************************

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap((Function<Integer, Single<Object>>) v -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapPublisherFunctionReturnsNull() {
        just1.flatMapPublisher((Function<Integer, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void liftFunctionReturnsNull() {
        just1.lift(observer -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnsSupplierReturnsNull() {
        error.onErrorReturn(t -> null).blockingGet();
    }

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            error.onErrorResumeNext((Function<Throwable, Single<Integer>>) e -> null).blockingGet();
        } catch (CompositeException ex) {
            assertTrue(ex.toString(), ex.getExceptions().get(1) instanceof NullPointerException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        error.repeatWhen((Function<Flowable<Object>, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        error.retryWhen((Function<Flowable<? extends Throwable>, Publisher<Object>>) e -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnSuccessNull() {
        just1.subscribe(null, e -> { });
    }

    @Test(expected = NullPointerException.class)
    public void zipWithFunctionReturnsNull() {
        just1.zipWith(just1, (a, b) -> null).blockingGet();
    }
}
