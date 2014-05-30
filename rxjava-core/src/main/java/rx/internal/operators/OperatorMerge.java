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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 * 
 * @param <T> the source and merged value type
 */
public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> outerOperation) {

        final Subscriber<T> o = new SerializedSubscriber<T>(outerOperation);
        final CompositeSubscription childrenSubscriptions = new CompositeSubscription();
        outerOperation.add(childrenSubscriptions);
        
        return new MergeSubscriber<T>(o, childrenSubscriptions);

    }
    static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<T> actual;
        final CompositeSubscription childrenSubscriptions;
        volatile int wip;
        volatile boolean completed;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MergeSubscriber> WIP_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(MergeSubscriber.class, "wip");
        
        public MergeSubscriber(Subscriber<T> actual, CompositeSubscription childrenSubscriptions) {
            super(actual);
            this.actual = actual;
            this.childrenSubscriptions = childrenSubscriptions;
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            WIP_UPDATER.incrementAndGet(this);
            Subscriber<T> i = new InnerSubscriber<T>(this);
            childrenSubscriptions.add(i);
            t.unsafeSubscribe(i);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            completed = true;
            if (wip == 0) {
                actual.onCompleted();
            }
        }
        void completeInner(InnerSubscriber<T> s) {
            try {
                if (WIP_UPDATER.decrementAndGet(this) == 0 && completed) {
                    actual.onCompleted();
                }
            } finally {
                childrenSubscriptions.remove(s);
            }
        }
    }
    static final class InnerSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super T> actual;
        final MergeSubscriber<T> parent;
        /** Make sure the inner termination events are delivered only once. */
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<InnerSubscriber> ONCE_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(InnerSubscriber.class, "once");
        
        public InnerSubscriber(MergeSubscriber<T> parent) {
            this.parent = parent;
            this.actual = parent.actual;
        }
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.completeInner(this);
            }
        }
        
    }
}
