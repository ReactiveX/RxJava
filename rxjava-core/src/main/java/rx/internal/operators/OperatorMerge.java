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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action1;
import rx.internal.util.RxSpscRingBuffer;
import rx.internal.util.SubscriptionSet;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 * 
 * @param <T>
 *            the source and merged value type
 */
public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SubscriptionSet<InnerSubscriber<T>> childrenSubscriptions = new SubscriptionSet<InnerSubscriber<T>>();
        child.add(childrenSubscriptions);

        System.out.println("subscribed");
        return new MergeSubscriber<T>(child, childrenSubscriptions);

    }

    static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<? super T> actual;
        final SubscriptionSet<InnerSubscriber<T>> childrenSubscribers;
        volatile MergeProducer<T> mergeProducer;
        volatile int wip;
        volatile boolean completed;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MergeSubscriber> WIP_UPDATER = AtomicIntegerFieldUpdater.newUpdater(MergeSubscriber.class, "wip");

        public MergeSubscriber(Subscriber<? super T> actual, SubscriptionSet<InnerSubscriber<T>> childrenSubscriptions) {
            super(actual);
            this.actual = actual;
            this.childrenSubscribers = childrenSubscriptions;
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            if (mergeProducer == null) {
                // this means it's an Observable without backpressure support
                mergeProducer = new MergeProducer<T>(null, actual, childrenSubscribers);
            }
            WIP_UPDATER.incrementAndGet(this);
            InnerSubscriber<T> i = new InnerSubscriber<T>(mergeProducer, this);
            childrenSubscribers.add(i);
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

        @Override
        protected Producer onSetProducer(final Producer p) {
            /*
             * Decouple the Producer chain.
             * 
             * Upwards we will just request -1 to get all Observables.
             * For requests coming from the child we want to route them instead
             * to the MergeProducer which will manage draining queues to deliver events.
             */
            mergeProducer = new MergeProducer<T>(p, actual, childrenSubscribers);
            return mergeProducer;
        }

        void completeInner(InnerSubscriber<T> s) {
            try {
                if (WIP_UPDATER.decrementAndGet(this) == 0 && completed) {
                    actual.onCompleted();
                }
            } finally {
                childrenSubscribers.remove(s);
            }
        }
    }

    private static final class MergeProducer<T> implements Producer {

        // TODO migrate to AFU
        private final AtomicInteger requested = new AtomicInteger(-1);
        private final AtomicInteger wip = new AtomicInteger();
        private final Subscriber<? super T> child;
        private final SubscriptionSet<InnerSubscriber<T>> childrenSubscribers;
        private final Producer parentProducer;

        public MergeProducer(Producer parentProducer, Subscriber<? super T> child, SubscriptionSet<InnerSubscriber<T>> childrenSubscribers) {
            this.parentProducer = parentProducer;
            this.child = child;
            this.childrenSubscribers = childrenSubscribers;
        }

        private final Action1<InnerSubscriber<T>> action = new Action1<InnerSubscriber<T>>() {

            @SuppressWarnings("unchecked")
            @Override
            public void call(InnerSubscriber<T> childSubscriber) {
                if (child.isUnsubscribed() || requested.get() == 0) {
                    return;
                }
                RxSpscRingBuffer q = childSubscriber.queue;
                Object o = null;
                while ((o = q.poll()) != null) {
                    if (q.isCompleted(o)) {
                        childSubscriber.complete();
                    } else {
                        child.onNext((T) o);
                    }

                    if (requested.decrementAndGet() == 0) {
                        // we have sent as many as have been requested
                        return;
                    }
                }
                childSubscriber.queue.requestIfNeeded(childSubscriber);
            }

        };

        @Override
        public void request(int n) {
            // we have been requested from our child
            requested.getAndAdd(n);
            // parentProducer can be null if we're merging an Observable<Observable> without backpressure support
            if (parentProducer != null) {
                // request up to our parent to start sending us the Observables for merging
                parentProducer.request(-1);
            }
            attemptSending();
        }

        /**
         * Allow only one thread to be sending at a time.
         * It will drain as many Observers as it can up to the requested limit.
         */
        private void attemptSending() {
            if (requested.get() == 0) {
                // we're done emitting the number requested so return
                return;
            }

            // TODO right now this starts from the beginning every time
            // it needs to be more intelligent and remember where in the list it last ended
            // so it can be more fair ... otherwise those at the end may never get their events emitted
            if (wip.getAndIncrement() == 0) {
                while (true) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    childrenSubscribers.forEach(action);
                    if (wip.decrementAndGet() == 0) {
                        //                            // no further events to try sending data for
                        return;
                    }
                    if (requested.get() == 0) {
                        // we're done emitting the number requested so return
                        return;
                    }
                }
            }
        }

    }

    private static final class InnerSubscriber<T> extends Subscriber<T> {
        final MergeProducer<T> mergeProducer;
        final Subscriber<? super T> actual;
        final MergeSubscriber<T> parent;
        /** Make sure the inner termination events are delivered only once. */
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<InnerSubscriber> ONCE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(InnerSubscriber.class, "once");

        private final RxSpscRingBuffer queue = new RxSpscRingBuffer();

        public InnerSubscriber(MergeProducer<T> mergeProducer, MergeSubscriber<T> parent) {
            this.mergeProducer = mergeProducer;
            this.parent = parent;
            this.actual = parent.actual;
            // setup request to fill queue
            queue.requestIfNeeded(this);
        }

        @Override
        public void onNext(T t) {
            try {
                queue.onNext(t);
            } catch (MissingBackpressureException e) {
                onError(e);
                unsubscribe();
            }
            mergeProducer.attemptSending();
        }

        @Override
        public void onError(Throwable e) {
            // it doesn't go through queues, it immediately onErrors and tears everything down
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            queue.onCompleted();
            mergeProducer.attemptSending();
        }

        void complete() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.completeInner(this);
            }
        }

    }
}
