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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.internal.producers.ProducerArbiter;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits the items emitted by two or more Observables, one after the other.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/concat.png" alt="">
 *
 * @param <T>
 *            the source and result value type
 */
public final class OperatorConcat<T> implements Operator<T, Observable<? extends T>> {
    /** Lazy initialization via inner-class holder. */
    private static final class Holder {
        /** A singleton instance. */
        static final OperatorConcat<Object> INSTANCE = new OperatorConcat<Object>();
    }
    /**
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorConcat<T> instance() {
        return (OperatorConcat<T>)Holder.INSTANCE;
    }
    private OperatorConcat() { }
    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription current = new SerialSubscription();
        child.add(current);
        ConcatSubscriber<T> cs = new ConcatSubscriber<T>(s, current);
        ConcatProducer<T> cp = new ConcatProducer<T>(cs);
        child.setProducer(cp);
        return cs;
    }

    static final class ConcatProducer<T> implements Producer {
        final ConcatSubscriber<T> cs;

        ConcatProducer(ConcatSubscriber<T> cs) {
            this.cs = cs;
        }

        @Override
        public void request(long n) {
            cs.requestFromChild(n);
        }

    }

    static final class ConcatSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final NotificationLite<Observable<? extends T>> nl = NotificationLite.instance();
        private final Subscriber<T> child;
        private final SerialSubscription current;
        final ConcurrentLinkedQueue<Object> queue;

        volatile ConcatInnerSubscriber<T> currentSubscriber;

        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<ConcatSubscriber> WIP = AtomicIntegerFieldUpdater.newUpdater(ConcatSubscriber.class, "wip");

        // accessed by REQUESTED
        private volatile long requested;
        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<ConcatSubscriber> REQUESTED = AtomicLongFieldUpdater.newUpdater(ConcatSubscriber.class, "requested");
        private final ProducerArbiter arbiter;

        public ConcatSubscriber(Subscriber<T> s, SerialSubscription current) {
            super(s);
            this.child = s;
            this.current = current;
            this.arbiter = new ProducerArbiter();
            this.queue = new ConcurrentLinkedQueue<Object>();
            add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    queue.clear();
                }
            }));
        }

        @Override
        public void onStart() {
            // no need for more than 1 at a time since we concat 1 at a time, so we'll request 2 to start ...
            // 1 to be subscribed to, 1 in the queue, then we'll keep requesting 1 at a time after that
            request(2);
        }

        private void requestFromChild(long n) {
            if (n <=0) return;
            // we track 'requested' so we know whether we should subscribe the next or not
            long previous = BackpressureUtils.getAndAddRequest(REQUESTED, this, n);
            arbiter.request(n);
            if (previous == 0) {
                if (currentSubscriber == null && wip > 0) {
                    // this means we may be moving from one subscriber to another after having stopped processing
                    // so need to kick off the subscribe via this request notification
                    subscribeNext();
                }
            } 
        }

        private void decrementRequested() {
            REQUESTED.decrementAndGet(this);
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            queue.add(nl.next(t));
            if (WIP.getAndIncrement(this) == 0) {
                subscribeNext();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            queue.add(nl.completed());
            if (WIP.getAndIncrement(this) == 0) {
                subscribeNext();
            }
        }
        

        void completeInner() {
            currentSubscriber = null;
            if (WIP.decrementAndGet(this) > 0) {
                subscribeNext();
            }
            request(1);
        }

        void subscribeNext() {
            if (requested > 0) {
                Object o = queue.poll();
                if (nl.isCompleted(o)) {
                    child.onCompleted();
                } else if (o != null) {
                    Observable<? extends T> obs = nl.getValue(o);
                    currentSubscriber = new ConcatInnerSubscriber<T>(this, child, arbiter);
                    current.set(currentSubscriber);
                    obs.unsafeSubscribe(currentSubscriber);
                }
            } else {
                // requested == 0, so we'll peek to see if we are completed, otherwise wait until another request
                Object o = queue.peek();
                if (nl.isCompleted(o)) {
                    child.onCompleted();
                }
            }
        }
    }

    static class ConcatInnerSubscriber<T> extends Subscriber<T> {

        private final Subscriber<T> child;
        private final ConcatSubscriber<T> parent;
        @SuppressWarnings("unused")
        private volatile int once = 0;
        @SuppressWarnings("rawtypes")
        private final static AtomicIntegerFieldUpdater<ConcatInnerSubscriber> ONCE = AtomicIntegerFieldUpdater.newUpdater(ConcatInnerSubscriber.class, "once");
        private final ProducerArbiter arbiter;

        public ConcatInnerSubscriber(ConcatSubscriber<T> parent, Subscriber<T> child, ProducerArbiter arbiter) {
            this.parent = parent;
            this.child = child;
            this.arbiter = arbiter;
        }
        
        @Override
        public void onNext(T t) {
            child.onNext(t);
            parent.decrementRequested();
            arbiter.produced(1);
        }

        @Override
        public void onError(Throwable e) {
            if (ONCE.compareAndSet(this, 0, 1)) {
                // terminal error through parent so everything gets cleaned up, including this inner
                parent.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (ONCE.compareAndSet(this, 0, 1)) {
                // terminal completion to parent so it continues to the next
                parent.completeInner();
            }
        }
        
        @Override
        public void setProducer(Producer producer) {
            arbiter.setProducer(producer);
        }

    };
}
