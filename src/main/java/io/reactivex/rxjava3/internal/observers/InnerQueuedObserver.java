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

package io.reactivex.rxjava3.internal.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.util.QueueDrainHelper;

/**
 * Subscriber that can fuse with the upstream and calls a support interface
 * whenever an event is available.
 *
 * @param <T> the value type
 */
public final class InnerQueuedObserver<T>
extends AtomicReference<Disposable>
implements Observer<T>, Disposable {

    private static final long serialVersionUID = -5417183359794346637L;

    final InnerQueuedObserverSupport<T> parent;

    final int prefetch;

    SimpleQueue<T> queue;

    volatile boolean done;

    int fusionMode;

    public InnerQueuedObserver(InnerQueuedObserverSupport<T> parent, int prefetch) {
        this.parent = parent;
        this.prefetch = prefetch;
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.setOnce(this, d)) {
            if (d instanceof QueueDisposable) {
                @SuppressWarnings("unchecked")
                QueueDisposable<T> qd = (QueueDisposable<T>) d;

                int m = qd.requestFusion(QueueDisposable.ANY);
                if (m == QueueSubscription.SYNC) {
                    fusionMode = m;
                    queue = qd;
                    done = true;
                    parent.innerComplete(this);
                    return;
                }
                if (m == QueueDisposable.ASYNC) {
                    fusionMode = m;
                    queue = qd;
                    return;
                }
            }

            queue = QueueDrainHelper.createQueue(-prefetch);
        }
    }

    @Override
    public void onNext(T t) {
        if (fusionMode == QueueDisposable.NONE) {
            parent.innerNext(this, t);
        } else {
            parent.drain();
        }
    }

    @Override
    public void onError(Throwable t) {
        parent.innerError(this, t);
    }

    @Override
    public void onComplete() {
        parent.innerComplete(this);
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return DisposableHelper.isDisposed(get());
    }

    public boolean isDone() {
        return done;
    }

    public void setDone() {
        this.done = true;
    }

    public SimpleQueue<T> queue() {
        return queue;
    }

    public int fusionMode() {
        return fusionMode;
    }
}
