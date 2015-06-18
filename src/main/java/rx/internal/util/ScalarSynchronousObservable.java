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

import rx.*;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.internal.schedulers.EventLoopsScheduler;

public final class ScalarSynchronousObservable<T> extends Observable<T> {
    public static final <T> Observable<T> create(final T t) {
        // create the onSubscribe func here so changes can be detected later
        OnSubscribe<T> onSubscribe = new ScalarSynchronousOnSubscribe<T>(t);

        ScalarSynchronousObservable<T> o = new ScalarSynchronousObservable<T>(t, onSubscribe);

        // check to see if the execution hooks have wrapped the onSubscribe func
        if (o.onSubscribe instanceof ScalarSynchronousOnSubscribe) {
            // if not then return as is
            return o;
        } else {
            // there is a hook wrapping the original onSubscribe func return a normal observable that won't be optimized out.
            return Observable.create(o.onSubscribe);
        }
    }

    private final T t;
    public static final class ScalarSynchronousOnSubscribe<T> implements OnSubscribe<T> {
        private final T t;

        private ScalarSynchronousOnSubscribe(T t) {
            this.t = t;
        }

            @Override
            public void call(Subscriber<? super T> s) {
                /*
                 *  We don't check isUnsubscribed as it is a significant performance impact in the fast-path use cases.
                 *  See PerfBaseline tests and https://github.com/ReactiveX/RxJava/issues/1383 for more information.
                 *  The assumption here is that when asking for a single item we should emit it and not concern ourselves with 
                 *  being unsubscribed already. If the Subscriber unsubscribes at 0, they shouldn't have subscribed, or it will 
                 *  filter it out (such as take(0)). This prevents us from paying the price on every subscription. 
                 */
                s.onNext(t);
                // checking unsubscribe here because if they really wanted performance then they should have called get().
                if (!s.isUnsubscribed())
                    s.onCompleted();
            }
    }

    protected ScalarSynchronousObservable(T t, OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
        this.t = t;
    }

    public T get() {
        return t;
    }
    /**
     * Customized observeOn/subscribeOn implementation which emits the scalar
     * value directly or with less overhead on the specified scheduler.
     * @param scheduler the target scheduler
     * @return the new observable
     */
    public Observable<T> scalarScheduleOn(Scheduler scheduler) {
        if (scheduler instanceof EventLoopsScheduler) {
            EventLoopsScheduler es = (EventLoopsScheduler) scheduler;
            return create(new DirectScheduledEmission<T>(es, t));
        }
        return create(new NormalScheduledEmission<T>(scheduler, t));
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
}
