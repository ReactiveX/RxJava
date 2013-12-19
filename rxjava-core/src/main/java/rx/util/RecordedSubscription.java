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
package rx.util;

/**
 * Records information about subscriptions and unsubscriptions.
 * <p>
 * You may static import the class to access the convenient factory methods
 * for creating finite and never unsubscribed subscriptions.
 */
public final class RecordedSubscription {
    /** Indicates that the unsubscription never took place. */
    public static final long NEVER_UNSUBSCRIBED = Long.MAX_VALUE;
    /** The subscription virtual time. */
    private final long subscribed;
    /** The unsubscription virtual time. */
    private final long unsubscribed;
    /**
     * Creates a RecordedSubscription instance with the given virtual
     * subscription time an NEVER_UNSUBSCRIBED as the virtual 
     * unsubscription time.
     * @param subscribed the virtual subscription time
     */
    public RecordedSubscription(long subscribed) {
        this(subscribed, NEVER_UNSUBSCRIBED);
    }
    /**
     * Creates a RecordedSubscription instance with the given virtual
     * subscription and unsubscription times.
     * @param subscribed the virtual subscription time
     * @param unsubscribed the virtual unsubscription time
     */
    public RecordedSubscription(long subscribed, long unsubscribed) {
        this.subscribed = subscribed;
        this.unsubscribed = unsubscribed;
    }
    /**
     * Return the recorded virtual subscription time.
     * @return the recorded virtual subscription time
     */
    public long subscribed() {
        return subscribed;
    }
    /**
     * Return the recorded virtual unsubscription time.
     * @return the recorded virtual unsubscription time 
     */
    public long unsubscribed() {
        return unsubscribed;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RecordedSubscription) {
            RecordedSubscription r = (RecordedSubscription)obj;
            return subscribed == r.subscribed && unsubscribed == r.unsubscribed; 
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 0;
        h = h * 31 + (int)((subscribed >>> 32) ^ subscribed);
        h = h * 31 + (int)((unsubscribed >>> 32) ^ unsubscribed);
        return h;
    }

    @Override
    public String toString() {
        if (unsubscribed == NEVER_UNSUBSCRIBED) {
            return "(" + subscribed + ", Never)";
        }
        return "(" + subscribed + ", " + unsubscribed + ")";
    }
    /**
     * Factory method to record a given subscription time.
     * @param start the virtual time of the subscription
     * @return the recorded subscription
     */
    public static RecordedSubscription subscribe(long start) {
        return new RecordedSubscription(start);
    }
    /**
     * Factory method to record a given subscription and unsubscription time.
     * @param start the virtual time of the subscription
     * @param end the virtual time of the unsubscription
     * @return the recorded subscription and unsubscription
     */
    public static RecordedSubscription subscribe(long start, long end) {
        return new RecordedSubscription(start, end);
        
    }
}
