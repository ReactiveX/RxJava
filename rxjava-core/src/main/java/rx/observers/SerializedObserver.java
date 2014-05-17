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

import rx.Observer;

/**
 * Enforce single-threaded, serialized, ordered execution of onNext, onCompleted, onError.
 * <p>
 * When multiple threads are notifying they will be serialized by:
 * <p>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * <p>
 * 
 * @param <T>
 */
public class SerializedObserver<T> implements Observer<T> {
    private final Observer<? super T> actual;

    private boolean emitting = false;
    private boolean terminated = false;
    private FastList queue;

    private static final int MAX_DRAIN_ITERATION = Integer.MAX_VALUE;
    private static final Object NULL_SENTINEL = new Object();
    private static final Object COMPLETE_SENTINEL = new Object();

    static final class FastList {
        Object[] array;
        int size;

        public void add(Object o) {
            int s = size;
            Object[] a = array;
            if (a == null) {
                a = new Object[16];
                array = a;
            } else if (s == a.length) {
                Object[] array2 = new Object[s + (s >> 2)];
                System.arraycopy(a, 0, array2, 0, s);
                a = array2;
                array = a;
            }
            a[s] = o;
            size = s + 1;
        }
    }

    private static final class ErrorSentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    public SerializedObserver(Observer<? super T> s) {
        this.actual = s;
    }

    @Override
    public void onCompleted() {
        FastList list;
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (emitting) {
                if (queue == null) {
                    queue = new FastList();
                }
                queue.add(COMPLETE_SENTINEL);
                return;
            }
            emitting = true;
            list = queue;
            queue = null;
        }
        drainQueue(list);
        actual.onCompleted();
    }

    @Override
    public void onError(final Throwable e) {
        FastList list;
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (emitting) {
                if (queue == null) {
                    queue = new FastList();
                }
                queue.add(new ErrorSentinel(e));
                return;
            }
            emitting = true;
            list = queue;
            queue = null;
        }
        drainQueue(list);
        actual.onError(e);
    }

    @Override
    public void onNext(T t) {
        FastList list;

        synchronized (this) {
            if (terminated) {
                return;
            }
            if (emitting) {
                if (queue == null) {
                    queue = new FastList();
                }
                queue.add(t != null ? t : NULL_SENTINEL);
                // another thread is emitting so we add to the queue and return
                return;
            }
            // we can emit
            emitting = true;
            // reference to the list to drain before emitting our value
            list = queue;
            queue = null;
        }

        // we only get here if we won the right to emit, otherwise we returned in the if(emitting) block above
        boolean skipFinal = false;
        try {
            int iter = MAX_DRAIN_ITERATION;
            do {
                drainQueue(list);
                if (iter == MAX_DRAIN_ITERATION) {
                    // after the first draining we emit our own value
                    actual.onNext(t);
                }
                --iter;
                if (iter > 0) {
                    synchronized (this) {
                        list = queue;
                        queue = null;
                        if (list == null) {
                            emitting = false;
                            skipFinal = true;
                            return;
                        }
                    }
                }
            } while (iter > 0);
        } finally {
            if (!skipFinal) {
                synchronized (this) {
                    if (terminated) {
                        list = queue;
                        queue = null;
                    } else {
                        emitting = false;
                        list = null;
                    }
                }
            }
        }
        
        // this will only drain if terminated (done here outside of synchronized block)
        drainQueue(list);
    }
    
    void drainQueue(FastList list) {
        if (list == null || list.size == 0) {
            return;
        }
        for (Object v : list.array) {
            if (v == null) {
                break;
            }
            if (v == NULL_SENTINEL) {
                actual.onNext(null);
            } else if (v == COMPLETE_SENTINEL) {
                actual.onCompleted();
            } else if (v.getClass() == ErrorSentinel.class) {
                actual.onError(((ErrorSentinel) v).e);
            } else {
                @SuppressWarnings("unchecked")
                T t = (T)v;
                actual.onNext(t);
            }
        }
    }
}