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

import java.util.Iterator;

import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;

/**
 * An Iterable and Iterator that crashes with TestException after the given number
 * of method invocations on iterator(), hasNext() and next().
 * 
 * @param <T> the result type
 */
public final class CrashingMappedIterable<T> implements Iterable<T> {
    int crashOnIterator;

    final int crashOnHasNext;

    final int crashOnNext;

    final Function<Integer, T> mapper;

    public CrashingMappedIterable(int crashOnIterator, int crashOnHasNext, int crashOnNext, Function<Integer, T> mapper) {
        this.crashOnIterator = crashOnIterator;
        this.crashOnHasNext = crashOnHasNext;
        this.crashOnNext = crashOnNext;
        this.mapper = mapper;
    }

    @Override
    public Iterator<T> iterator() {
        if (--crashOnIterator <= 0) {
            throw new TestException("iterator()");
        }
        return new CrashingMapperIterator<T>(crashOnHasNext, crashOnNext, mapper);
    }

    static final class CrashingMapperIterator<T> implements Iterator<T> {
        int crashOnHasNext;

        int crashOnNext;

        int count;

        final Function<Integer, T> mapper;

        CrashingMapperIterator(int crashOnHasNext, int crashOnNext, Function<Integer, T> mapper) {
            this.crashOnHasNext = crashOnHasNext;
            this.crashOnNext = crashOnNext;
            this.mapper = mapper;
        }

        @Override
        public boolean hasNext() {
            if (--crashOnHasNext <= 0) {
                throw new TestException("hasNext()");
            }
            return true;
        }

        @Override
        public T next() {
            if (--crashOnNext <= 0) {
                throw new TestException("next()");
            }
            try {
                return mapper.apply(count++);
            } catch (Throwable ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
