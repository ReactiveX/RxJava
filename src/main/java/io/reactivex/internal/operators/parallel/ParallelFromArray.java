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

package io.reactivex.internal.operators.parallel;

import org.reactivestreams.*;

import io.reactivex.parallel.ParallelFlowable;

/**
 * Wraps multiple Publishers into a ParallelFlowable which runs them
 * in parallel.
 *
 * @param <T> the value type
 */
public final class ParallelFromArray<T> extends ParallelFlowable<T> {
    final Publisher<T>[] sources;

    public ParallelFromArray(Publisher<T>[] sources) {
        this.sources = sources;
    }

    @Override
    public int parallelism() {
        return sources.length;
    }

    @Override
    public void subscribe(Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;

        for (int i = 0; i < n; i++) {
            sources[i].subscribe(subscribers[i]);
        }
    }
}
