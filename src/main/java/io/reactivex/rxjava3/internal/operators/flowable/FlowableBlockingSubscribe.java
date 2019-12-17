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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.internal.subscribers.*;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Utility methods to consume a Publisher in a blocking manner with callbacks or Subscriber.
 */
public final class FlowableBlockingSubscribe {

    /** Utility class. */
    private FlowableBlockingSubscribe() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Subscribes to the source and calls the Subscriber methods on the current thread.
     * <p>
     * @param o the source publisher
     * The cancellation and backpressure is composed through.
     * @param subscriber the subscriber to forward events and calls to in the current thread
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o, Subscriber<? super T> subscriber) {
        final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();

        BlockingSubscriber<T> bs = new BlockingSubscriber<T>(queue);

        o.subscribe(bs);

        try {
            for (;;) {
                if (bs.isCancelled()) {
                    break;
                }
                Object v = queue.poll();
                if (v == null) {
                    if (bs.isCancelled()) {
                        break;
                    }
                    BlockingHelper.verifyNonBlocking();
                    v = queue.take();
                }
                if (bs.isCancelled()) {
                    break;
                }
                if (v == BlockingSubscriber.TERMINATED
                        || NotificationLite.acceptFull(v, subscriber)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            bs.cancel();
            subscriber.onError(e);
        }
    }

    /**
     * Runs the source observable to a terminal event, ignoring any values and rethrowing any exception.
     * @param o the source publisher
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o) {
        BlockingIgnoringReceiver callback = new BlockingIgnoringReceiver();
        LambdaSubscriber<T> ls = new LambdaSubscriber<T>(Functions.emptyConsumer(),
        callback, callback, Functions.REQUEST_MAX);

        o.subscribe(ls);

        BlockingHelper.awaitForComplete(callback, ls);
        Throwable e = callback.error;
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
    }

    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param o the source publisher
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o, final Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError, final Action onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        subscribe(o, new LambdaSubscriber<T>(onNext, onError, onComplete, Functions.REQUEST_MAX));
    }

    /**
     * Subscribes to the source and calls the given actions on the current thread.
     * @param o the source publisher
     * @param onNext the callback action for each source value
     * @param onError the callback action for an error event
     * @param onComplete the callback action for the completion event.
     * @param bufferSize the number of elements to prefetch from the source Publisher
     * @param <T> the value type
     */
    public static <T> void subscribe(Publisher<? extends T> o, final Consumer<? super T> onNext,
        final Consumer<? super Throwable> onError, final Action onComplete, int bufferSize) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        ObjectHelper.verifyPositive(bufferSize, "number > 0 required");
        subscribe(o, new BoundedSubscriber<T>(onNext, onError, onComplete, Functions.boundedConsumer(bufferSize),
                bufferSize));
    }
}
