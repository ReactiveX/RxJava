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
package rx.operators;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Groups the items emitted by an Observable according to a specified criterion, and emits these
 * grouped items as Observables, one Observable per group.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/groupBy.png">
 * @param <K> the key type
 * @param <T> the source and group value type
 */
public final class OperatorGroupBy<K, T> implements Operator<GroupedObservable<K, T>, T> {
    
    final Func1<? super T, ? extends K> keySelector;
    
    public OperatorGroupBy(final Func1<? super T, ? extends K> keySelector) {
        this.keySelector = keySelector;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super GroupedObservable<K, T>> childObserver) {
        return new GroupBySubscriber<K, T>(keySelector, childObserver);
    }
    static final class GroupBySubscriber<K, T> extends Subscriber<T> {
        final Func1<? super T, ? extends K> keySelector;
        final Subscriber<? super GroupedObservable<K, T>> childObserver;
        public GroupBySubscriber(Func1<? super T, ? extends K> keySelector, Subscriber<? super GroupedObservable<K, T>> childObserver) {
            // a new CompositeSubscription to decouple the subscription as the inner subscriptions need a separate lifecycle
            // and will unsubscribe on this parent if they are all unsubscribed
            super(new CompositeSubscription());
            this.keySelector = keySelector;
            this.childObserver = childObserver;
        }
        private final Map<K, BufferUntilSubscriber<T>> groups = new HashMap<K, BufferUntilSubscriber<T>>();
        volatile int completionCounter;
        volatile int completionEmitted;
        volatile int terminated;
        
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<GroupBySubscriber> COUNTER_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "completionCounter");
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<GroupBySubscriber> EMITTED_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "completionEmitted");
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<GroupBySubscriber> TERMINATED_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(GroupBySubscriber.class, "terminated");
        
        @Override
        public void onCompleted() {
            if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
                // if we receive onCompleted from our parent we onComplete children
                for (BufferUntilSubscriber<T> ps : groups.values()) {
                    ps.onCompleted();
                }
                
                // special case for empty (no groups emitted)
                if (completionCounter == 0) {
                    // we must track 'completionEmitted' seperately from 'completed' since `completeInner` can result in childObserver.onCompleted() being emitted
                    if (EMITTED_UPDATER.compareAndSet(this, 0, 1)) {
                        childObserver.onCompleted();
                    }
                }
            }
        }
        
        @Override
        public void onError(Throwable e) {
            if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
                // we immediately tear everything down if we receive an error
                childObserver.onError(e);
            }
        }
        
        @Override
        public void onNext(T t) {
            try {
                final K key = keySelector.call(t);
                BufferUntilSubscriber<T> gps = groups.get(key);
                if (gps == null) {
                    // this group doesn't exist
                    if (childObserver.isUnsubscribed()) {
                        // we have been unsubscribed on the outer so won't send any  more groups
                        return;
                    }
                    gps = BufferUntilSubscriber.create();
                    final BufferUntilSubscriber<T> _gps = gps;
                    
                    GroupedObservable<K, T> go = new GroupedObservable<K, T>(key, new OnSubscribe<T>() {
                        
                        @Override
                        public void call(final Subscriber<? super T> o) {
                            // number of children we have running
                            COUNTER_UPDATER.incrementAndGet(GroupBySubscriber.this);
                            o.add(Subscriptions.create(new Action0() {
                                
                                @Override
                                public void call() {
                                    completeInner();
                                }
                                
                            }));
                            _gps.unsafeSubscribe(new Subscriber<T>(o) {
                                
                                @Override
                                public void onCompleted() {
                                    o.onCompleted();
                                    completeInner();
                                }
                                
                                @Override
                                public void onError(Throwable e) {
                                    o.onError(e);
                                }
                                
                                @Override
                                public void onNext(T t) {
                                    o.onNext(t);
                                }
                                
                            });
                        }
                        
                    });
                    groups.put(key, gps);
                    childObserver.onNext(go);
                }
                // we have the correct group so send value to it
                gps.onNext(t);
            } catch (Throwable e) {
                onError(OnErrorThrowable.addValueAsLastCause(e, t));
            }
        }
        
        private void completeInner() {
            // count can be < 0 because unsubscribe also calls this
            if (COUNTER_UPDATER.decrementAndGet(this) <= 0 && (terminated == 1 || childObserver.isUnsubscribed())) {
                // completionEmitted ensures we only emit onCompleted once
                if (EMITTED_UPDATER.compareAndSet(this, 0, 1)) {
                    if (childObserver.isUnsubscribed()) {
                        // if the entire groupBy has been unsubscribed and children are completed we will propagate the unsubscribe up.
                        unsubscribe();
                    }
                    childObserver.onCompleted();
                }
            }
        }
        
    }
}
