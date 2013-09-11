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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Subscription;
import rx.util.CompositeException;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed together.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable(v=vs.103).aspx">Rx.Net equivalent CompositeDisposable</a>
 */
public class CompositeSubscription implements Subscription {

    /*
     * The reason 'synchronized' is used on 'add' and 'unsubscribe' is because AtomicBoolean/ConcurrentLinkedQueue are both being modified so it needs to be done atomically.
     * 
     * TODO evaluate whether use of synchronized is a performance issue here and if it's worth using an atomic state machine or other non-locking approach
     */
    private AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private final LinkedBlockingDeque<Subscription> subscriptions = new LinkedBlockingDeque<Subscription>();

    public CompositeSubscription(List<Subscription> subscriptions) {
        this.subscriptions.addAll(subscriptions);
    }

    public CompositeSubscription(Subscription... subscriptions) {
        for (Subscription s : subscriptions) {
            this.subscriptions.add(s);
        }
    }

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    public synchronized void add(Subscription s) {
        if (unsubscribed.get()) {
            s.unsubscribe();
        } else {
            subscriptions.add(s);
        }
    }

    /**
     * Remove the last Subscription that was added.
     * 
     * @return Subscription or null if none exists
     */
    public synchronized Subscription removeLast() {
        return subscriptions.pollLast();
    }

    @Override
    public synchronized void unsubscribe() {
        if (unsubscribed.compareAndSet(false, true)) {
            Collection<Throwable> es = null;
            for (Subscription s : subscriptions) {
                try {
                    s.unsubscribe();
                } catch (Throwable e) {
                    if (es == null) {
                        es = new ArrayList<Throwable>();
                    }
                    es.add(e);
                }
            }
            if (es != null) {
                throw new CompositeException("Failed to unsubscribe to 1 or more subscriptions.", es);
            }
        }
    }

    public static class UnitTest {

        @Test
        public void testSuccess() {
            final AtomicInteger counter = new AtomicInteger();
            CompositeSubscription s = new CompositeSubscription();
            s.add(new Subscription() {

                @Override
                public void unsubscribe() {
                    counter.incrementAndGet();
                }
            });

            s.add(new Subscription() {

                @Override
                public void unsubscribe() {
                    counter.incrementAndGet();
                }
            });

            s.unsubscribe();

            assertEquals(2, counter.get());
        }

        @Test
        public void testException() {
            final AtomicInteger counter = new AtomicInteger();
            CompositeSubscription s = new CompositeSubscription();
            s.add(new Subscription() {

                @Override
                public void unsubscribe() {
                    throw new RuntimeException("failed on first one");
                }
            });

            s.add(new Subscription() {

                @Override
                public void unsubscribe() {
                    counter.incrementAndGet();
                }
            });

            try {
                s.unsubscribe();
                fail("Expecting an exception");
            } catch (CompositeException e) {
                // we expect this
                assertEquals(1, e.getExceptions().size());
            }

            // we should still have unsubscribed to the second one
            assertEquals(1, counter.get());
        }
    }

}
