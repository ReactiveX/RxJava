/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.plugins;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.annotations.Experimental;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;

/**
 * Test operators that support reporting on callback crash.
 * <p>Why a separate test file? We have to ensure the anonymous inner classes
 * have predictable numbering.
 * @since 2.1.5 - experimental
 */
@Experimental
public class OnCallbackCrashTest {

    List<String> error = Collections.synchronizedList(new ArrayList<String>());

    final class CrashReport implements BiConsumer<Throwable, Object> {

        @Override
        public void accept(Throwable t1, Object t2) throws Exception {
            StringBuilder sb = new StringBuilder(128);
            sb
            .append("Class: ").append(t2.getClass().getName())
            .append(", Enclosing: ").append(t2.getClass().getEnclosingMethod());

            error.add(sb.toString());
        }
    }

    @Before
    public void before() {
        RxJavaPlugins.setOnCallbackCrash(new CrashReport());
    }

    @After
    public void after() {
        RxJavaPlugins.setOnCallbackCrash(null);
    }

    @Test
    public void map() {
        Flowable.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(error.toString(), 1, error.size());
        String s = error.get(0);
        assertEquals("Class: io.reactivex.plugins.OnCallbackCrashTest$1, Enclosing: public void io.reactivex.plugins.OnCallbackCrashTest.map()", s);
    }

    @Test
    public void mapConditional() {
        Flowable.just(1)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(error.toString(), 1, error.size());
        String s = error.get(0);
        assertEquals("Class: io.reactivex.plugins.OnCallbackCrashTest$2, Enclosing: public void io.reactivex.plugins.OnCallbackCrashTest.mapConditional()", s);
    }

    @Test
    public void mapConditional2() {
        Flowable.range(1, 2)
        .map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        })
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(NullPointerException.class);

        assertEquals(error.toString(), 1, error.size());
        String s = error.get(0);
        assertEquals("Class: io.reactivex.plugins.OnCallbackCrashTest$3, Enclosing: public void io.reactivex.plugins.OnCallbackCrashTest.mapConditional2()", s);
    }
}
