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
package rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import rx.Observer;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Implements an observer that ensures proper event delivery
 * semantics to its abstract onXxxxCore methods.
 */
public abstract class ObserverBase<T> implements Observer<T> {
    private final AtomicBoolean completed = new AtomicBoolean();

    @Override
    public void onNext(T args) {
        if (!completed.get()) {
            onNextCore(args);
        }
    }

    @Override
    public void onError(Throwable e) {
        if (completed.compareAndSet(false, true)) {
            onErrorCore(e);
        }
    }

    @Override
    public void onCompleted() {
        if (completed.compareAndSet(false, true)) {
            onCompletedCore();
        }
    }
    /**
     * Implement this method to react to the receival of a new element in the sequence.
     */
    protected abstract void onNextCore(T args);
    /**
     * Implement this method to react to the occurrence of an exception.
     */
    protected abstract void onErrorCore(Throwable e);
    /**
     * Implement this method to react to the end of the sequence.
     */
    protected abstract void onCompletedCore();
    /**
     * Try to trigger the error state.
     * @param t 
     * @return false if already completed
     */
    protected boolean fail(Throwable t) {
        if (completed.compareAndSet(false, true)) {
            onErrorCore(t);
            return true;
        }
        return false;
    }
    /**
     * Wrap an existing observer into an ObserverBase instance.
     * 
     * @param observer the observer to wrap
     * @return the wrapped observer base which forwards its onNext
     *         onError and onCompleted events to the wrapped observer's
     *         methods of the same name
     */
    public static <T> ObserverBase<T> wrap(Observer<T> observer) {
        return new WrappingObserverBase<T>(observer);
    }
    /**
     * Create an ObserverBase instance that forwards its onNext to the
     * given action and ignores the onError and onCompleted events.
     * @param onNextAction the onNext action
     * @return an ObserverBase instance that forwards its events to the given
     *         action.
     */
    public static <T> ObserverBase<T> create(Action1<T> onNextAction) {
        return create(onNextAction, ObserverBase.<Throwable>empty1(), empty0());
    }
    /**
     * Create an ObserverBase instance that forwards its onNext to the
     * given action and ignores the onError and onCompleted events.
     * @param onNextAction the onNext action
     * @param onErrorAction the onError action
     * @return an ObserverBase instance that forwards its events to the given
     *         actions.
     */
    public static <T> ObserverBase<T> create(Action1<T> onNextAction, Action1<Throwable> onErrorAction) {
        return create(onNextAction, onErrorAction, empty0());
    }
    /**
     * Create an ObserverBase instance that forwards its onNext, onError
     * and onCompleted events to the given actions.
     * @param onNextAction the onNext action
     * @param onErrorAction the onError action
     * @param onCompletedAction the onCompleted action
     * @return an ObserverBase instance that forwards its events to the given
     *         actions.
     */
    public static <T> ObserverBase<T> create(Action1<T> onNextAction, Action1<Throwable> onErrorAction, Action0 onCompletedAction) {
        return new FunctionalObserverBase<T>(onNextAction, onErrorAction, onCompletedAction);
    }
    /**
     * Creates a no-op Action1 instance.
     */
    private static <T> Action1<T> empty1() {
        return new Action1<T>() {
            @Override
            public void call(T t1) {
            }
        };
    }
    /**
     * Creates a no-op Action0 instance.
     */
    private static Action0 empty0() {
        return new Action0() {
            @Override
            public void call() {
            }
        };
    }
    /** Require the object to be non-null. */
    private static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }
    /** 
     * Default implementation which calls actions for the onNext, onError
     * and onCompleted events.
     */
    private static class FunctionalObserverBase<T> extends ObserverBase<T> {
        private final Action1<T> onNextAction;
        private final Action1<Throwable> onErrorAction;
        private final Action0 onCompletedAction;
        public FunctionalObserverBase(
            Action1<T> onNextAction,
            Action1<Throwable> onErrorAction,
            Action0 onCompletedAction
        ) {
            this.onNextAction = requireNonNull(onNextAction, "onNextAction");
            this.onErrorAction = requireNonNull(onErrorAction, "onErrorAction");
            this.onCompletedAction = requireNonNull(onCompletedAction, "onCompletedAction");
        }

        @Override
        protected void onNextCore(T args) {
            onNextAction.call(args);
        }

        @Override
        protected void onErrorCore(Throwable e) {
            onErrorAction.call(e);
        }

        @Override
        protected void onCompletedCore() {
            onCompletedAction.call();
        }
        
    }
    /**
     * Default implementation which wraps another observer.
     */
    private static class WrappingObserverBase<T> extends ObserverBase<T> {
        private final Observer<T> observer;
        public WrappingObserverBase(Observer<T> observer) {
            this.observer = requireNonNull(observer, "observer");
        }

        @Override
        protected void onNextCore(T args) {
            observer.onNext(args);
        }

        @Override
        protected void onErrorCore(Throwable e) {
            observer.onError(e);
        }

        @Override
        protected void onCompletedCore() {
            observer.onCompleted();
        }
    }
}
