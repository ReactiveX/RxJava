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

package io.reactivex.rxjava3.single;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.*;
import java.util.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;

public class SingleNullTests extends RxJavaTest {

    Single<Integer> just1 = Single.just(1);

    Single<Integer> error = Single.error(new TestException());

    @Test
    public void ambIterableIteratorNull() {
        Single.amb(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
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
        Single.concat(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).blockingSubscribe();
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
    public void mergeIterableIteratorNull() {
        Single.merge(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
                return null;
            }
        }).blockingSubscribe();
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
    public void zipIterableIteratorNull() {
        Single.zip(new Iterable<Single<Object>>() {
            @Override
            public Iterator<Single<Object>> iterator() {
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

    //**************************************************
    // Instance methods
    //**************************************************

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            error.onErrorResumeNext(new Function<Throwable, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Throwable e) {
                    return null;
                }
            }).blockingGet();
        } catch (CompositeException ex) {
            assertTrue(ex.toString(), ex.getExceptions().get(1) instanceof NullPointerException);
        }
    }
}
