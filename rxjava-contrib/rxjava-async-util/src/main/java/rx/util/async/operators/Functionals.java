/**
 * Copyright 2014 Netflix, Inc.
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

package rx.util.async.operators;

import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Utility methods convert between functional interfaces of actions and functions.
 */
public final class Functionals {
    private Functionals() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Return an action which takes a Throwable and does nothing.
     * <p>(To avoid casting from the generic empty1().)
     * @return the action
     */
    public static Action1<Throwable> emptyThrowable() {
        return EMPTY_THROWABLE;
    }
    /**
     * An action that takes a Throwable and does nothing.
     */
    private static final Action1<Throwable> EMPTY_THROWABLE = new EmptyThrowable();
    /** An empty throwable class. */
    private static final class EmptyThrowable implements Action1<Throwable> {
        @Override
        public void call(Throwable t1) {
        }
    }
    /**
     * Return an Action0 instance which does nothing.
     * @return an Action0 instance which does nothing
     */
    public static Action0 empty() {
        return EMPTY;
    }
    /** A single empty instance. */
    private static final Action0 EMPTY = new EmptyAction();
    /** An empty action class. */
    private static final class EmptyAction implements Action0 {
        @Override
        public void call() {
        }
    }
    
    /**
     * Converts a runnable instance into an Action0 instance.
     * @param run the Runnable to run when the Action0 is called
     * @return the Action0 wrapping the Runnable
     */
    public static Action0 fromRunnable(Runnable run, Worker inner) {
        if (run == null) {
            throw new NullPointerException("run");
        }
        return new ActionWrappingRunnable(run, inner);
    }
    /** An Action1 which wraps and calls a Runnable. */
    private static final class ActionWrappingRunnable implements Action0 {
        final Runnable run;
        final Worker inner;

        public ActionWrappingRunnable(Runnable run, Worker inner) {
            this.run = run;
            this.inner = inner;
        }

        @Override
        public void call() {
            try {
                run.run();
            } finally {
                inner.unsubscribe();
            }
        }
        
    }
    /**
     * Converts an Action0 instance into a Runnable instance.
     * @param action the Action0 to call when the Runnable is run
     * @return the Runnable wrapping the Action0
     */
    public static Runnable toRunnable(Action0 action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        return new RunnableAction(action);
    }
    /** An Action0 which wraps and calls a Runnable. */
    private static final class RunnableAction implements Runnable {
        final Action0 action;

        public RunnableAction(Action0 action) {
            this.action = action;
        }

        @Override
        public void run() {
            action.call();
        }
        
    }
}
