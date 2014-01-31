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
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

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
     * Skips a specified number of contiguous values from the start of a Observable sequence and then returns the remaining values.
     * 
     * @param items
     * @param num
     * @return the observable sequence starting after a number of skipped values
     * 
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx">Observable.Skip(TSource) Method</a>
     */
    public static <T> OnSubscribeFunc<T> skip(final Observable<? extends T> items, final int num) {
        // wrap in a Observable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new Skip<T>(items, num).onSubscribe(observer);
            }

        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * 
     * @param <T>
     */
    private static class Skip<T> implements OnSubscribeFunc<T> {
        private final int num;
        private final Observable<? extends T> items;

        Skip(final Observable<? extends T> items, final int num) {
            this.num = num;
            this.items = items;
        }

        public Subscription onSubscribe(Observer<? super T> observer) {
            return items.subscribe(new ItemObserver(observer));
        }

        /**
         * Used to subscribe to the 'items' Observable sequence and forward to the actualObserver up to 'num' count.
         */
        private class ItemObserver implements Observer<T> {

            private AtomicInteger counter = new AtomicInteger();
            private final Observer<? super T> observer;

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                // skip them until we reach the 'num' value
                if (counter.incrementAndGet() > num) {
                    observer.onNext(args);
                }
            }

        }

    }

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

            SourceObserver<T> so = new SourceObserver<T>(t1, csub);
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
        private static final class SourceObserver<T> implements Observer<T> implements Action0 {
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
            public void call() {
                gate.set(true);
            }

        }
    }
}
