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

package io.reactivex.rxjava3.internal.functions;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FunctionsTest extends RxJavaTest {
    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(Functions.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void hashSetCallableEnum() {
        // inlined TestHelper.checkEnum due to access restrictions
        try {
            Method m = Functions.HashSetSupplier.class.getMethod("values");
            m.setAccessible(true);
            Method e = Functions.HashSetSupplier.class.getMethod("valueOf", String.class);
            e.setAccessible(true);

            for (Enum<HashSetSupplier> o : (Enum<HashSetSupplier>[])m.invoke(null)) {
                assertSame(o, e.invoke(null, o.name()));
            }

        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void naturalComparatorEnum() {
        // inlined TestHelper.checkEnum due to access restrictions
        try {
            Method m = Functions.NaturalComparator.class.getMethod("values");
            m.setAccessible(true);
            Method e = Functions.NaturalComparator.class.getMethod("valueOf", String.class);
            e.setAccessible(true);

            for (Enum<NaturalComparator> o : (Enum<NaturalComparator>[])m.invoke(null)) {
                assertSame(o, e.invoke(null, o.name()));
            }

        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    @Test
    public void booleanSupplierPredicateReverse() throws Throwable {
        BooleanSupplier s = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        };

        assertTrue(Functions.predicateReverseFor(s).test(1));

        s = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return true;
            }
        };

        assertFalse(Functions.predicateReverseFor(s).test(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction2() throws Throwable {
        Functions.toFunction(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction3() throws Throwable {
        Functions.toFunction(new Function3<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction4() throws Throwable {
        Functions.toFunction(new Function4<Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction5() throws Throwable {
        Functions.toFunction(new Function5<Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction6() throws Throwable {
        Functions.toFunction(new Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction7() throws Throwable {
        Functions.toFunction(new Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction8() throws Throwable {
        Functions.toFunction(new Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction9() throws Throwable {
        Functions.toFunction(new Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) throws Exception {
                return null;
            }
        }).apply(new Object[20]);
    }

    @Test
    public void identityFunctionToString() {
        assertEquals("IdentityFunction", Functions.identity().toString());
    }

    @Test
    public void emptyActionToString() {
        assertEquals("EmptyAction", Functions.EMPTY_ACTION.toString());
    }

    @Test
    public void emptyRunnableToString() {
        assertEquals("EmptyRunnable", Functions.EMPTY_RUNNABLE.toString());
    }

    @Test
    public void emptyConsumerToString() {
        assertEquals("EmptyConsumer", Functions.EMPTY_CONSUMER.toString());
    }

    @Test
    public void errorConsumerEmpty() throws Throwable {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Functions.ERROR_CONSUMER.accept(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            assertEquals(errors.toString(), 1, errors.size());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
