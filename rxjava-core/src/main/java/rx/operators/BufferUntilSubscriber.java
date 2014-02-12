/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import java.util.LinkedList;
import java.util.Queue;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Buffers the incoming events until notified, then replays the
 * buffered events and continues as a simple pass-through subscriber.
 * @param <T> the streamed value type
 */
public class BufferUntilSubscriber<T> extends Subscriber<T> {
    /** The actual subscriber. */
    private final Subscriber<? super T> actual;
    /** The mutual exclusion for the duration of the replay. */
    private final Object gate = new Object();
    /** Queued events. */
    private final Queue<Object> queue = new LinkedList<Object>();
    /** Indicate the pass-through mode. */
    private volatile boolean passthroughMode;
    /** Null sentinel (in case queue type is changed). */
    private static final Object NULL_SENTINEL = new Object();
    /** Complete sentinel. */
    private static final Object COMPLETE_SENTINEL = new Object();
    /**
     * Container for an onError event.
     */
    private static final class ErrorSentinel {
        final Throwable t;

        public ErrorSentinel(Throwable t) {
            this.t = t;
        }
        
    }
    /**
     * Constructor that wraps the actual subscriber and shares its subscription.
     * @param actual 
     */
    public BufferUntilSubscriber(Subscriber<? super T> actual) {
        super(actual);
        this.actual = actual;
    }
    /**
     * Constructor that wraps the actual subscriber and uses the given composite
     * subscription.
     * @param actual
     * @param cs 
     */
    public BufferUntilSubscriber(Subscriber<? super T> actual, CompositeSubscription cs) {
        super(cs);
        this.actual = actual;
    }
    
    /**
     * Call this method to replay the buffered events and continue as a pass-through subscriber.
     * If already in pass-through mode, this method is a no-op.
     */
    public void enterPassthroughMode() {
        if (!passthroughMode) {
            synchronized (gate) {
                if (!passthroughMode) {
                    while (!queue.isEmpty()) {
                        Object o = queue.poll();
                        
                        if (o == NULL_SENTINEL) {
                            actual.onNext(null);
                        } else
                        if (o == COMPLETE_SENTINEL) {
                            actual.onCompleted();
                        } else
                        if (o instanceof ErrorSentinel) {
                            actual.onError(((ErrorSentinel)o).t);
                        } else
                        if (o != null) {
                            @SuppressWarnings("unchecked")
                            T v = (T)o;
                            actual.onNext(v);
                        } else {
                            throw new NullPointerException();
                        }
                    }
                    /* Test artificial back-pressure.
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (Throwable t) {
                        
                    }
                    */
                    passthroughMode = true;
                }
            }
        }
    }
    
    @Override
    public void onNext(T t) {
        if (!passthroughMode) {
            synchronized (gate) {
                if (!passthroughMode) {
                    queue.offer(t != null ? t : NULL_SENTINEL);
                    return;
                }
            }
        }
        actual.onNext(t);
    }

    @Override
    public void onError(Throwable e) {
        if (!passthroughMode) {
            synchronized (gate) {
                if (!passthroughMode) {
                    queue.offer(new ErrorSentinel(e));
                    return;
                }
            }
        }
        actual.onError(e);
    }

    @Override
    public void onCompleted() {
        if (!passthroughMode) {
            synchronized (gate) {
                if (!passthroughMode) {
                    queue.offer(COMPLETE_SENTINEL);
                    return;
                }
            }
        }
        actual.onCompleted();
    }
}
