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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;

/**
 * Subscription that swaps the underlying subscription only if
 * the associated index is greater than the current.
 * 
 * Usage example:
 * <pre>
 * IncrementalSubscription is = ...
 * 
 * long index = is.nextIndex();
 * 
 * // do some recursive work
 * Subscription s = ...
 * 
 * is.compareExchange(index, s, false);
 * </pre>
 * 
 * This will replace the current subscription only if its index
 * is less than the new index. This way, if a recursive call
 * has already set a more recent subscription, this won't
 * replace it back to an older one.
 */
public class IncrementalSubscription implements Subscription {
    /** The current index. */
    protected final AtomicLong index = new AtomicLong();
    /** The current reference. */
    protected final AtomicReference<IndexedRef> reference = new AtomicReference<IndexedRef>();
    /** The unsubscription sentinel. */
    private static final IndexedRef UNSUBSCRIBE_SENTINEL
            = new IndexedRef(Long.MAX_VALUE, Subscriptions.empty());
    
    public boolean isUnsubscribed() {
        return reference.get() == UNSUBSCRIBE_SENTINEL;
    }
    
    public IncrementalSubscription() {
        this(Subscriptions.empty());
    }
    public IncrementalSubscription(Subscription initial) {
        reference.set(new IndexedRef(0L, initial));
    }
    
    public Subscription getSubscription() {
        return reference.get().value(); // the sentinel holds empty anyway
    }

    /**
     * Generate the next index.
     * @return the next index
     */
    public long nextIndex() {
        return index.incrementAndGet();
    }
    
    /**
     * Return the current index. For testing purposes only.
     * @return the current index
     */
    /* public */protected long index() {
        return index.get();
    }
    /**
     * Sets the given subscription as the latest value.
     * @param newValue the new subscription to set
     * @param unsubscribeOld unsubscribe the old subscription?
     */
    public void setSubscription(Subscription newValue, boolean unsubscribeOld) {
        do {
            IndexedRef r = reference.get();
            if (r == UNSUBSCRIBE_SENTINEL) {
                if (newValue != null) {
                    newValue.unsubscribe();
                }
                return;
            }
            long newIndex = nextIndex();
            if (r.index() < newIndex) {
                IndexedRef newRef = new IndexedRef(newIndex, newValue);
                if (reference.compareAndSet(r, newRef)) {
                    if (unsubscribeOld) {
                        r.unsubscribe();
                    }
                    return;
                }
            }
        } while (true);
    }
    /**
     * Compare the current index with the new index and if newIndex
     * is greater, exchange the subscription with the new value
     * and optionally unsubscribe the old one.
     * @param newIndex
     * @param newValue
     * @param unsubscribeOld
     * @return true if the exchange succeeded, false if not.
     */
    public boolean compareExchange(long newIndex, Subscription newValue, boolean unsubscribeOld) {
        do {
            IndexedRef r = reference.get();
            if (r == UNSUBSCRIBE_SENTINEL) {
                if (newValue != null) {
                    newValue.unsubscribe();
                }
                return false;
            }
            if (r.index >= newIndex) {
                return false;
            }
            IndexedRef newRef = new IndexedRef(newIndex, newValue);
            if (reference.compareAndSet(r, newRef)) {
                if (unsubscribeOld) {
                    r.unsubscribe();
                }
                return true;
            }
        } while (true);
    }
    
    
    /** The indexed reference object. */
    protected static final class IndexedRef implements Subscription {
        private final long index;
        private final Subscription value;

        public IndexedRef(long index, Subscription value) {
            this.index = index;
            this.value = value;
        }

        public long index() {
            return index;
        }
        
        public Subscription value() {
            return value;
        }

        @Override
        public void unsubscribe() {
            if (value != null) {
                value.unsubscribe();
            }
        }
    }

    @Override
    public void unsubscribe() {
        reference.getAndSet(UNSUBSCRIBE_SENTINEL).unsubscribe();
    }
    
}
