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

import java.util.*;

import rx.*;
import rx.Observable;
import rx.Observable.Operator;
import rx.exceptions.CompositeException;
import rx.internal.producers.ProducerArbiter;
import rx.plugins.RxJavaPlugins;
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
    /** Lazy initialization via inner-class holder. */
    private static final class Holder {
        /** A singleton instance. */
        static final OperatorSwitch<Object> INSTANCE = new OperatorSwitch<Object>(false);
    }
    /** Lazy initialization via inner-class holder. */
    private static final class HolderDelayError {
        /** A singleton instance. */
        static final OperatorSwitch<Object> INSTANCE = new OperatorSwitch<Object>(true);
    }
    /**
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> OperatorSwitch<T> instance(boolean delayError) {
        if (delayError) {
            return (OperatorSwitch<T>)HolderDelayError.INSTANCE;
        }
        return (OperatorSwitch<T>)Holder.INSTANCE;
    }

    final boolean delayError;
    
    OperatorSwitch(boolean delayError) { 
        this.delayError = delayError;
    }

    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        SwitchSubscriber<T> sws = new SwitchSubscriber<T>(child, delayError);
        child.add(sws);
        sws.init();
        return sws;
    }

    private static final class SwitchSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<? super T> child;
        final SerialSubscription ssub;
        final ProducerArbiter arbiter;
        
        final boolean delayError;
        
        long index;
        
        Throwable error;
        
        boolean mainDone;
        
        List<T> queue;
        
        boolean innerActive;
        
        boolean emitting;
        
        boolean missed;

        SwitchSubscriber(Subscriber<? super T> child, boolean delayError) {
            this.child = child;
            this.arbiter = new ProducerArbiter();
            this.ssub = new SerialSubscription();
            this.delayError = delayError;
        }
        
        void init() {
            child.add(ssub);
            child.setProducer(new Producer(){

                @Override
                public void request(long n) {
                    if (n > 0) {
                        arbiter.request(n);
                    }
                }
            });
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            InnerSubscriber<T> inner;
            synchronized (this) {
                long id = ++index;
                inner = new InnerSubscriber<T>(id, this);
                innerActive = true;
            }
            ssub.set(inner);
            
            t.unsafeSubscribe(inner);
        }

        @Override
        public void onError(Throwable e) {
            synchronized (this) {
                e = updateError(e);
                mainDone = true;
                
                if (emitting) {
                    missed = true;
                    return;
                }
                if (delayError && innerActive) {
                    return;
                }
                emitting = true;
            }
            
            child.onError(e);
        }

        @Override
        public void onCompleted() {
            Throwable ex;
            synchronized (this) {
                mainDone = true;
                if (emitting) {
                    missed = true;
                    return;
                }
                if (innerActive) {
                    return;
                }
                emitting = true;
                ex = error;
            }
            if (ex == null) {
                child.onCompleted();
            } else {
                child.onError(ex);
            }
        }
        
        Throwable updateError(Throwable e) {
            Throwable ex = error;
            if (ex == null) {
                error = e;
            } else
            if (ex instanceof CompositeException) {
                CompositeException ce = (CompositeException) ex;
                List<Throwable> list = new ArrayList<Throwable>(ce.getExceptions());
                list.add(e);
                e = new CompositeException(list);
                error = e;
            } else {
                e = new CompositeException(Arrays.asList(ex, e));
                error = e;
            }
            return e;
        }
        
        void emit(T value, long id) {
            synchronized (this) {
                if (id != index) {
                    return;
                }
                
                if (emitting) {
                    List<T> q = queue;
                    if (q == null) {
                        q = new ArrayList<T>(4);
                        queue = q;
                    }
                    q.add(value);
                    missed = true;
                    return;
                }
                
                emitting = true;
            }
            
            child.onNext(value);
            
            arbiter.produced(1);
            
            for (;;) {
                if (child.isUnsubscribed()) {
                    return;
                }
                
                Throwable localError;
                boolean localMainDone;
                boolean localActive;
                List<T> localQueue;
                synchronized (this) {
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    
                    localError = error;
                    localMainDone = mainDone;
                    localQueue = queue;
                    localActive = innerActive;
                }
                
                if (!delayError && localError != null) {
                    child.onError(localError);
                    return;
                }
                
                if (localQueue == null && !localActive && localMainDone) {
                    if (localError != null) {
                        child.onError(localError);
                    } else {
                        child.onCompleted();
                    }
                    return;
                }
                
                if (localQueue != null) {
                    int n = 0;
                    for (T v : localQueue) {
                        if (child.isUnsubscribed()) {
                            return;
                        }

                        child.onNext(v);
                        n++;
                    }
                    
                    arbiter.produced(n);
                }
            }
        }

        void error(Throwable e, long id) {
            boolean drop;
            synchronized (this) {
                if (id == index) {
                    innerActive = false;
                    
                    e = updateError(e);
                    
                    if (emitting) {
                        missed = true;
                        return;
                    }
                    if (delayError && !mainDone) {
                        return;
                    }
                    emitting = true;
                    
                    drop = false;
                } else {
                    drop = true;
                }
            }
            
            if (drop) {
                pluginError(e);
            } else {
                child.onError(e);
            }
        }
        
        void complete(long id) {
            Throwable ex;
            synchronized (this) {
                if (id != index) {
                    return;
                }
                innerActive = false;
                
                if (emitting) {
                    missed = true;
                    return;
                }
                
                ex = error;

                if (!mainDone) {
                    return;
                }
            }
            
            if (ex != null) {
                child.onError(ex);
            } else {
                child.onCompleted();
            }
        }

        void pluginError(Throwable e) {
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
        }
    }
    
    private static final class InnerSubscriber<T> extends Subscriber<T> {

        private final long id;

        private final SwitchSubscriber<T> parent;

        InnerSubscriber(long id, SwitchSubscriber<T> parent) {
            this.id = id;
            this.parent = parent;
        }
        
        @Override
        public void setProducer(Producer p) {
            parent.arbiter.setProducer(p);
        }

        @Override
        public void onNext(T t) {
            parent.emit(t, id);
        }

        @Override
        public void onError(Throwable e) {
            parent.error(e, id);
        }

        @Override
        public void onCompleted() {
            parent.complete(id);
        }
    }

}
