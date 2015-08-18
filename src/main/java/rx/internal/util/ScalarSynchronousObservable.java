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
package rx.internal.util;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.internal.schedulers.EventLoopsScheduler;

public abstract class ScalarSynchronousObservable<T> extends Observable<T> {

    protected ScalarSynchronousObservable(OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
    }

    public abstract T get();

    /**
     * Customized observeOn/subscribeOn implementation which emits the scalar
     * value directly or with less overhead on the specified scheduler.
     * @param scheduler the target scheduler
     * @return the new observable
     */
    public Observable<T> scalarScheduleOn(Scheduler scheduler) {
        if (scheduler instanceof EventLoopsScheduler) {
            EventLoopsScheduler es = (EventLoopsScheduler) scheduler;
            return create(new DirectScheduledEmission<T>(es, get()));
        }
        return create(new NormalScheduledEmission<T>(scheduler, get()));
    }
    
    /** Optimized observeOn for scalar value observed on the EventLoopsScheduler. */
    static final class DirectScheduledEmission<T> implements OnSubscribe<T> {
        private final EventLoopsScheduler es;
        private final T value;
        DirectScheduledEmission(EventLoopsScheduler es, T value) {
            this.es = es;
            this.value = value;
        }
        @Override
        public void call(final Subscriber<? super T> child) {
            child.add(es.scheduleDirect(new ScalarSynchronousAction<T>(child, value)));
        }
    }
    /** Emits a scalar value on a general scheduler. */
    static final class NormalScheduledEmission<T> implements OnSubscribe<T> {
        private final Scheduler scheduler;
        private final T value;

        NormalScheduledEmission(Scheduler scheduler, T value) {
            this.scheduler = scheduler;
            this.value = value;
        }
        
        @Override
        public void call(final Subscriber<? super T> subscriber) {
            Worker worker = scheduler.createWorker();
            subscriber.add(worker);
            worker.schedule(new ScalarSynchronousAction<T>(subscriber, value));
        }
    }
    /** Action that emits a single value when called. */
    static final class ScalarSynchronousAction<T> implements Action0 {
        private final Subscriber<? super T> subscriber;
        private final T value;

        private ScalarSynchronousAction(Subscriber<? super T> subscriber,
                T value) {
            this.subscriber = subscriber;
            this.value = value;
        }

        @Override
        public void call() {
            try {
                subscriber.onNext(value);
            } catch (Throwable t) {
                subscriber.onError(t);
                return;
            }
            subscriber.onCompleted();
        }
    }
    
    public <R> Observable<R> scalarFlatMap(final Func1<? super T, ? extends Observable<? extends R>> func) {
        return create(new OnSubscribe<R>() {
            @Override
            public void call(final Subscriber<? super R> child) {
                Observable<? extends R> o = func.call(get());
                if (o instanceof ScalarSynchronousObservable) {
                    child.onNext(((ScalarSynchronousObservable<? extends R>)o).get());
                    child.onCompleted();
                } else {
                    o.unsafeSubscribe(new Subscriber<R>(child) {
                        @Override
                        public void onNext(R v) {
                            child.onNext(v);
                        }
                        @Override
                        public void onError(Throwable e) {
                            child.onError(e);
                        }
                        @Override
                        public void onCompleted() {
                            child.onCompleted();
                        }
                    });
                }
            }
        });
    }
}
