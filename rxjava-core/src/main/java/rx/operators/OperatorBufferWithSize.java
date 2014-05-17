/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * This operation takes
 * values from the specified {@link Observable} source and stores them in all active chunks until the buffer
 * contains a specified number of elements. The buffer is then emitted. Chunks are created after a certain
 * amount of values have been received. When the source {@link Observable} completes or produces an error,
 * the currently active chunks are emitted, and the event is propagated to all subscribed {@link Subscriber}s.
 * <p>
 * Note that this operation can produce <strong>non-connected, connected non-overlapping, or overlapping
 * chunks</strong> depending on the input parameters.
 * </p>

* @param <T> the buffered value type
 */
public final class OperatorBufferWithSize<T> implements Operator<List<T>, T> {
    final int count;
    final int skip;

    /**
     * @param count
     *            the number of elements a buffer should have before being emitted
     * @param skip
     *            the interval with which chunks have to be created. Note that when {@code skip == count} 
     *            the operator will produce non-overlapping chunks. If
     *            {@code skip < count}, this buffer operation will produce overlapping chunks and if
     *            {@code skip > count} non-overlapping chunks will be created and some values will not be pushed
     *            into a buffer at all!
     */
    public OperatorBufferWithSize(int count, int skip) {
        this.count = count;
        this.skip = skip;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
        if (count == skip) {
            return new Subscriber<T>(child) {
                List<T> buffer;
                @Override
                public void onNext(T t) {
                    if (buffer == null) {
                        buffer = new ArrayList<T>(count);
                    }
                    buffer.add(t);
                    if (buffer.size() == count) {
                        List<T> oldBuffer = buffer;
                        buffer = null;
                        child.onNext(oldBuffer);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    buffer = null;
                    child.onError(e);
                }

                @Override
                public void onCompleted() {
                    List<T> oldBuffer = buffer;
                    buffer = null;
                    if (oldBuffer != null) {
                        try {
                            child.onNext(oldBuffer);
                        } catch (Throwable t) {
                            onError(t);
                            return;
                        }
                    }
                    child.onCompleted();
                }
            };
        }
        return new Subscriber<T>(child) {
            final List<List<T>> chunks = new LinkedList<List<T>>();
            int index;
            @Override
            public void onNext(T t) {
                if (index++ % skip == 0) {
                    chunks.add(new ArrayList<T>(count));
                }
                
                Iterator<List<T>> it = chunks.iterator();
                while (it.hasNext()) {
                    List<T> chunk = it.next();
                    chunk.add(t);
                    if (chunk.size() == count) {
                        it.remove();
                        child.onNext(chunk);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                chunks.clear();
                child.onError(e);
            }
            @Override
            public void onCompleted() {
                try {
                    for (List<T> chunk : chunks) {
                        try {
                            child.onNext(chunk);
                        } catch (Throwable t) {
                            onError(t);
                            return;
                        }
                    }
                    child.onCompleted();
                } finally {
                    chunks.clear();
                }
            }
        };
    }
}
