/**
 * Copyright (c) 2019-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.schedulers;

import io.reactivex.rxjava3.annotations.Experimental;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class integrates RxJava with BlockHound.
 * <p>
 * It is public but only because of the ServiceLoader's limitations
 * and SHOULD NOT be considered a public API.
 */
@SuppressWarnings("Since15")
@Experimental
public final class RxJavaBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.nonBlockingThreadPredicate(new Function<Predicate<Thread>, Predicate<Thread>>() {
            @Override
            public Predicate<Thread> apply(Predicate<Thread> p) {
                return p.or(new Predicate<Thread>() {
                    @Override
                    public boolean test(Thread obj) {
                        return obj instanceof NonBlockingThread;
                    }
                });
            }
        });
    }

    @Override
    public int compareTo(BlockHoundIntegration o) {
        return 0;
    }
}
