/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.operators;

import java.util.concurrent.TimeUnit;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

/**
 * This operation filters out events which are published too quickly in succession. This is done by dropping events which are
 * followed up by other events before a specified timer has expired. If the timer expires and no follow up event was published (yet)
 * the last received event is published.
 *
 * @param <T> the value type
 */
public final class OperatorDebounceWithTime<T> implements Operator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    /**
     * @param timeout
     *            How long each event has to be the 'last event' before it gets published.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     *
     */
    public OperatorDebounceWithTime(long timeout, TimeUnit unit, Scheduler scheduler) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Worker worker = scheduler.createWorker();
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription ssub = new SerialSubscription();
        
        s.add(worker);
        s.add(ssub);
        
        return new Subscriber<T>(child) {
            final DebounceState<T> state = new DebounceState<T>();
            final Subscriber<?> self = this;
            @Override
            public void onNext(final T t) {
                
                final int index = state.next(t);
                ssub.set(worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        state.emit(index, s, self);
                    }
                }, timeout, unit));
            }
            
            @Override
            public void onError(Throwable e) {
                s.onError(e);
                unsubscribe();
                state.clear();
            }
            
            @Override
            public void onCompleted() {
                state.emitAndComplete(s, this);
            }
        };
    }
    /**
     * Tracks the last value to be emitted and manages completion.
     * @param <T> the value type
     */
    static final class DebounceState<T> {
        /** Guarded by this. */
        int index;
        /** Guarded by this. */
        T value;
        /** Guarded by this. */
        boolean hasValue;
        /** Guarded by this. */
        boolean terminate;
        /** Guarded by this. */
        boolean emitting;
        
        public synchronized int next(T value) {
            this.value = value;
            this.hasValue = true;
            return ++index;
        }
        public void emit(int index, Subscriber<T> onNextAndComplete, Subscriber<?> onError) {
            T localValue;
            boolean localHasValue;
            synchronized (this) {
                if (emitting || !hasValue || index != this.index) {
                    return;
                }
                localValue = value;
                localHasValue = hasValue;
                
                value = null;
                hasValue = false;
                emitting = true;
            }

            if  (localHasValue) {
                try {
                    onNextAndComplete.onNext(localValue);
                } catch (Throwable e) {
                    onError.onError(e);
                    return;
                }
            }

            // Check if a termination was requested in the meantime.
            synchronized (this) {
                if (!terminate) {
                    emitting = false;
                    return;
                }
            }
            
            onNextAndComplete.onCompleted();
        }
        public void emitAndComplete(Subscriber<T> onNextAndComplete, Subscriber<?> onError) {
            T localValue;
            boolean localHasValue;
            
            synchronized (this) {
                if (emitting) {
                    terminate = true;
                    return;
                }
                localValue = value;
                localHasValue = hasValue;
                
                value = null;
                hasValue = false;

                emitting = true;
            }

            if  (localHasValue) {
                try {
                    onNextAndComplete.onNext(localValue);
                } catch (Throwable e) {
                    onError.onError(e);
                    return;
                }
            }
            onNextAndComplete.onCompleted();
        }
        public synchronized void clear() {
            ++index;
            value = null;
            hasValue = false;
        }
    }
}