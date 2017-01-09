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

/**
 * An Iterable and Iterator that crashes with TestException after the given number
 * of method invocations on iterator(), hasNext() and next().
 */
public final class CrashingIterable implements Iterable<Integer> {
    int crashOnIterator;

    final int crashOnHasNext;

    final int crashOnNext;

    public CrashingIterable(int crashOnIterator, int crashOnHasNext, int crashOnNext) {
        this.crashOnIterator = crashOnIterator;
        this.crashOnHasNext = crashOnHasNext;
        this.crashOnNext = crashOnNext;
    }

    @Override
    public Iterator<Integer> iterator() {
        if (--crashOnIterator <= 0) {
            throw new TestException("iterator()");
        }
        return new CrashingIterator(crashOnHasNext, crashOnNext);
    }

    static final class CrashingIterator implements Iterator<Integer> {
        int crashOnHasNext;

        int crashOnNext;

        int count;

        CrashingIterator(int crashOnHasNext, int crashOnNext) {
            this.crashOnHasNext = crashOnHasNext;
            this.crashOnNext = crashOnNext;
        }

        @Override
        public boolean hasNext() {
            if (--crashOnHasNext <= 0) {
                throw new TestException("hasNext()");
            }
            return true;
        }

        @Override
        public Integer next() {
            if (--crashOnNext <= 0) {
                throw new TestException("next()");
            }
            return count++;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
