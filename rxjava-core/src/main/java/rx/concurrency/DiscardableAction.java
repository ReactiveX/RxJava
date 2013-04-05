/**
 * Copyright 2013 Netflix, Inc.
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
package rx.concurrency;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func0;

/**
 * Combines standard {@link Subscription#unsubscribe()} functionality with ability to skip execution if an unsubscribe occurs before the {@link #call()} method is invoked.
 */
/* package */class DiscardableAction implements Func0<Subscription>, Subscription {
    private final Func0<Subscription> underlying;

    private final AtomicObservableSubscription wrapper = new AtomicObservableSubscription();
    private final AtomicBoolean ready = new AtomicBoolean(true);

    public DiscardableAction(Func0<Subscription> underlying) {
        this.underlying = underlying;
    }

    @Override
    public Subscription call() {
        if (ready.compareAndSet(true, false)) {
            Subscription subscription = underlying.call();
            wrapper.wrap(subscription);
            return subscription;
        }
        return wrapper;
    }

    @Override
    public void unsubscribe() {
        ready.set(false);
        wrapper.unsubscribe();
    }
}
