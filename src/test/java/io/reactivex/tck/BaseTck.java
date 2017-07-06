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

package io.reactivex.tck;

import java.util.*;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.*;
import org.testng.annotations.Test;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;

/**
 * Base abstract class for Flowable verifications, contains support for creating
 * Iterable range of values.
 * 
 * @param <T> the element type
 */
@Test
public abstract class BaseTck<T> extends PublisherVerification<T> {

    public BaseTck() {
        this(25L);
    }

    public BaseTck(long timeout) {
        super(new TestEnvironment(timeout));
    }

    @Override
    public Publisher<T> createFailedPublisher() {
        return Flowable.error(new TestException());
    }


    @Override
    public long maxElementsFromPublisher() {
        return 1024;
    }

    /**
     * Creates an Iterable with the specified number of elements or an infinite one if
     * elements > Integer.MAX_VALUE.
     * @param elements the number of elements to return, Integer.MAX_VALUE means an infinite sequence
     * @return the Iterable
     */
    protected Iterable<Long> iterate(long elements) {
        return iterate(elements > Integer.MAX_VALUE, elements);
    }

    protected Iterable<Long> iterate(boolean useInfinite, long elements) {
        return useInfinite ? new InfiniteRange() : new FiniteRange(elements);
    }

    /**
     * Create an array of Long values, ranging from 0L to elements - 1L.
     * @param elements the number of elements to return
     * @return the array
     */
    protected Long[] array(long elements) {
        Long[] a = new Long[(int)elements];
        for (int i = 0; i < elements; i++) {
            a[i] = (long)i;
        }
        return a;
    }

    static final class FiniteRange implements Iterable<Long> {
        final long end;
        FiniteRange(long end) {
            this.end = end;
        }

        @Override
        public Iterator<Long> iterator() {
            return new FiniteRangeIterator(end);
        }

        static final class FiniteRangeIterator implements Iterator<Long> {
            final long end;
            long count;

            FiniteRangeIterator(long end) {
                this.end = end;
            }

            @Override
            public boolean hasNext() {
                return count != end;
            }

            @Override
            public Long next() {
                long c = count;
                if (c != end) {
                    count = c + 1;
                    return c;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    static final class InfiniteRange implements Iterable<Long> {
        @Override
        public Iterator<Long> iterator() {
            return new InfiniteRangeIterator();
        }

        static final class InfiniteRangeIterator implements Iterator<Long> {
            long count;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                return count++;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
