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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Returns an Observable that skips the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/skip.png">
 * <p>
 * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
 * those items that come after, by modifying the Observable with the skip operation.
 */
public final class OperationSkip {

    /**
     * Skip the items after subscription for the given duration.
     * 
     * @param <T>
     *            the value type
     */
    public static final class SkipTimed<T> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;

        public SkipTimed(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {

            SafeObservableSubscription timer = new SafeObservableSubscription();
            SafeObservableSubscription data = new SafeObservableSubscription();

            CompositeSubscription csub = new CompositeSubscription(timer, data);

            final SourceObserver<T> so = new SourceObserver<T>(t1, csub);
            data.wrap(source.subscribe(so));
            if (!data.isUnsubscribed()) {
                timer.wrap(scheduler.schedule(so, time, unit));
            }

            return csub;
        }

        /**
         * Observes the source and relays its values once gate turns into true.
         * 
         * @param <T>
         *            the observed value type
         */
        private static final class SourceObserver<T> implements Observer<T>, Action1<Inner> {
            final AtomicBoolean gate;
            final Observer<? super T> observer;
            final Subscription cancel;

            public SourceObserver(Observer<? super T> observer,
                    Subscription cancel) {
                this.gate = new AtomicBoolean();
                this.observer = observer;
                this.cancel = cancel;
            }

            @Override
            public void onNext(T args) {
                if (gate.get()) {
                    observer.onNext(args);
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    observer.onError(e);
                } finally {
                    cancel.unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                try {
                    observer.onCompleted();
                } finally {
                    cancel.unsubscribe();
                }
            }

            @Override
            public void call(Inner inner) {
                gate.set(true);
            }

        }
    }
}
