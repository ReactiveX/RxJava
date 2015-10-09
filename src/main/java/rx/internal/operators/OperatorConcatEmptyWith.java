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

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.internal.producers.ProducerArbiter;
import rx.subscriptions.SerialSubscription;

/**
 * Returns an Observable that emits an error if any item is emitted by the source and emits items from the supplied
 * alternate {@code Observable} after the source completes.
 *
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class OperatorConcatEmptyWith<T, R> implements Operator<R, T> {

    private final Observable<? extends R> alternate;

    public OperatorConcatEmptyWith(Observable<? extends R> alternate) {
        this.alternate = alternate;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> child) {
        final SerialSubscription ssub = new SerialSubscription();
        final ParentSubscriber parent = new ParentSubscriber(child, ssub, alternate);
        ssub.set(parent);
        child.add(ssub);
        child.setProducer(parent.emptyProducer);
        return parent;
    }

    private final class ParentSubscriber extends Subscriber<T> {

        private final Subscriber<? super R> child;
        private final SerialSubscription ssub;
        private final EmptyProducer emptyProducer;
        private final Observable<? extends R> alternate;

        ParentSubscriber(Subscriber<? super R> child, final SerialSubscription ssub, Observable<? extends R> alternate) {
            this.child = child;
            this.ssub = ssub;
            this.emptyProducer = new EmptyProducer();
            this.alternate = alternate;
        }

        @Override
        public void setProducer(final Producer producer) {
            /*
             * Always request Max from the parent as we never really expect the parent to emit an item, so the
             * actual value does not matter. However, if the parent producer is waiting for a request to emit
             * a terminal event, not requesting the same will cause a deadlock of the parent never completing and
             * the child never subscribed.
             */
            producer.request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            if (!child.isUnsubscribed()) {
                AlternateSubscriber as = new AlternateSubscriber(child, emptyProducer);
                ssub.set(as);
                alternate.unsafeSubscribe(as);
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            onError(new IllegalStateException("Concat empty with source emitted an item: " + t));
        }
    }

    private final class AlternateSubscriber extends Subscriber<R> {

        private final EmptyProducer emptyProducer;
        private final Subscriber<? super R> child;

        AlternateSubscriber(Subscriber<? super R> child, EmptyProducer emptyProducer) {
            this.child = child;
            this.emptyProducer = emptyProducer;
        }

        @Override
        public void setProducer(final Producer producer) {
            emptyProducer.setAltProducer(producer);
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(R r) {
            child.onNext(r);
        }
    }

    /**
     * This is a producer implementation that does the following:
     *
     * <ul>
     * <li>If the alternate producer has not yet arrived, store the total requested count from downstream.</li>
     * <li>If the alternate producer has arrived, then relay the request demand to it.</li>
     * <li>Request {@link Long#MAX_VALUE} from the parent producer, the first time the child requests anything.</li>
     * </ul>
     *
     * Since, this is only applicable to this operator, it does not check for emissions from the source, as the source
     * is never expected to emit any item. Thus it is "lighter" weight than {@link ProducerArbiter}
     */
    private static final class EmptyProducer implements Producer {

        /*Total requested items till the time the alternate producer arrives.*/
        private long missedRequested; /*Guarded by this*/
        /*Producer from the alternate Observable for this operator*/
        private Producer altProducer; /*Guarded by this*/

        @Override
        public void request(final long requested) {
            if (requested < 0) {
                throw new IllegalArgumentException("Requested items can not be negative.");
            }

            if (requested == 0) {
                return;
            }

            boolean requestToAlternate = false;
            Producer _altProducer;
            synchronized (this) {
                if (null == altProducer) {
                    /*Accumulate requested till the time an alternate producer arrives.*/
                    long r = this.missedRequested;
                    long u = r + requested;
                    if (u < 0) {
                        u = Long.MAX_VALUE;
                    }
                    this.missedRequested = u;
                } else {
                    /*If the alternate producer exists, then relay a valid request. The missed requested will be
                    requested from the alt producer on setProducer()*/
                    requestToAlternate = true;
                }

                _altProducer = altProducer;
            }

            if (requestToAlternate) {
                _altProducer.request(requested);
            }
        }

        private void setAltProducer(final Producer altProducer) {
            if (null == altProducer) {
                throw new IllegalArgumentException("Producer can not be null.");
            }

            boolean requestToAlternate = false;
            long _missedRequested;

            synchronized (this) {
                if (0 != missedRequested) {
                    /*Something was requested from the source Observable, relay that to the new producer*/
                    requestToAlternate = true;
                }

                this.altProducer = altProducer;
                _missedRequested = missedRequested;
            }

            if (requestToAlternate) {
                altProducer.request(_missedRequested);
            }
        }
    }
}
