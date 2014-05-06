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

import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

/**
 * Transforms an Observable that emits Observables into a single Observable that
 * emits the items emitted by the most recently published of those Observables.
 * <p>
 * <img width="640" src=
 * "https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
 * 
 * @param <T> the value type
 */
public final class OperatorSwitch<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription ssub = new SerialSubscription();
        child.add(ssub);
        
        return new Subscriber<Observable<? extends T>>(child) {
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
            @Override
            public void onNext(Observable<? extends T> t) {
                final int id;
                synchronized (guard) {
                    id = ++index;
                    active = true;
                }
                
                Subscriber<T> sub = new Subscriber<T>() {

                    @Override
                    public void onNext(T t) {
                        emit(t, id);
                    }

                    @Override
                    public void onError(Throwable e) {
                        error(e, id);
                    }

                    @Override
                    public void onCompleted() {
                        complete(id);
                    }
                    
                };
                ssub.set(sub);
                
                t.unsafeSubscribe(sub);
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
            void emit(T value, int id) {
                List<Object> localQueue;
                synchronized (guard) {
                    if (id != index) {
                        return;
                    }
                    if (emitting) {
                        if (queue == null) {
                            queue = new ArrayList<Object>();
                        }
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
        };
    }
    
}
