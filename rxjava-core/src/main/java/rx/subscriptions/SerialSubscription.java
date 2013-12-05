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
package rx.subscriptions;

import static rx.subscriptions.Subscriptions.empty;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rx.Subscription;

/**
 * Represents a subscription whose underlying subscription can be swapped for another subscription
 * which causes the previous underlying subscription to be unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.serialdisposable(v=vs.103).aspx">Rx.Net equivalent SerialDisposable</a>
 */
public class SerialSubscription implements Subscription {
    private final AtomicBoolean unsubscribed = new AtomicBoolean();
    private final AtomicReference<Subscription> reference = new AtomicReference<Subscription>(empty());
    private final Lock read;
    private final Lock write;

    public SerialSubscription() {
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        read = lock.readLock();
        write = lock.writeLock();
    }

    @Override
    public void unsubscribe() {
        write.lock();
        try {
            if (unsubscribed.compareAndSet(false, true)) {
                reference.getAndSet(empty()).unsubscribe();
            }
        } finally {
            write.unlock();
        }
    }

    public void setSubscription(final Subscription subscription) {
        read.lock();
        try {
            if (unsubscribed.get()) {
                subscription.unsubscribe();
            } else {
                reference.getAndSet(subscription).unsubscribe();
            }
        } finally {
            read.unlock();
        }
    }

    public Subscription getSubscription() {
        return reference.get();
    }
}
