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

import java.util.concurrent.atomic.AtomicReference;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Actions;
import rx.util.functions.Func0;

/**
 * Base class for subscriptions with lock-free behavior.
 */
public abstract class AbstractAtomicSubscription implements Subscription {
    /** 
     * The subscription state for tracking the mutating and unsubscribed states. 
     */
    protected enum SubscriptionState {
        /** Indicates that the subscription may be mutated. */
        ACTIVE,
        /** Indicates that a mutation is going on. */
        MUTATING,
        /** Indicates that the subscription has been unsubscribed and no further mutation may happen. */
        UNSUBSCRIBED
    }
    /** The current state. */
    private final AtomicReference<SubscriptionState> state = new AtomicReference<SubscriptionState>(SubscriptionState.ACTIVE);
    /** 
     * Atomically sets the state. 
     * @param newState the new state
     */
    protected final void setState(SubscriptionState newState) {
        if (newState == null) {
            throw new NullPointerException("newState");
        }
        state.set(newState);
    }
    /**
     * Atomically retrieves the current state.
     * @return the current state.
     */
    protected final SubscriptionState getState() {
        return state.get();
    }
    /**
     * Executes the given action while in the MUTATING state and transitions to the supplied new state.
     * <p> 
     * Even if the {@code action} throws an exception, the state is always set to the {@code newState}.
     * @param newState the state to set after the action was called
     * @param action the action to execute while in the MUTATING state
     * @return true if the action was called, false if the subscription was unsubscribed
     */
    protected final boolean callAndSet(SubscriptionState newState, Action0 action) {
        return callAndSet(newState, action, null);
    }
    /**
     * Executes the given action and sets the state to the supplied newState or
     * executes the function and sets the state to its return value.
     * 
     * @param newState the default new state, set if action is executed or the func call throws.
     * @param action the action to execute, null if not applicable
     * @param func the function to execute, null if not applicable
     * @return true if either the action or function was executed, 
     *         false if the subscription was unsubscribed during the operation
     */
    private boolean callAndSet(SubscriptionState newState, Action0 action, Func0<SubscriptionState> func) {
        if (newState == null) {
            throw new NullPointerException("newState");
        }
        if (action == null && func == null) {
            throw new NullPointerException("action & func both null!");
        }
        if (action != null && func != null) {
            throw new NullPointerException("action & func both non-null!");
        }
        do {
            SubscriptionState s = state.get();
            if (s == SubscriptionState.UNSUBSCRIBED) {
                return false;
            }
            if (s == SubscriptionState.MUTATING) {
                continue;
            }
            if (state.compareAndSet(s, SubscriptionState.MUTATING)) {
                SubscriptionState toSet = newState;
                try {
                    if (action != null) {
                        action.call();
                    } else {
                        toSet = func.call();
                    }
                } finally {
                    state.set(toSet);
                }
                return true;
            }
        } while (true);
    }
    /**
     * Executes the given function while in the MUTATING state and transitions 
     * to state returned by the function.
     * <p>
     * If the func throws, the state is reset to ACTIVE and the exception is propagated.
     * 
     * @param func the function to call, should return the state after the function call
     * @return true if the action was called, false if the subscription was unsubscribed
     */
    protected final boolean call(Func0<SubscriptionState> func) {
       return callAndSet(SubscriptionState.ACTIVE, null, func); 
    }
    /**
     * Transitions to the supplied new state and executes the given action.
     * <p> 
     * The action is responsible to
     * @param newState the state to set before the action is called
     * @param action the action to execute while in the MUTATING state
     * @return true if the action was called, false if the subscription was unsubscribed
     */
    protected final boolean setAndCall(SubscriptionState newState, Action0 action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        if (setAndCall(newState, Actions.empty0())) {
            action.call();
            return true;
        }
        return false;
    }
    /**
     * Executes the given action and returns in the ACTIVE state.
     * @param action the action to execute while in the MUTATING state
     * @return true if the action was called, false if the subscription was unsubscribed
     */
    protected final boolean call(Action0 action) {
        return callAndSet(SubscriptionState.ACTIVE, action);
    }
    /**
     * Returns true if this subscription has been unsubscribed.
     * @return true if this subscription has been unsubscribed 
     */
    public final boolean isUnsubscribed() {
        return state.get() == SubscriptionState.UNSUBSCRIBED;
    }
}
