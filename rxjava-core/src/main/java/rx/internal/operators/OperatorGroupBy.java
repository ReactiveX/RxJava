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
package rx.internal.operators;

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
import rx.subscriptions.Subscriptions;

/**
 * Groups the items emitted by an Observable according to a specified criterion, and emits these
 * grouped items as Observables, one Observable per group.
 * <p>
 * <img width="640" height="360" src="https://raw.githubusercontent.com/wiki/Netflix/RxJava/images/rx-operators/groupBy.png" alt="">
 *
 * @param <K> the key type
 * @param <T> the source and group value type
 */
public final class OperatorGroupBy<K, T> implements Operator<GroupedObservable<K, T>, T> {
    
    final Func1<? super T, ? extends K> keySelector;
    
    public OperatorGroupBy(final Func1<? super T, ? extends K> keySelector) {
        this.keySelector = keySelector;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super GroupedObservable<K, T>> child) {
        return new GroupBySubscriber<K, T>(keySelector, child);
    }
    static final class GroupBySubscriber<K, T> extends Subscriber<T> {
        final Func1<? super T, ? extends K> keySelector;
        final Subscriber<? super GroupedObservable<K, T>> child;
        public GroupBySubscriber(Func1<? super T, ? extends K> keySelector, Subscriber<? super GroupedObservable<K, T>> child) {
            // a new CompositeSubscription to decouple the subscription as the inner subscriptions need a separate lifecycle
            // and will unsubscribe on this parent if they are all unsubscribed
            super();
            this.keySelector = keySelector;
            this.child = child;
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
        public void onStart() {
            /*
             * This operator does not support backpressure as splitting a stream effectively turns it into a "hot observable" and
             * blocking any one group would block the entire parent stream. If backpressure is needed on individual groups then
             * operators such as `onBackpressureDrop` or `onBackpressureBuffer` should be used.
             */
            request(Long.MAX_VALUE);
        }
        
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
                        child.onCompleted();
                    }
                }
            }
        }
        
        @Override
        public void onError(Throwable e) {
            if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
                // we immediately tear everything down if we receive an error
                child.onError(e);
            }
        }
        
        @Override
        public void onNext(T t) {
            try {
                final K key = keySelector.call(t);
                BufferUntilSubscriber<T> group = groups.get(key);
                if (group == null) {
                    // this group doesn't exist
                    if (child.isUnsubscribed()) {
                        // we have been unsubscribed on the outer so won't send any  more groups
                        return;
                    }
                    group = BufferUntilSubscriber.create();
                    final BufferUntilSubscriber<T> _group = group;
                    
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
                            _group.unsafeSubscribe(new Subscriber<T>(o) {
                                
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
                    groups.put(key, group);
                    child.onNext(go);
                }
                // we have the correct group so send value to it
                group.onNext(t);
            } catch (Throwable e) {
                onError(OnErrorThrowable.addValueAsLastCause(e, t));
            }
        }
        
        private void completeInner() {
            // count can be < 0 because unsubscribe also calls this
            if (COUNTER_UPDATER.decrementAndGet(this) <= 0 && (terminated == 1 || child.isUnsubscribed())) {
                // completionEmitted ensures we only emit onCompleted once
                if (EMITTED_UPDATER.compareAndSet(this, 0, 1)) {
                    if (child.isUnsubscribed()) {
                        // if the entire groupBy has been unsubscribed and children are completed we will propagate the unsubscribe up.
                        unsubscribe();
                    }
                    child.onCompleted();
                }
            }
        }
        
    }
}
