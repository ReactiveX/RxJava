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
package rx.observers;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Producer;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.internal.util.SimpleArrayList;

/**
 * Enforces single-threaded, serialized, ordered execution of {@link #onNext}, {@link #onCompleted}, and
 * {@link #onError}.
 * <p>
 * When multiple threads are emitting and/or notifying they will be serialized by:
 * </p><ul>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * </ul>
 * 
 * @param <T>
 *          the type of items expected to be emitted to the {@code Subscriber}
 */
public class SerializedSubscriber<T> extends Subscriber<T> {

    private final Subscriber<? super T> s;
    private boolean emitting = false;
    private boolean terminated = false;
    private SimpleArrayList queue;

    private static final Object NULL_SENTINEL = new Object();
    private static final Object COMPLETE_SENTINEL = new Object();

    volatile long req = Long.MIN_VALUE; // default to not set;
    @SuppressWarnings("rawtypes")
    static final AtomicLongFieldUpdater<SerializedSubscriber> REQUESTED_UPDATER
    = AtomicLongFieldUpdater.newUpdater(SerializedSubscriber.class, "req");

    private static final class ErrorSentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }
    
    public SerializedSubscriber(Subscriber<? super T> s) {
        super(s);
        this.s = s;
    }
    
    @Override
    public void onStart() {
        s.onStart();
        s.setProducer(new Producer() {
            @Override
            public void request(long n) {
                requestInternal(n);
            }
        });
    }
    
    @Override
    public void setProducer(final Producer producer) {
        s.setProducer(new Producer() {
            @Override
            public void request(long n) {
                requestInternal(n);
                producer.request(n);
            }
        });
    }
    
    void requestInternal(long n) {
        do {
            long r = req;
            if (r == Long.MAX_VALUE) {
                return;
            }
            long u;
            if (n == Long.MAX_VALUE || r == Long.MIN_VALUE) {
                u = n;
            } else {
                u = r + n;
            }
            if (REQUESTED_UPDATER.compareAndSet(this, r, u)) {
                if (r <= 0 && u > 0) {
                    deliver(null);
                }
                break;
            }
        } while (true);
    }

    @Override
    public void onCompleted() {
        deliver(COMPLETE_SENTINEL);
    }

    @Override
    public void onError(Throwable e) {
        Exceptions.throwIfFatal(e);
        deliver(new ErrorSentinel(e));
    }

    @Override
    public void onNext(T t) {
        deliver(t != null ? t : NULL_SENTINEL);
    }

    protected void accept(Object v) {
        if (v == NULL_SENTINEL) {
            s.onNext(null);
        } else if (v == COMPLETE_SENTINEL) {
            s.onCompleted();
        } else if (v.getClass() == ErrorSentinel.class) {
            s.onError(((ErrorSentinel) v).e);
        } else {
            @SuppressWarnings("unchecked")
            T t = (T)v;
            s.onNext(t);
        }
    }

    /**
     * Try to deliver the content of the queue and the additional object o
     * if it is not null. Null o indicates a simple deliver what is already buffered.
     * @param o
     */
    private void deliver(Object o) {
        SimpleArrayList list;

        synchronized (this) {
            if (terminated && o != null) {
                return;
            }
            if (o == COMPLETE_SENTINEL) {
                terminated = true;
            }
            if (emitting) {
                if (o != null) {
                    if (queue == null) {
                        queue = new SimpleArrayList();
                    }
                    queue.add(o);
                }
                // another thread is emitting so we add to the queue and return
                return;
            }
            // we can emit
            emitting = true;
            // reference to the list to drain before emitting our value
            list = queue;
            queue = null;
        }
        boolean runFinal = true;
        try {
            boolean once = o != null;
            do {
                long r = req;
                int n = 0;
                int available = list != null ? list.size : 0;
                
                if (r == Long.MAX_VALUE || r == Long.MIN_VALUE) {
                    for (int i = 0; i < available; i++) {
                        accept(list.array[i]);
                    }
                    if (once) {
                        accept(o);
                        once = false;
                    }
                    synchronized (this) {
                        list = queue;
                        queue = null;
                        if (list == null) {
                            emitting = false;
                            runFinal = false;
                            return;
                        }                        
                    }
                } else {
                    n = (int)Math.min(r, available);
                    for (int i = 0; i < n; i++) {
                        accept(list.array[i]);
                    }
                    r -= n;
                    if (once && r > 0) {
                        accept(o);
                        n++;
                        once = false;
                    }
                    r = REQUESTED_UPDATER.addAndGet(this, -n);
                    synchronized (this) {
                        if (once) {
                            if (list == null) {
                                list = new SimpleArrayList();
                            }
                            list.add(o);
                            available++;
                        }
                        if (n < available) {
                            if (queue == null) {
                                queue = new SimpleArrayList();
                            }
                            queue.addFirst(list, n);
                            list = null;
                        } else
                        if (r > 0) {
                            list = queue;
                            queue = null;
                        }
                        if (list == null) {
                            emitting = false;
                            runFinal = false;
                            return;
                        }
                    }
                }
            } while (true);
        } finally {
            if (runFinal) {
                synchronized (this) {
                    emitting = false;
                }
            }
        }
    }

}
