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

package io.reactivex.internal.util;

import java.util.List;

import io.reactivex.functions.*;

@SuppressWarnings("rawtypes")
public enum ListAddBiConsumer implements BiFunction<List, Object, List> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    public static <T> BiFunction<List<T>, T, List<T>> instance() {
        return (BiFunction)INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List apply(List t1, Object t2) throws Exception {
        t1.add(t2);
        return t1;
    }
}
