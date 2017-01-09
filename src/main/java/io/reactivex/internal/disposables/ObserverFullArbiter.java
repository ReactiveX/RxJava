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

package io.reactivex.internal.disposables;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Performs full arbitration of Subscriber events with strict drain (i.e., old emissions of another
 * subscriber are dropped).
 *
 * @param <T> the value type
 */
public final class ObserverFullArbiter<T> extends FullArbiterPad1 implements Disposable {
    final Observer<? super T> actual;
    final SpscLinkedArrayQueue<Object> queue;

    volatile Disposable s;

    Disposable resource;

    volatile boolean cancelled;

    public ObserverFullArbiter(Observer<? super T> actual, Disposable resource, int capacity) {
        this.actual = actual;
        this.resource = resource;
        this.queue = new SpscLinkedArrayQueue<Object>(capacity);
        this.s = EmptyDisposable.INSTANCE;
    }

    @Override
    public void dispose() {
        if (!cancelled) {
            cancelled = true;
            disposeResource();
        }
    }

    @Override
    public boolean isDisposed() {
        Disposable d = resource;
        return d != null ? d.isDisposed() : cancelled;
    }

    void disposeResource() {
        Disposable d = resource;
        resource = null;
        if (d != null) {
            d.dispose();
        }
    }

    public boolean setDisposable(Disposable s) {
        if (cancelled) {
            return false;
        }

        queue.offer(this.s, NotificationLite.disposable(s));
        drain();
        return true;
    }

    public boolean onNext(T value, Disposable s) {
        if (cancelled) {
            return false;
        }

        queue.offer(s, NotificationLite.next(value));
        drain();
        return true;
    }

    public void onError(Throwable value, Disposable s) {
        if (cancelled) {
            RxJavaPlugins.onError(value);
            return;
        }
        queue.offer(s, NotificationLite.error(value));
        drain();
    }

    public void onComplete(Disposable s) {
        queue.offer(s, NotificationLite.complete());
        drain();
    }

    void drain() {
        if (wip.getAndIncrement() != 0) {
            return;
        }

        int missed = 1;

        final SpscLinkedArrayQueue<Object> q = queue;
        final Observer<? super T> a = actual;

        for (;;) {

            for (;;) {
                Object o = q.poll();
                if (o == null) {
                    break;
                }

                Object v = q.poll();

                if (o == s) {
                    if (NotificationLite.isDisposable(v)) {
                        Disposable next = NotificationLite.getDisposable(v);
                        s.dispose();
                        if (!cancelled) {
                            s = next;
                        } else {
                            next.dispose();
                        }
                    } else if (NotificationLite.isError(v)) {
                        q.clear();
                        disposeResource();

                        Throwable ex = NotificationLite.getError(v);
                        if (!cancelled) {
                            cancelled = true;
                            a.onError(ex);
                        } else {
                            RxJavaPlugins.onError(ex);
                        }
                    } else if (NotificationLite.isComplete(v)) {
                        q.clear();
                        disposeResource();

                        if (!cancelled) {
                            cancelled = true;
                            a.onComplete();
                        }
                    } else {
                        a.onNext(NotificationLite.<T>getValue(v));
                    }
                }
            }

            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }
}

/** Pads the object header away. */
class FullArbiterPad0 {
    volatile long p1a, p2a, p3a, p4a, p5a, p6a, p7a;
    volatile long p8a, p9a, p10a, p11a, p12a, p13a, p14a, p15a;
}

/** The work-in-progress counter. */
class FullArbiterWip extends FullArbiterPad0 {
    final AtomicInteger wip = new AtomicInteger();
}

/** Pads the wip counter away. */
class FullArbiterPad1 extends FullArbiterWip {
    volatile long p1b, p2b, p3b, p4b, p5b, p6b, p7b;
    volatile long p8b, p9b, p10b, p11b, p12b, p13b, p14b, p15b;
}
