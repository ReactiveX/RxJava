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


import rx.*;
import rx.internal.producers.ProducerArbiter;
import rx.subscriptions.SerialSubscription;

/**
 * If the Observable completes without emitting any items, subscribe to an alternate Observable. Allows for similar
 * functionality to {@link rx.internal.operators.OperatorDefaultIfEmpty} except instead of one item being emitted when
 * empty, the results of the given Observable will be emitted.
 * @param <T> the value type
 */
public final class OperatorSwitchIfEmpty<T> implements Observable.Operator<T, T> {
    private final Observable<? extends T> alternate;

    public OperatorSwitchIfEmpty(Observable<? extends T> alternate) {
        this.alternate = alternate;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final SerialSubscription ssub = new SerialSubscription();
        ProducerArbiter arbiter = new ProducerArbiter();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child, ssub, arbiter, alternate);
        ssub.set(parent);
        child.add(ssub);
        child.setProducer(arbiter);
        return parent;
    }

    private static final class ParentSubscriber<T> extends Subscriber<T> {

        private boolean empty = true;
        private final Subscriber<? super T> child;
        private final SerialSubscription ssub;
        private final ProducerArbiter arbiter;
        private final Observable<? extends T> alternate;

        ParentSubscriber(Subscriber<? super T> child, final SerialSubscription ssub, ProducerArbiter arbiter, Observable<? extends T> alternate) {
            this.child = child;
            this.ssub = ssub;
            this.arbiter = arbiter;
            this.alternate = alternate;
        }

        @Override
        public void setProducer(final Producer producer) {
            arbiter.setProducer(producer);
        }

        @Override
        public void onCompleted() {
            if (!empty) {
                child.onCompleted();
            } else if (!child.isUnsubscribed()) {
                subscribeToAlternate();
            }
        }

        private void subscribeToAlternate() {
            AlternateSubscriber<T> as = new AlternateSubscriber<T>(child, arbiter);
            ssub.set(as);
            alternate.unsafeSubscribe(as);
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            empty = false;
            child.onNext(t);
            arbiter.produced(1);
        }
    }
    
    private static final class AlternateSubscriber<T> extends Subscriber<T> {
        
        private final ProducerArbiter arbiter;
        private final Subscriber<? super T> child;

        AlternateSubscriber(Subscriber<? super T> child, ProducerArbiter arbiter) {
            this.child = child;
            this.arbiter = arbiter;
        }
        
        @Override
        public void setProducer(final Producer producer) {
            arbiter.setProducer(producer);
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
        public void onNext(T t) {
            child.onNext(t);
            arbiter.produced(1);
        }        
    }
}
