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

package io.reactivex.rxjava3.internal.util;

import org.reactivestreams.Subscriber;

public interface QueueDrain<T, U> {

    boolean cancelled();

    boolean done();

    Throwable error();

    boolean enter();

    long requested();

    long produced(long n);

    /**
     * Adds m to the wip counter.
     * @param m the value to add
     * @return the current value after adding m
     */
    int leave(int m);

    /**
     * Accept the value and return true if forwarded.
     * @param a the subscriber
     * @param v the value
     * @return true if the value was delivered
     */
    boolean accept(Subscriber<? super U> a, T v);
}
