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

package io.reactivex.rxjava3.internal.util;

import java.util.*;

import io.reactivex.rxjava3.functions.*;

public enum ArrayListSupplier implements Supplier<List<Object>>, Function<Object, List<Object>> {
    INSTANCE;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Supplier<List<T>> asSupplier() {
        return (Supplier)INSTANCE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, O> Function<O, List<T>> asFunction() {
        return (Function)INSTANCE;
    }

    @Override
    public List<Object> get() {
        return new ArrayList<Object>();
    }

    @Override public List<Object> apply(Object o) {
        return new ArrayList<Object>();
    }
}
