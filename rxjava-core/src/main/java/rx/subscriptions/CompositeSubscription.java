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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rx.Subscription;
import rx.util.functions.Action0;

/**
 * Subscription that represents a group of Subscriptions that are unsubscribed
 * together.
 *
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.compositedisposable.aspx">Rx.Net
 *      equivalent CompositeDisposable</a>
 */
public class CompositeSubscription extends AbstractAtomicSubscription {
    /** The tracked subscriptions. */
    protected final Set<Subscription> subscriptions = new LinkedHashSet<Subscription>();
    public CompositeSubscription(final Subscription... subscriptions) {
        this.subscriptions.addAll(Arrays.asList(subscriptions));
    }
    
    public void add(final Subscription s) {
        Add a = new Add(s);
        if (!call(a) && s != null) {
            s.unsubscribe();
        }
    }
    
    public void remove(final Subscription s) {
        Remove r = new Remove(s);
        call(r);
        if (s != null) {
            s.unsubscribe();
        }
    }
    
    public void clear() {
        Clear c = new Clear();
        if (call(c)) {
            Subscriptions.unsubscribeAll(c.list);
        }
    }
    
    @Override
    public void unsubscribe() {
        Clear c = new Clear();
        if (callAndSet(SubscriptionState.UNSUBSCRIBED, c)) {
            Subscriptions.unsubscribeAll(c.list);
        }
    }
    /** Add a subscription. */
    private final class Add implements Action0 {
        final Subscription s;
        public Add(Subscription s) {
            this.s = s;
        }
        @Override
        public void call() {
            subscriptions.add(s);
        }
    }
    /** Remove a subscription if present. */
    private final class Remove implements Action0 {
        final Subscription s;
        boolean found;
        public Remove(Subscription s) {
            this.s = s;
        }
        @Override
        public void call() {
            found = subscriptions.remove(s);
        }
    }
    /**
     * Clears and returns the subscriptions from this composite.
     */
    private final class Clear implements Action0 {
        List<Subscription> list;
        @Override
        public void call() {
            list = new ArrayList<Subscription>(subscriptions);
            subscriptions.clear();
        }
    }
}
