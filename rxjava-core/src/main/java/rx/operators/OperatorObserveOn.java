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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TestScheduler;
import rx.schedulers.TrampolineScheduler;
import rx.util.functions.Action1;

/**
 * Asynchronously notify Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperatorObserveOn<T> implements Operator<T, T> {

    private final Scheduler scheduler;

    public OperatorObserveOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        if (scheduler instanceof ImmediateScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TrampolineScheduler) {
            // avoid overhead, execute directly
            return child;
        } else if (scheduler instanceof TestScheduler) {
            // this one will deadlock as it is single-threaded and won't run the scheduled
            // work until it manually advances, which it won't be able to do as it will block
            return child;
        } else {
            return new ObserveOnSubscriber(child);
        }

    }

    private static Object NULL_SENTINEL = new Object();
    private static Object COMPLETE_SENTINEL = new Object();

    private static class ErrorSentinel {
        final Throwable e;

        ErrorSentinel(Throwable e) {
            this.e = e;
        }
    }

    /** Observe through individual queue per observer. */
    private class ObserveOnSubscriber extends Subscriber<T> {
        final Subscriber<? super T> observer;
        private volatile Scheduler.Inner recursiveScheduler;

        private final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);
        final AtomicLong counter = new AtomicLong(0);

        public ObserveOnSubscriber(Subscriber<? super T> observer) {
            super(observer);
            this.observer = observer;
        }

        @Override
        public void onNext(final T t) {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                if (t == null) {
                    queue.put(NULL_SENTINEL);
                } else {
                    queue.put(t);
                }
                schedule();
            } catch (InterruptedException e) {
                onError(e);
            }
        }

        @Override
        public void onCompleted() {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                queue.put(COMPLETE_SENTINEL);
                schedule();
            } catch (InterruptedException e) {
                onError(e);
            }
        }

        @Override
        public void onError(final Throwable e) {
            try {
                // we want to block for natural back-pressure
                // so that the producer waits for each value to be consumed
                queue.put(new ErrorSentinel(e));
                schedule();
            } catch (InterruptedException e2) {
                // call directly if we can't schedule
                observer.onError(e2);
            }
        }

        protected void schedule() {
            if (counter.getAndIncrement() == 0) {
                if (recursiveScheduler == null) {
                    add(scheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            recursiveScheduler = inner;
                            pollQueue();
                        }

                    }));
                } else {
                    recursiveScheduler.schedule(new Action1<Inner>() {

                        @Override
                        public void call(Inner inner) {
                            pollQueue();
                        }

                    });
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void pollQueue() {
            do {
                Object v = queue.poll();
                if (v != null) {
                    if (v == NULL_SENTINEL) {
                        observer.onNext(null);
                    } else if (v == COMPLETE_SENTINEL) {
                        observer.onCompleted();
                    } else if (v instanceof ErrorSentinel) {
                        observer.onError(((ErrorSentinel) v).e);
                    } else {
                        observer.onNext((T) v);
                    }
                }
            } while (counter.decrementAndGet() > 0);
        }

    }

}