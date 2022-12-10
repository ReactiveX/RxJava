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

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FunctionsTest extends RxJavaTest {
    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(Functions.class);
    }

    @Test
    public void booleanSupplierPredicateReverse() throws Throwable {
        BooleanSupplier s = () -> false;

        assertTrue(Functions.predicateReverseFor(s).test(1));

        s = () -> true;

        assertFalse(Functions.predicateReverseFor(s).test(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction2() throws Throwable {
        Functions.toFunction((BiFunction<Integer, Integer, Integer>) (t1, t2) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction3() throws Throwable {
        Functions.toFunction((Function3<Integer, Integer, Integer, Integer>)
                (t1, t2, t3) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction4() throws Throwable {
        Functions.toFunction((Function4<Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction5() throws Throwable {
        Functions.toFunction((Function5<Integer, Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4, t5) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction6() throws Throwable {
        Functions.toFunction((Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4, t5, t6) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction7() throws Throwable {
        Functions.toFunction((Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4, t5, t6, t7) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction8() throws Throwable {
        Functions.toFunction((Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4, t5, t6, t7, t8) -> null).apply(new Object[20]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void toFunction9() throws Throwable {
        Functions.toFunction((Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>)
                (t1, t2, t3, t4, t5, t6, t7, t8, t9) -> null).apply(new Object[20]);
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
