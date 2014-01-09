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
package rx.schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Combines standard {@link Subscription#unsubscribe()} functionality with ability to skip execution if an unsubscribe occurs before the {@link #call()} method is invoked.
 */
/* package */class DiscardableAction<T> implements Func1<Scheduler, Subscription>, Subscription {
    private final Func2<? super Scheduler, ? super T, ? extends Subscription> underlying;
    private final T state;

    private final SafeObservableSubscription wrapper = new SafeObservableSubscription();
    private final AtomicBoolean ready = new AtomicBoolean(true);

    public DiscardableAction(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> underlying) {
        this.state = state;
        this.underlying = underlying;
    }

    @Override
    public Subscription call(Scheduler scheduler) {
        if (ready.compareAndSet(true, false)) {
            Subscription subscription = underlying.call(scheduler, state);
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
