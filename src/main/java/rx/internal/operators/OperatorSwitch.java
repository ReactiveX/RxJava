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
package rx.internal.operators;

import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

/**
 * Transforms an Observable that emits Observables into a single Observable that
 * emits the items emitted by the most recently published of those Observables.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/switchDo.png" alt="">
 * 
 * @param <T> the value type
 */
public final class OperatorSwitch<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        return new SwitchSubscriber<T>(child);
    }

    private static final class SwitchSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final SerializedSubscriber<T> s;
        final SerialSubscription ssub;
        final Object guard = new Object();
        final NotificationLite<?> nl = NotificationLite.instance();
        /** Guarded by guard. */
        int index;
        /** Guarded by guard. */
        boolean active;
        /** Guarded by guard. */
        boolean mainDone;
        /** Guarded by guard. */
        List<Object> queue;
        /** Guarded by guard. */
        boolean emitting;
        /** Guarded by guard. */
        InnerSubscriber currentSubscriber;
        /** Guarded by guard. */
        long initialRequested;

        volatile boolean infinite = false;

        public SwitchSubscriber(Subscriber<? super T> child) {
            super(child);
            s = new SerializedSubscriber<T>(child);
            ssub = new SerialSubscription();
            child.add(ssub);
            child.setProducer(new Producer(){

                @Override
                public void request(long n) {
                    if (infinite) {
                        return;
                    }
                    if(n == Long.MAX_VALUE) {
                        infinite = true;
                    }
                    InnerSubscriber localSubscriber;
                    synchronized (guard) {
                        localSubscriber = currentSubscriber;
                        if (currentSubscriber == null) {
                            initialRequested = n;
                        } else {
                            // If n == Long.MAX_VALUE, infinite will become true. Then currentSubscriber.requested won't be used.
                            // Therefore we don't need to worry about overflow.
                            currentSubscriber.requested += n;
                        }
                    }
                    if (localSubscriber != null) {
                        localSubscriber.requestMore(n);
                    }
                }
            });
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            final int id;
            long remainingRequest;
            synchronized (guard) {
                id = ++index;
                active = true;
                if (infinite) {
                    remainingRequest = Long.MAX_VALUE;
                } else {
                    remainingRequest = currentSubscriber == null ? initialRequested : currentSubscriber.requested;
                }
                currentSubscriber = new InnerSubscriber(id, remainingRequest);
                currentSubscriber.requested = remainingRequest;
            }
            ssub.set(currentSubscriber);

            t.unsafeSubscribe(currentSubscriber);
        }

        @Override
        public void onError(Throwable e) {
            s.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            List<Object> localQueue;
            synchronized (guard) {
                mainDone = true;
                if (active) {
                    return;
                }
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(nl.completed());
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            drain(localQueue);
            s.onCompleted();
            unsubscribe();
        }
        void emit(T value, int id, InnerSubscriber innerSubscriber) {
            List<Object> localQueue;
            synchronized (guard) {
                if (id != index) {
                    return;
                }
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    innerSubscriber.requested--;
                    queue.add(value);
                    return;
                }
                localQueue = queue;
                queue = null;
                emitting = true;
            }
            boolean once = true;
            boolean skipFinal = false;
            try {
                do {
                    drain(localQueue);
                    if (once) {
                        once = false;
                        synchronized (guard) {
                            innerSubscriber.requested--;
                        }
                        s.onNext(value);
                    }
                    synchronized (guard) {
                        localQueue = queue;
                        queue = null;
                        if (localQueue == null) {
                            emitting = false;
                            skipFinal = true;
                            break;
                        }
                    }
                } while (!s.isUnsubscribed());
            } finally {
                if (!skipFinal) {
                    synchronized (guard) {
                        emitting = false;
                    }
                }
            }
        }
        void drain(List<Object> localQueue) {
            if (localQueue == null) {
                return;
            }
            for (Object o : localQueue) {
                if (nl.isCompleted(o)) {
                    s.onCompleted();
                    break;
                } else
                if (nl.isError(o)) {
                    s.onError(nl.getError(o));
                    break;
                } else {
                    @SuppressWarnings("unchecked")
                    T t = (T)o;
                    s.onNext(t);
                }
            }
        }

        void error(Throwable e, int id) {
            List<Object> localQueue;
            synchronized (guard) {
                if (id != index) {
                    return;
                }
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(nl.error(e));
                    return;
                }

                localQueue = queue;
                queue = null;
                emitting = true;
            }

            drain(localQueue);
            s.onError(e);
            unsubscribe();
        }
        void complete(int id) {
            List<Object> localQueue;
            synchronized (guard) {
                if (id != index) {
                    return;
                }
                active = false;
                if (!mainDone) {
                    return;
                }
                if (emitting) {
                    if (queue == null) {
                        queue = new ArrayList<Object>();
                    }
                    queue.add(nl.completed());
                    return;
                }

                localQueue = queue;
                queue = null;
                emitting = true;
            }

            drain(localQueue);
            s.onCompleted();
            unsubscribe();
        }

        final class InnerSubscriber extends Subscriber<T> {

            /**
             * The number of request that is not acknowledged.
             *
             * Guarded by guard.
             */
            private long requested = 0;

            private final int id;

            private final long initialRequested;

            public InnerSubscriber(int id, long initialRequested) {
                this.id = id;
                this.initialRequested = initialRequested;
            }

            @Override
            public void onStart() {
                requestMore(initialRequested);
            }

            public void requestMore(long n) {
                request(n);
            }

            @Override
            public void onNext(T t) {
                emit(t, id, this);
            }

            @Override
            public void onError(Throwable e) {
                error(e, id);
            }

            @Override
            public void onCompleted() {
                complete(id);
            }
        }
    }
}
