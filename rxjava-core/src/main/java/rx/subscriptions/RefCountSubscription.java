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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

/**
 * Keeps track of the sub-subscriptions and unsubscribes the underlying
 * subscription once all sub-subscriptions have unsubscribed.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/system.reactive.disposables.refcountdisposable.aspx'>MSDN RefCountDisposable</a>
 */
public class RefCountSubscription extends AbstractAtomicSubscription {
    /** The reference to the actual subscription. */
    private volatile Subscription main;
    /** Counts the number of sub-subscriptions. */
    private int count;
    /** Indicate the request to unsubscribe from the main. */
    private boolean mainDone;
    /**
     * Create a RefCountSubscription by wrapping the given non-null Subscription.
     * @param s 
     */
    public RefCountSubscription(Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        this.main = s;
    }
    /**
     * Returns a new sub-subscription.
     * @return a new sub-subscription.
     */
    public Subscription getSubscription() {
        PlusPlusCount ppc = new PlusPlusCount();
        if (call(ppc)) {
            return new InnerSubscription();
        }
        return Subscriptions.empty();
    }
    
    @Override
    public void unsubscribe() {
        MainDone md = new MainDone();
        if (call(md)) {
            if (md.runTerminate) {
                terminate();
            }
        }
    }
    /** 
     * Terminate this subscription by unsubscribing from main and setting the
     * state to UNSUBSCRIBED.
     */
    private void terminate() {
        Subscription r = main;
        main = null;
        r.unsubscribe();
    }
    
    /** Remove an inner subscription. */
    void innerDone() {
        InnerDone id = new InnerDone();
        if (call(id)) {
            if (id.runTerminate) {
                terminate();
            }
        }
    }
    /** The individual sub-subscriptions. */
    private final class InnerSubscription extends BooleanSubscription {
        @Override
        protected void onUnsubscribe() {
            innerDone();
        }
    };
    /**
     * Execute a {@code ++count}.
     */
    private final class PlusPlusCount implements Action0 {
        @Override
        public void call() {
            ++count;
        }
    }
    /** Called from the main unsubscribe(). */
    private final class MainDone implements Func0<SubscriptionState> {
        boolean runTerminate;
        @Override
        public SubscriptionState call() {
            if (!mainDone) {
                mainDone = true;
                if (count == 0) {
                    runTerminate = true;
                    return SubscriptionState.UNSUBSCRIBED;
                }
            }
            return SubscriptionState.ACTIVE;
        }
    }
    
    /** Called from the main unsubscribe(). */
    private final class InnerDone implements Func0<SubscriptionState> {
        boolean runTerminate;
        @Override
        public SubscriptionState call() {
            if (--count == 0 && mainDone) {
                if (count == 0) {
                    runTerminate = true;
                    return SubscriptionState.UNSUBSCRIBED;
                }
            }
            return SubscriptionState.ACTIVE;
        }
    }
}