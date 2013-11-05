/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Range implements Iterable<Integer> {
    private final int start;
    private final int end;
    private final int step;

    public static Range createWithCount(int start, int count) {
        return create(start, start + count);
    }

    public static Range create(int start, int end) {
        return new Range(start, end, 1);
    }

    public static Range createWithStep(int start, int end, int step) {
        return new Range(start, end, step);
    }

    private Range(int start, int end, int step) {
        this.start = start;
        this.end = end;
        this.step = step;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int current = start;

            @Override
            public boolean hasNext() {
                return current < end;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements");
                }
                int result = current;
                current += step;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Read only iterator");
            }
        };
    }

    @Override
    public String toString() {
        return "Range (" + start + ", " + end + "), step " + step;
    }
}