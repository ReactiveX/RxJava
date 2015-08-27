/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.functions;

import java.util.function.Function;

@FunctionalInterface
public interface Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> extends Function<Object[], R> {
    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8);
    
    @Override
    @SuppressWarnings("unchecked")
    default R apply(Object[] a) {
        if (a.length != 8) {
            throw new IllegalArgumentException("Array of size 8 expected but got " + a.length);
        }
        return apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7]);
    }
}
