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

package io.reactivex.internal.operators.maybe;

import org.junit.Test;
import static org.junit.Assert.*;

import io.reactivex.Maybe;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;

public class AbstractMaybeWithUpstreamTest {

    @SuppressWarnings("unchecked")
    @Test
    public void upstream() {
        Maybe<Integer> source = Maybe.just(1);

        assertSame(source, ((HasUpstreamMaybeSource<Integer>)source.map(Functions.<Integer>identity())).source());
    }
}
