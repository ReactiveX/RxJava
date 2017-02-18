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
package io.reactivex.internal.subscribers;

import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Subscription;

import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

public abstract class BlockingBaseSubscriber<T> extends CountDownLatch
implements FlowableSubscriber<T> {

    T value;
    Throwable error;

    Subscription s;

    volatile boolean cancelled;

    public BlockingBaseSubscriber() {
        super(1);
    }

    @Override
    public final void onSubscribe(Subscription s) {
        if (SubscriptionHelper.validate(this.s, s)) {
            this.s = s;
            if (!cancelled) {
                s.request(Long.MAX_VALUE);
                if (cancelled) {
                    this.s = SubscriptionHelper.CANCELLED;
                    s.cancel();
                }
            }
        }
    }

    @Override
    public final void onComplete() {
        countDown();
    }

    /**
     * Block until the first value arrives and return it, otherwise
     * return null for an empty source and rethrow any exception.
     * @return the first value or null if the source is empty
     */
    public final T blockingGet() {
        if (getCount() != 0) {
            try {
                BlockingHelper.verifyNonBlocking();
                await();
            } catch (InterruptedException ex) {
                Subscription s = this.s;
                this.s = SubscriptionHelper.CANCELLED;
                if (s != null) {
                    s.cancel();
                }
                throw ExceptionHelper.wrapOrThrow(ex);
            }
        }

        Throwable e = error;
        if (e != null) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
        return value;
    }
}
