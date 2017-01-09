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

package io.reactivex.internal.observers;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.util.NotificationLite;

public final class BlockingObserver<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {

    private static final long serialVersionUID = -4875965440900746268L;

    public static final Object TERMINATED = new Object();

    final Queue<Object> queue;

    public BlockingObserver(Queue<Object> queue) {
        this.queue = queue;
    }

    @Override
    public void onSubscribe(Disposable s) {
        DisposableHelper.setOnce(this, s);
    }

    @Override
    public void onNext(T t) {
        queue.offer(NotificationLite.next(t));
    }

    @Override
    public void onError(Throwable t) {
        queue.offer(NotificationLite.error(t));
    }

    @Override
    public void onComplete() {
        queue.offer(NotificationLite.complete());
    }

    @Override
    public void dispose() {
        if (DisposableHelper.dispose(this)) {
            queue.offer(TERMINATED);
        }
    }

    @Override
    public boolean isDisposed() {
        return get() == DisposableHelper.DISPOSED;
    }
}
