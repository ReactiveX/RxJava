/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.concurrent.TimeUnit;

import rx.IObservable;
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.SerialSubscription;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public final class OperationDelay {

    public static <T> Observable<T> delay(IObservable<T> observable, final long delay, final TimeUnit unit, final Scheduler scheduler) {
        // observable.map(x => Observable.timer(t).map(_ => x).startItAlreadyNow()).concat()
        Observable<Observable<T>> seqs = Observable.from(observable).map(new Func1<T, Observable<T>>() {
            @Override
            public Observable<T> call(final T x) {
                ConnectableObservable<T> co = Observable.timer(delay, unit, scheduler).map(new Func1<Long, T>() {
                    @Override
                    public T call(Long ignored) {
                        return x;
                    }
                }).replay();
                co.connect();
                return co;
            }
        });
        return Observable.concat(seqs);
    }
    
    /**
     * Delays the subscription to the source by the given amount, running on the given scheduler.
     */
    public static <T> OnSubscribeFunc<T> delaySubscription(IObservable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        return new DelaySubscribeFunc<T>(source, time, unit, scheduler);
    }
    
    /** Subscribe function which schedules the actual subscription to source on a scheduler at a later time. */
    private static final class DelaySubscribeFunc<T> implements OnSubscribeFunc<T> {
        final IObservable<? extends T> source;
        final Scheduler scheduler;
        final long time;
        final TimeUnit unit;

        public DelaySubscribeFunc(IObservable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
            this.time = time;
            this.unit = unit;
        }
        @Override
        public Subscription onSubscribe(final Observer<? super T> t1) {
            final SerialSubscription ssub = new SerialSubscription();
            
            ssub.setSubscription(scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    if (!ssub.isUnsubscribed()) {
                        ssub.setSubscription(source.subscribe(t1));
                    }
                }
            }, time, unit));
            
            return ssub;
        }
    }
}
