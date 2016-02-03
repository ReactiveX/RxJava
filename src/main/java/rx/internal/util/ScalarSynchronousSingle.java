/**
 * Copyright 2014 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.util;

import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.internal.schedulers.EventLoopsScheduler;

public final class ScalarSynchronousSingle<T> extends Single<T> {

    public static final <T> ScalarSynchronousSingle<T> create(T t) {
        return new ScalarSynchronousSingle<T>(t);
    }

    final T value;

    protected ScalarSynchronousSingle(final T t) {
        super(new OnSubscribe<T>() {

            @Override
            public void call(SingleSubscriber<? super T> te) {
                te.onSuccess(t);
            }

        });
        this.value = t;
    }

    public T get() {
        return value;
    }

    /**
     * Customized observeOn/subscribeOn implementation which emits the scalar
     * value directly or with less overhead on the specified scheduler.
     *
     * @param scheduler the target scheduler
     * @return the new observable
     */
    public Single<T> scalarScheduleOn(Scheduler scheduler) {
        if (scheduler instanceof EventLoopsScheduler) {
            EventLoopsScheduler es = (EventLoopsScheduler) scheduler;
            return create(new DirectScheduledEmission<T>(es, value));
        }
        return create(new NormalScheduledEmission<T>(scheduler, value));
    }

    /**
     * Optimized observeOn for scalar value observed on the EventLoopsScheduler.
     */
    static final class DirectScheduledEmission<T> implements OnSubscribe<T> {
        private final EventLoopsScheduler es;
        private final T value;

        DirectScheduledEmission(EventLoopsScheduler es, T value) {
            this.es = es;
            this.value = value;
        }

        @Override
        public void call(SingleSubscriber<? super T> singleSubscriber) {
            singleSubscriber.add(es.scheduleDirect(new ScalarSynchronousSingleAction<T>(singleSubscriber, value)));
        }
    }

    /**
     * Emits a scalar value on a general scheduler.
     */
    static final class NormalScheduledEmission<T> implements OnSubscribe<T> {
        private final Scheduler scheduler;
        private final T value;

        NormalScheduledEmission(Scheduler scheduler, T value) {
            this.scheduler = scheduler;
            this.value = value;
        }

        @Override
        public void call(SingleSubscriber<? super T> singleSubscriber) {
            Worker worker = scheduler.createWorker();
            singleSubscriber.add(worker);
            worker.schedule(new ScalarSynchronousSingleAction<T>(singleSubscriber, value));
        }
    }

    /**
     * Action that emits a single value when called.
     */
    static final class ScalarSynchronousSingleAction<T> implements Action0 {
        private final SingleSubscriber<? super T> subscriber;
        private final T value;

        ScalarSynchronousSingleAction(SingleSubscriber<? super T> subscriber,
                                      T value) {
            this.subscriber = subscriber;
            this.value = value;
        }

        @Override
        public void call() {
            try {
                subscriber.onSuccess(value);
            } catch (Throwable t) {
                subscriber.onError(t);
            }
        }
    }

    public <R> Single<R> scalarFlatMap(final Func1<? super T, ? extends Single<? extends R>> func) {
        return create(new OnSubscribe<R>() {
            @Override
            public void call(final SingleSubscriber<? super R> child) {

                Single<? extends R> o = func.call(value);
                if (o instanceof ScalarSynchronousSingle) {
                    child.onSuccess(((ScalarSynchronousSingle<? extends R>) o).value);
                } else {
                    Subscriber<R> subscriber = new Subscriber<R>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            child.onError(e);
                        }

                        @Override
                        public void onNext(R r) {
                            child.onSuccess(r);
                        }
                    };
                    child.add(subscriber);
                    o.unsafeSubscribe(subscriber);
                }
            }
        });
    }
}
