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

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func2;
import rx.internal.producers.ProducerArbiter;
import rx.schedulers.Schedulers;
import rx.subscriptions.SerialSubscription;

public final class OperatorRetryWithPredicate<T> implements Observable.Operator<T, Observable<T>> {
    final Func2<Integer, Throwable, Boolean> predicate;
    public OperatorRetryWithPredicate(Func2<Integer, Throwable, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> child) {
        final Scheduler.Worker inner = Schedulers.trampoline().createWorker();
        child.add(inner);
        
        final SerialSubscription serialSubscription = new SerialSubscription();
        // add serialSubscription so it gets unsubscribed if child is unsubscribed
        child.add(serialSubscription);
        ProducerArbiter pa = new ProducerArbiter();
        child.setProducer(pa);
        return new SourceSubscriber<T>(child, predicate, inner, serialSubscription, pa);
    }
    
    static final class SourceSubscriber<T> extends Subscriber<Observable<T>> {
        final Subscriber<? super T> child;
        final Func2<Integer, Throwable, Boolean> predicate;
        final Scheduler.Worker inner;
        final SerialSubscription serialSubscription;
        final ProducerArbiter pa;
        
        final AtomicInteger attempts = new AtomicInteger();

        public SourceSubscriber(Subscriber<? super T> child, 
                final Func2<Integer, Throwable, Boolean> predicate, 
                Scheduler.Worker inner, 
                SerialSubscription serialSubscription,
                ProducerArbiter pa) {
            this.child = child;
            this.predicate = predicate;
            this.inner = inner;
            this.serialSubscription = serialSubscription;
            this.pa = pa;
        }
        
        
        @Override
        public void onCompleted() {
            // ignore as we expect a single nested Observable<T>
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(final Observable<T> o) {
            inner.schedule(new Action0() {

                @Override
                public void call() {
                    final Action0 _self = this;
                    attempts.incrementAndGet();

                    // new subscription each time so if it unsubscribes itself it does not prevent retries
                    // by unsubscribing the child subscription
                    Subscriber<T> subscriber = new Subscriber<T>() {
                        boolean done;
                        @Override
                        public void onCompleted() {
                            if (!done) {
                                done = true;
                                child.onCompleted();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (!done) {
                                done = true;
                                if (predicate.call(attempts.get(), e) && !inner.isUnsubscribed()) {
                                    // retry again
                                    inner.schedule(_self);
                                } else {
                                    // give up and pass the failure
                                    child.onError(e);
                                }
                            }
                        }

                        @Override
                        public void onNext(T v) {
                            if (!done) {
                                child.onNext(v);
                                pa.produced(1);
                            }
                        }

                        @Override
                        public void setProducer(Producer p) {
                            pa.setProducer(p);
                        }
                    };
                    // register this Subscription (and unsubscribe previous if exists) 
                    serialSubscription.set(subscriber);
                    o.unsafeSubscribe(subscriber);
                }
            });
        }
    }
}
