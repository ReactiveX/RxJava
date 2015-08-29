/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class PublisherAmb<T> implements Publisher<T> {
    final Publisher<? extends T>[] sources;
    final Iterable<? extends Publisher<? extends T>> sourcesIterable;
    
    public PublisherAmb(Publisher<? extends T>[] sources, Iterable<? extends Publisher<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Subscriber<? super T> s) {
        Publisher<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Publisher[8];
            for (Publisher<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    Publisher<? extends T>[] b = new Publisher[count + count >> 2];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        } else
        if (count == 1) {
            sources[0].subscribe(s);
            return;
        }

        AmbCoordinator<T> ac = new AmbCoordinator<>(s, count);
        ac.subscribe(sources);
    }
    
    static final class AmbCoordinator<T> implements Subscription {
        final Subscriber<? super T> actual;
        final AmbInnerSubscriber<T>[] subscribers;
        
        volatile int winner;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<AmbCoordinator> WINNER =
                AtomicIntegerFieldUpdater.newUpdater(AmbCoordinator.class, "winner");
        
        @SuppressWarnings("unchecked")
        public AmbCoordinator(Subscriber<? super T> actual, int count) {
            this.actual = actual;
            this.subscribers = new AmbInnerSubscriber[count];
        }
        
        public void subscribe(Publisher<? extends T>[] sources) {
            AmbInnerSubscriber<T>[] as = subscribers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new AmbInnerSubscriber<>(this, i + 1, actual);
            }
            WINNER.lazySet(this, 0); // release the contents of 'as'
            actual.onSubscribe(this);
            
            for (int i = 0; i < len; i++) {
                if (winner != 0) {
                    return;
                }
                
                sources[i].subscribe(as[i]);
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            int w = winner;
            if (w > 0) {
                subscribers[w - 1].request(n);
            } else
            if (w == 0) {
                for (AmbInnerSubscriber<T> a : subscribers) {
                    a.cancel();
                }
            }
        }
        
        public boolean win(int index) {
            int w = winner;
            if (w == 0) {
                if (WINNER.compareAndSet(this, 0, index)) {
                    return true;
                }
                return false;
            }
            return w == index;
        }
        
        @Override
        public void cancel() {
            if (winner != -1) {
                WINNER.lazySet(this, -1);
                
                for (AmbInnerSubscriber<T> a : subscribers) {
                    a.cancel();
                }
            }
        }
    }
    
    static final class AmbInnerSubscriber<T> extends AtomicReference<Subscription> implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -1185974347409665484L;
        final AmbCoordinator<T> parent;
        final int index;
        final Subscriber<? super T> actual;
        
        boolean won;
        
        volatile long missedRequested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<AmbInnerSubscriber> MISSED_REQUESTED =
                AtomicLongFieldUpdater.newUpdater(AmbInnerSubscriber.class, "missedRequested");
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        public AmbInnerSubscriber(AmbCoordinator<T> parent, int index, Subscriber<? super T> actual) {
            this.parent = parent;
            this.index = index;
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (!compareAndSet(null, s)) {
                s.cancel();
                if (get() != CANCELLED) {
                    SubscriptionHelper.reportSubscriptionSet();
                }
                return;
            }
            
            long r = MISSED_REQUESTED.getAndSet(this, 0L);
            if (r != 0L) {
                s.request(r);
            }
        }
        
        @Override
        public void request(long n) {
            Subscription s = get();
            if (s != null) {
                s.request(n);
            } else {
                BackpressureHelper.add(MISSED_REQUESTED, this, n);
                s = get();
                if (s != null && s != CANCELLED) {
                    long r = MISSED_REQUESTED.getAndSet(this, 0L);
                    if (r != 0L) {
                        s.request(r);
                    }
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            if (won) {
                actual.onNext(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onNext(t);
                } else {
                    get().cancel();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (won) {
                actual.onError(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onError(t);
                } else {
                    get().cancel();
                    RxJavaPlugins.onError(t);
                }
            }
        }
        
        @Override
        public void onComplete() {
            if (won) {
                actual.onComplete();
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onComplete();
                } else {
                    get().cancel();
                }
            }
        }
        
        @Override
        public void cancel() {
            Subscription s = get();
            if (s != CANCELLED) {
                s = getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.cancel();
                }
            }
        }
        
    }
}
