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

import static rx.Observable.create;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.producers.ProducerArbiter;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.SerialSubscription;

public final class OnSubscribeRedo<T> implements OnSubscribe<T> {

    static final Func1<Observable<? extends Notification<?>>, Observable<?>> REDO_INFINITE = new Func1<Observable<? extends Notification<?>>, Observable<?>>() {
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            return ts.map(new Func1<Notification<?>, Notification<?>>() {
                @Override
                public Notification<?> call(Notification<?> terminal) {
                    return Notification.createOnNext(null);
                }
            });
        }
    };

    public static final class RedoFinite implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        final long count;

        public RedoFinite(long count) {
            this.count = count;
        }

        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            return ts.map(new Func1<Notification<?>, Notification<?>>() {

                int num=0;
                
                @Override
                public Notification<?> call(Notification<?> terminalNotification) {
                    if(count == 0) {
                        return terminalNotification;
                    }
                    
                    num++;
                    if(num <= count) {
                        return Notification.createOnNext(num);
                    } else {
                        return terminalNotification;
                    }
                }
                
            }).dematerialize();
        }
    }

    public static final class RetryWithPredicate implements Func1<Observable<? extends Notification<?>>, Observable<? extends Notification<?>>> {
        final Func2<Integer, Throwable, Boolean> predicate;

        public RetryWithPredicate(Func2<Integer, Throwable, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Observable<? extends Notification<?>> call(Observable<? extends Notification<?>> ts) {
            return ts.scan(Notification.createOnNext(0), new Func2<Notification<Integer>, Notification<?>, Notification<Integer>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Notification<Integer> call(Notification<Integer> n, Notification<?> term) {
                    final int value = n.getValue();
                    if (predicate.call(value, term.getThrowable()))
                        return Notification.createOnNext(value + 1);
                    else
                        return (Notification<Integer>) term;
                }
            });
        }
    }

    public static <T> Observable<T> retry(Observable<T> source) {
       return retry(source, REDO_INFINITE);
    }

    public static <T> Observable<T> retry(Observable<T> source, final long count) {
        if (count < 0)
            throw new IllegalArgumentException("count >= 0 expected");
        if (count == 0)
            return source;
        return retry(source, new RedoFinite(count));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, Schedulers.trampoline()));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, scheduler));
    }

    public static <T> Observable<T> repeat(Observable<T> source) {
        return repeat(source, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, Scheduler scheduler) {
        return repeat(source, REDO_INFINITE, scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count) {
        return repeat(source, count, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count, Scheduler scheduler) {
        if(count == 0) {
            return Observable.empty();
        }
        if (count < 0)
            throw new IllegalArgumentException("count >= 0 expected");
        return repeat(source, new RedoFinite(count - 1), scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, Schedulers.trampoline()));
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, scheduler));
    }

    public static <T> Observable<T> redo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, false, scheduler));
    }

    final Observable<T> source;
    private final Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> controlHandlerFunction;
    final boolean stopOnComplete;
    final boolean stopOnError;
    private final Scheduler scheduler;

    private OnSubscribeRedo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> f, boolean stopOnComplete, boolean stopOnError,
            Scheduler scheduler) {
        this.source = source;
        this.controlHandlerFunction = f;
        this.stopOnComplete = stopOnComplete;
        this.stopOnError = stopOnError;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        
        // when true is a marker to say we are ready to resubscribe to source
        final AtomicBoolean resumeBoundary = new AtomicBoolean(true);
        
        // incremented when requests are made, decremented when requests are fulfilled
        final AtomicLong consumerCapacity = new AtomicLong();

        final Scheduler.Worker worker = scheduler.createWorker();
        child.add(worker);

        final SerialSubscription sourceSubscriptions = new SerialSubscription();
        child.add(sourceSubscriptions);

        // use a subject to receive terminals (onCompleted and onError signals) from 
        // the source observable. We use a BehaviorSubject because subscribeToSource 
        // may emit a terminal before the restarts observable (transformed terminals) 
        // is subscribed
        final BehaviorSubject<Notification<?>> terminals = BehaviorSubject.create();
        final Subscriber<Notification<?>> dummySubscriber = Subscribers.empty();
        // subscribe immediately so the last emission will be replayed to the next 
        // subscriber (which is the one we care about)
        terminals.subscribe(dummySubscriber);

        final ProducerArbiter arbiter = new ProducerArbiter();
        
        final Action0 subscribeToSource = new Action0() {
            @Override
            public void call() {
                if (child.isUnsubscribed()) {
                    return;
                }

                Subscriber<T> terminalDelegatingSubscriber = new Subscriber<T>() {
                    boolean done;

                    @Override
                    public void onCompleted() {
                        if (!done) {
                            done = true;
                            unsubscribe();
                            terminals.onNext(Notification.createOnCompleted());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!done) {
                            done = true;
                            unsubscribe();
                            terminals.onNext(Notification.createOnError(e));
                        }
                    }

                    @Override
                    public void onNext(T v) {
                        if (!done) {
                            child.onNext(v);
                            decrementConsumerCapacity();
                            arbiter.produced(1);
                        }
                    }

                    private void decrementConsumerCapacity() {
                        // use a CAS loop because we don't want to decrement the
                        // value if it is Long.MAX_VALUE
                        while (true) {
                            long cc = consumerCapacity.get();
                            if (cc != Long.MAX_VALUE) {
                                if (consumerCapacity.compareAndSet(cc, cc - 1)) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        arbiter.setProducer(producer);
                    }
                };
                // new subscription each time so if it unsubscribes itself it does not prevent retries
                // by unsubscribing the child subscription
                sourceSubscriptions.set(terminalDelegatingSubscriber);
                source.unsafeSubscribe(terminalDelegatingSubscriber);
            }
        };

        // the observable received by the control handler function will receive notifications of onCompleted in the case of 'repeat' 
        // type operators or notifications of onError for 'retry' this is done by lifting in a custom operator to selectively divert 
        // the retry/repeat relevant values to the control handler
        final Observable<?> restarts = controlHandlerFunction.call(
                terminals.lift(new Operator<Notification<?>, Notification<?>>() {
                    @Override
                    public Subscriber<? super Notification<?>> call(final Subscriber<? super Notification<?>> filteredTerminals) {
                        return new Subscriber<Notification<?>>(filteredTerminals) {
                            @Override
                            public void onCompleted() {
                                filteredTerminals.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                filteredTerminals.onError(e);
                            }

                            @Override
                            public void onNext(Notification<?> t) {
                                if (t.isOnCompleted() && stopOnComplete) {
                                    filteredTerminals.onCompleted();
                                } else if (t.isOnError() && stopOnError) {
                                    filteredTerminals.onError(t.getThrowable());
                                } else {
                                    filteredTerminals.onNext(t);
                                }
                            }

                            @Override
                            public void setProducer(Producer producer) {
                                producer.request(Long.MAX_VALUE);
                            }
                        };
                    }
                }));

        // subscribe to the restarts observable to know when to schedule the next redo.
        worker.schedule(new Action0() {
            @Override
            public void call() {
                restarts.unsafeSubscribe(new Subscriber<Object>(child) {
                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Object t) {
                        if (!child.isUnsubscribed()) {
                            // perform a best endeavours check on consumerCapacity 
                            // with the intent of only resubscribing immediately 
                            // if there is outstanding capacity
                            if (consumerCapacity.get() > 0) {
                                worker.schedule(subscribeToSource);
                            } else {
                                // set this to true so that on next request
                                // subscribeToSource will be scheduled
                                resumeBoundary.compareAndSet(false, true);
                            }
                        }
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        producer.request(Long.MAX_VALUE);
                    }
                });
            }
        });

        child.setProducer(new Producer() {

            @Override
            public void request(final long n) {
                if (n > 0) {
                    BackpressureUtils.getAndAddRequest(consumerCapacity, n);
                    arbiter.request(n);
                    if (resumeBoundary.compareAndSet(true, false))
                        worker.schedule(subscribeToSource);
                }
            }
        });
        
    }
}
