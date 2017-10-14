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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.*;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.observers.*;
import io.reactivex.internal.util.*;

/**
 * Utility methods to consume an Observable in a blocking manner with callbacks or Observer.
 */
public final class ObservableBlockingSubscribe {

    /** Utility class. */
    private ObservableBlockingSubscribe() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Subscribes to the source and calls the Observer methods on the current thread.
     * <p>
     * @param o the source ObservableSource
     * The call to dispose() is composed through.
     * @param observer the subscriber to forward events and calls to in the current thread
     * @param <T> the value type
     */
    public static <T> void subscribe(ObservableSource<? extends T> o, Observer<? super T> observer) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

        BlockingObserver<T> bs = new BlockingObserver<T>(queue);
        observer.onSubscribe(bs);

        o.subscribe(bs);
        for (;;) {
            if (bs.isDisposed()) {
                break;
            }
            Object v = queue.poll();
            if (v == null) {
                try {
                    v = queue.take();
                } catch (InterruptedException ex) {
                    bs.dispose();
                    observer.onError(ex);
                    return;
                }
            }
            if (bs.isDisposed()
                    || o == BlockingObserver.TERMINATED
                    || NotificationLite.acceptFull(v, observer)) {
                break;
            }
        }
    }

    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     * @param o the source ObservableSource
     * @param <T> the value type
     */
    public static <T> void subscribe(ObservableSource<? extends T> o) {
        BlockingIgnoringReceiver callback = new BlockingIgnoringReceiver();
        LambdaObserver<T> ls = new LambdaObserver<T>(Functions.emptyConsumer(),
        callback, callback, Functions.emptyConsumer());

        o.subscribe(ls);

        BlockingHelper.awaitForComplete(callback, ls);
        Throwable e = callback.error;
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
    }

    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param o the source ObservableSource
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @param <T> the value type
     */
    public static <T> void subscribe(ObservableSource<? extends T> o, final Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError, final Action onComplete) {
        ObjectHelper.requireNonNull(onNext, "onNext is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        subscribe(o, new LambdaObserver<T>(onNext, onError, onComplete, Functions.emptyConsumer()));
    }
}
