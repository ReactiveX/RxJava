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

import java.util.concurrent.CountDownLatch;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TestScheduler;
import rx.schedulers.TrampolineScheduler;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Asynchronously subscribes and unsubscribes Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
 */
public class OperatorSubscribeOn<T> implements Operator<T, Observable<T>> {
    // debug
    private static final boolean ASYNC_UNSUBSCRIBE = false;
    // debug
    private static final boolean ALLOW_CANCEL = true;
    
    private final Scheduler scheduler;

    public OperatorSubscribeOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> child) {
        return new Subscriber<Observable<T>>(child) {

            @Override
            public void onCompleted() {
                // we ignore the outer Observable an onCompleted will be passed when the inner completes
            }

            @Override
            public void onError(Throwable e) {
                // we should never receive this but if we do we pass it on
                child.onError(new IllegalStateException("Error received on nested Observable.", e));
            }

            @Override
            public void onNext(final Observable<T> o) {
                if (scheduler instanceof ImmediateScheduler) {
                    // avoid overhead, execute directly
                    o.subscribe(child);
                    return;
                } else if (scheduler instanceof TrampolineScheduler) {
                    // avoid overhead, execute directly
                    o.subscribe(child);
                    return;
                } else if (scheduler instanceof TestScheduler) {
                    // this one will deadlock as it is single-threaded and won't run the scheduled
                    // work until it manually advances, which it won't be able to do as it will block
                    
                    if (ALLOW_CANCEL) {
                        final CompositeSubscription cs = new CompositeSubscription();
                        child.add(cs);
                        final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
                        cs.add(mas);
                        mas.set(scheduler.schedule(new Action1<Inner>() {
                            @Override
                            public void call(Inner t1) {
                                cs.delete(mas);
                                o.subscribe(child);
                            }
                        }));
                    } else {
                        scheduler.schedule(new Action1<Inner>() {
                            @Override
                            public void call(Inner t1) {
                                o.subscribe(child);
                            }
                        });
                    }
                    return;
                }

                final CountDownLatch onSubscribeLatch = new CountDownLatch(1);
                
                if (ALLOW_CANCEL) {
                    final CompositeSubscription cs = new CompositeSubscription();
                    child.add(cs);
                    // unblock call in case an asynchronous unsubscribe happens
                    final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
                    cs.add(BooleanSubscription.create(new Action0() {
                        @Override
                        public void call() {
                            onSubscribeLatch.countDown();
                        }
                    }));
                    cs.add(mas);
                    mas.set(scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(final Inner inner) {
                            cs.delete(mas);
                            // we inject 'child' so it's the same subscription
                            // so it works on synchronous Observables

                            final Subscriber<T> s2 = new Subscriber<T>() {

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
                                }

                                @Override
                                public void onSubscribe() {
                                    onSubscribeLatch.countDown();
                                }
                            };

                            if (ASYNC_UNSUBSCRIBE) {
                                child.add(BooleanSubscription.create(new Action0() {
                                    @Override
                                    public void call() {
                                        inner.schedule(new Action1<Inner>() {
                                            @Override
                                            public void call(Inner t1) {
                                                s2.unsubscribe();
                                            }
                                        });
                                    }

                                }));
                            } else {
                                child.add(s2);
                            }

                            o.subscribe(s2);
                            onSubscribeLatch.countDown();
                        }
                    }));
                } else {
                    scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(final Inner inner) {
                            // we inject 'child' so it's the same subscription
                            // so it works on synchronous Observables
                            o.subscribe(new Subscriber<T>(child) {

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
                                }

                                @Override
                                public void onSubscribe() {
                                    onSubscribeLatch.countDown();
                                }
                            });
                            onSubscribeLatch.countDown();
                        }
                    });
                }
                try {
                    onSubscribeLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        };
    }
}
