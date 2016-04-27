/**
 * Copyright 2016 Netflix, Inc.
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
package rx.internal.operators;

import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Nulls out references to upstream data structures when the source terminates or 
 * the child unsubscribes.
 * @param <T> the value type
 */
public final class OnSubscribeDetach<T> implements OnSubscribe<T> {
    
    final Observable<T> source;
    
    public OnSubscribeDetach(Observable<T> source) {
        this.source = source;
    }
    
    @Override
    public void call(Subscriber<? super T> t) {
        DetachSubscriber<T> parent = new DetachSubscriber<T>(t);
        DetachProducer<T> producer = new DetachProducer<T>(parent);
        
        t.add(producer);
        t.setProducer(producer);
        
        source.unsafeSubscribe(parent);
    }
    
    /**
     * The parent subscriber that forwards events and cleans up on a terminal state.
     * @param <T> the value type
     */
    static final class DetachSubscriber<T> extends Subscriber<T> {
        
        final AtomicReference<Subscriber<? super T>> actual;
        
        final AtomicReference<Producer> producer;
        
        final AtomicLong requested;

        public DetachSubscriber(Subscriber<? super T> actual) {
            this.actual = new AtomicReference<Subscriber<? super T>>(actual);
            this.producer = new AtomicReference<Producer>();
            this.requested = new AtomicLong();
        }
        
        @Override
        public void onNext(T t) {
            Subscriber<? super T> a = actual.get();
            
            if (a != null) {
                a.onNext(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            producer.lazySet(TerminatedProducer.INSTANCE);
            Subscriber<? super T> a = actual.getAndSet(null);
            
            if (a != null) {
                a.onError(e);
            } else {
                RxJavaPluginUtils.handleException(e);
            }
        }

        
        @Override
        public void onCompleted() {
            producer.lazySet(TerminatedProducer.INSTANCE);
            Subscriber<? super T> a = actual.getAndSet(null);
            
            if (a != null) {
                a.onCompleted();
            }
        }
        
        void innerRequest(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
            Producer p = producer.get();
            if (p != null) {
                p.request(n);
            } else {
                BackpressureUtils.getAndAddRequest(requested, n);
                p = producer.get();
                if (p != null && p != TerminatedProducer.INSTANCE) {
                    long r = requested.getAndSet(0L);
                    p.request(r);
                }
            }
        }
        
        @Override
        public void setProducer(Producer p) {
            if (producer.compareAndSet(null, p)) {
                long r = requested.getAndSet(0L);
                p.request(r);
            } else {
                if (producer.get() != TerminatedProducer.INSTANCE) {
                    throw new IllegalStateException("Producer already set!");
                }
            }
        }
        
        void innerUnsubscribe() {
            producer.lazySet(TerminatedProducer.INSTANCE);
            actual.lazySet(null);
            // full barrier in unsubscribe()
            unsubscribe();
        }
    }
    
    /**
     * Callbacks from the child Subscriber.
     * @param <T> the value type
     */
    static final class DetachProducer<T> implements Producer, Subscription {
        final DetachSubscriber<T> parent;

        public DetachProducer(DetachSubscriber<T> parent) {
            this.parent = parent;
        }
        
        @Override
        public void request(long n) {
            parent.innerRequest(n);
        }
        
        @Override
        public boolean isUnsubscribed() {
            return parent.isUnsubscribed();
        }
        
        @Override
        public void unsubscribe() {
            parent.innerUnsubscribe();
        }
    }
    
    /**
     * Singleton instance via enum.
     */
    enum TerminatedProducer implements Producer {
        INSTANCE;
        
        @Override
        public void request(long n) {
            // ignored
        }
    }
}
