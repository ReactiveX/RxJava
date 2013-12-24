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

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.util.Timestamped;

/**
 * Returns an Observable that emits the last <code>count</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
 */
public final class OperationTakeLast {

    public static <T> OnSubscribeFunc<T> takeLast(final Observable<? extends T> items, final int count) {
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new TakeLast<T>(items, count).onSubscribe(observer);
            }

        };
    }

    private static class TakeLast<T> implements OnSubscribeFunc<T> {
        private final int count;
        private final Observable<? extends T> items;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        TakeLast(final Observable<? extends T> items, final int count) {
            this.count = count;
            this.items = items;
        }

        public Subscription onSubscribe(Observer<? super T> observer) {
            if (count < 0) {
                throw new IndexOutOfBoundsException(
                        "count could not be negative");
            }
            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {

            /**
             * Store the last count elements until now.
             */
            private Deque<T> deque = new LinkedList<T>();
            private final Observer<? super T> observer;
            private final ReentrantLock lock = new ReentrantLock();

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                try {
                    for (T value : deque) {
                        observer.onNext(value);
                    }
                    observer.onCompleted();
                } catch (Throwable e) {
                    observer.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque and
                    // remove it at once. We can ignore the value directly.
                    return;
                }
                lock.lock();
                try {
                    deque.offerLast(value);
                    if (deque.size() > count) {
                        // Now deque has count + 1 elements, so the first
                        // element in the deque definitely does not belong
                        // to the last count elements of the source
                        // sequence. We can drop it now.
                        deque.removeFirst();
                    }
                } catch (Throwable e) {
                    observer.onError(e);
                    subscription.unsubscribe();
                } finally {
                    lock.unlock();
                }
            }

        }

    }
    
    /**
     * Returns the items emitted by source whose arrived in the time window
     * before the source completed.
     */
    public static <T> OnSubscribeFunc<T> takeLast(Observable<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        return new TakeLastTimed<T>(source, -1, time, unit, scheduler);
    }
    
    /**
     * Returns the items emitted by source whose arrived in the time window
     * before the source completed and at most count values.
     */
    public static <T> OnSubscribeFunc<T> takeLast(Observable<? extends T> source, int count, long time, TimeUnit unit, Scheduler scheduler) {
        return new TakeLastTimed<T>(source, count, time, unit, scheduler);
    }
    
    /** Take only the values which appeared some time before the completion. */
    static final class TakeLastTimed<T> implements OnSubscribeFunc<T> {
        final Observable<? extends T> source;
        final long ageMillis;
        final Scheduler scheduler;
        final int count;

        public TakeLastTimed(Observable<? extends T> source, int count, long time, TimeUnit unit, Scheduler scheduler) {
            this.source = source;
            this.ageMillis = unit.toMillis(time);
            this.scheduler = scheduler;
            this.count = count;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            SafeObservableSubscription s = new SafeObservableSubscription();
            source.subscribe(new TakeLastTimedObserver<T>(t1, s, count, ageMillis, scheduler));
            return s;
        }
    }
    /** Observes source values and keeps the most recent items. */
    static final class TakeLastTimedObserver<T> implements Observer<T> {
        final Observer<? super T> observer;
        final Subscription cancel;
        final long ageMillis;
        final Scheduler scheduler;
        /** -1 indicates unlimited buffer. */
        final int count;
        
        final Deque<Timestamped<T>> buffer = new LinkedList<Timestamped<T>>();

        public TakeLastTimedObserver(Observer<? super T> observer, Subscription cancel, 
                int count, long ageMillis, Scheduler scheduler) {
            this.observer = observer;
            this.cancel = cancel;
            this.ageMillis = ageMillis;
            this.scheduler = scheduler;
            this.count = count;
        }

        protected void runEvictionPolicy(long now) {
            // trim size
            while (count >= 0 && buffer.size() > count) {
                buffer.pollFirst();
            }
            // remove old entries
            while (!buffer.isEmpty()) {
                Timestamped<T> v = buffer.peekFirst();
                if (v.getTimestampMillis() < now - ageMillis) {
                    buffer.pollFirst();
                } else {
                    break;
                }
            }
        }
        
        @Override
        public void onNext(T args) {
            long t = scheduler.now();
            buffer.add(new Timestamped<T>(t, args));
            runEvictionPolicy(t);
        }

        @Override
        public void onError(Throwable e) {
            buffer.clear();
            observer.onError(e);
            cancel.unsubscribe();
        }

        /** 
         * Emit the contents of the buffer. 
         * @return true if no exception was raised in the process
         */
        protected boolean emitBuffer() {
            for (Timestamped<T> v : buffer) {
                try {
                    observer.onNext(v.getValue());
                } catch (Throwable t) {
                    buffer.clear();
                    observer.onError(t);
                    return false;
                }
            }
            buffer.clear();
            return true;
        }
        
        @Override
        public void onCompleted() {
            runEvictionPolicy(scheduler.now());

            if (emitBuffer()) {
                observer.onCompleted();
            }
            cancel.unsubscribe();
        }
        
    }
}
