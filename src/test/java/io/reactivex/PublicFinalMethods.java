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

package io.reactivex;

import static org.junit.Assert.fail;

import java.lang.reflect.*;

import org.junit.Test;

/**
 * Verifies that instance methods of the base reactive classes are all declared final.
 */
public class PublicFinalMethods {

    static void scan(Class<?> clazz) {
        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass() == clazz) {
                if ((m.getModifiers() & Modifier.STATIC) == 0) {
                    if ((m.getModifiers() & (Modifier.PUBLIC | Modifier.FINAL)) == Modifier.PUBLIC) {
                        fail("Not final: " + m);
                    }
                }
            }
        }
    }

    @Test
    public void flowable() {
        scan(Flowable.class);
    }

    @Test
    public void observable() {
        scan(Observable.class);
    }

    @Test
    public void single() {
        scan(Single.class);
    }

    @Test
    public void completable() {
        scan(Completable.class);
    }

    @Test
    public void maybe() {
        scan(Maybe.class);
    }
}
