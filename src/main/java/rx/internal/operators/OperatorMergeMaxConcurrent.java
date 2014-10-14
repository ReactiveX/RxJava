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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/merge.png" alt="">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 *
 * @param <T> the emitted value type
 */
public final class OperatorMergeMaxConcurrent<T> implements Operator<T, Observable<? extends T>> {
    final int maxConcurrency;
    
    public OperatorMergeMaxConcurrent(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
    
    @Override
    public Subscriber<? super Observable<? extends T>> call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        
        return new SourceSubscriber<T>(maxConcurrency, s, csub);
    }
    static final class SourceSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final int maxConcurrency;
        final Subscriber<T> s;
        final CompositeSubscription csub;
        final Object guard;

        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<SourceSubscriber> WIP_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(SourceSubscriber.class, "wip");
        
        /** Guarded by guard. */
        int active;
        /** Guarded by guard. */
        final Queue<Observable<? extends T>> queue;
        
        public SourceSubscriber(int maxConcurrency, Subscriber<T> s, CompositeSubscription csub) {
            super(s);
            this.maxConcurrency = maxConcurrency;
            this.s = s;
            this.csub = csub;
            this.guard = new Object();
            this.queue = new LinkedList<Observable<? extends T>>();
            this.wip = 1;
        }
        
        @Override
        public void onNext(Observable<? extends T> t) {
            synchronized (guard) {
                queue.add(t);
            }
            subscribeNext();
        }
        
        void subscribeNext() {
            Observable<? extends T> t;
            synchronized (guard) {
                t = queue.peek();
                if (t == null || active >= maxConcurrency) {
                    return;
                }
                active++;
                queue.poll();
            }
            
            Subscriber<T> itemSub = new Subscriber<T>() {
                boolean once = true;
                @Override
                public void onNext(T t) {
                    s.onNext(t);
                }
                
                @Override
                public void onError(Throwable e) {
                    SourceSubscriber.this.onError(e);
                }
                
                @Override
                public void onCompleted() {
                    if (once) {
                        once = false;
                        synchronized (guard) {
                            active--;
                        }
                        csub.remove(this);
                        
                        subscribeNext();
                        
                        SourceSubscriber.this.onCompleted();
                    }
                }
                
            };
            csub.add(itemSub);
            WIP_UPDATER.incrementAndGet(this);
            
            t.unsafeSubscribe(itemSub);
        }
        
        @Override
        public void onError(Throwable e) {
            s.onError(e);
            unsubscribe();
        }
        
        @Override
        public void onCompleted() {
            if (WIP_UPDATER.decrementAndGet(this) == 0) {
                s.onCompleted();
            }
        }
    }
}
