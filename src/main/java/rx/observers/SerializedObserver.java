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
import rx.exceptions.*;
import rx.internal.operators.NotificationLite;

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
 *          the type of items expected to be observed by the {@code Observer}
 */
public class SerializedObserver<T> implements Observer<T> {
    private final Observer<? super T> actual;

    private boolean emitting;
    /** Set to true if a terminal event was received. */
    private volatile boolean terminated;
    /** If not null, it indicates more work. */
    private FastList queue;
    private final NotificationLite<T> nl = NotificationLite.instance();

    /** Number of iterations without additional safepoint poll in the drain loop. */
    private static final int MAX_DRAIN_ITERATION = 1024;

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

    public SerializedObserver(Observer<? super T> s) {
        this.actual = s;
    }

    @Override
    public void onNext(T t) {
        if (terminated) {
            return;
        }
        synchronized (this) {
            if (terminated) {
                return;
            }
            if (emitting) {
                FastList list = queue;
                if (list == null) {
                    list = new FastList();
                    queue = list;
                }
                list.add(nl.next(t));
                return;
            }
            emitting = true;
        }
        try {
            actual.onNext(t);
        } catch (Throwable e) {
            terminated = true;
            Exceptions.throwOrReport(e, actual, t);
            return;
        }
        for (;;) {
            for (int i = 0; i < MAX_DRAIN_ITERATION; i++) {
                FastList list;
                synchronized (this) {
                    list = queue;
                    if (list == null) {
                        emitting = false;
                        return;
                    }
                    queue = null;
                }
                for (Object o : list.array) {
                    if (o == null) {
                        break;
                    }
                    try {
                        if (nl.accept(actual, o)) {
                            terminated = true;
                            return;
                        }
                    } catch (Throwable e) {
                        terminated = true;
                        Exceptions.throwIfFatal(e);
                        actual.onError(OnErrorThrowable.addValueAsLastCause(e, t));
                        return;
                    }
                }
            }
        }
    }
    
    @Override
    public void onError(final Throwable e) {
        Exceptions.throwIfFatal(e);
        if (terminated) {
            return;
        }
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (emitting) {
                /* 
                 * FIXME: generally, errors jump the queue but this wasn't true 
                 * for SerializedObserver and may break existing expectations. 
                 */
                FastList list = queue;
                if (list == null) {
                    list = new FastList();
                    queue = list;
                }
                list.add(nl.error(e));
                return;
            }
            emitting = true;
        }
        actual.onError(e);
    }

    @Override
    public void onCompleted() {
        if (terminated) {
            return;
        }
        synchronized (this) {
            if (terminated) {
                return;
            }
            terminated = true;
            if (emitting) {
                FastList list = queue;
                if (list == null) {
                    list = new FastList();
                    queue = list;
                }
                list.add(nl.completed());
                return;
            }
            emitting = true;
        }
        actual.onCompleted();
    }
}
