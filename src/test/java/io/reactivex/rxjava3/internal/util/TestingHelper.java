/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.reactivex.rxjava3.internal.util;

import java.util.*;

import io.reactivex.rxjava3.functions.*;

public final class TestingHelper {

    private TestingHelper() {
        // prevent instantiation
    }

    public static <T> Consumer<T> addToList(final List<T> list) {
        return new Consumer<T>() {

            @Override
            public void accept(T t) {
                list.add(t);
            }
        };
    }

    public static <T> Supplier<List<T>> supplierListCreator() {
        return new Supplier<List<T>>() {

            @Override
            public List<T> get() {
                return new ArrayList<>();
            }
        };
    }

    public static BiConsumer<Object, Object> biConsumerThrows(final RuntimeException e) {
        return new BiConsumer<Object, Object>() {

            @Override
            public void accept(Object t1, Object t2) {
                throw e;
            }
        };
    }
}
