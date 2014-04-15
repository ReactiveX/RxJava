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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Schedulable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.ImmediateScheduler;
import rx.schedulers.TrampolineScheduler;

/**
 * Delivers events on the specified Scheduler asynchronously via an unbounded buffer.
 * 
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperatorObserveOn<T> implements Operator<T, T> {

    private final Scheduler scheduler;

    /**
     * @param scheduler
     */
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
        } else {
            return new ObserveOnSubscriber(child);
        }
    }

    private final NotificationLite<T> on = NotificationLite.instance();

    /** Observe through individual queue per observer. */
    private class ObserveOnSubscriber extends Subscriber<T> {
        final Subscriber<? super T> observer;
        private volatile Scheduler.EventLoop recursiveScheduler;

        private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<Object>();
        final AtomicLong counter = new AtomicLong(0);

        public ObserveOnSubscriber(Subscriber<? super T> observer) {
            super(observer);
            this.observer = observer;
        }

        @Override
        public void onNext(final T t) {
            queue.offer(on.next(t));
            schedule();
        }

        @Override
        public void onCompleted() {
            queue.offer(on.completed());
            schedule();
        }

        @Override
        public void onError(final Throwable e) {
            queue.offer(on.error(e));
            schedule();
        }

        protected void schedule() {
            if (counter.getAndIncrement() == 0) {
                if (recursiveScheduler == null) {
                    recursiveScheduler = scheduler.createEventLoop();
                    add(recursiveScheduler);
                }
                recursiveScheduler.schedule(new Action1<Schedulable>() {

                    @Override
                    public void call(Schedulable inner) {
                        pollQueue();
                    }

                });
            }
        }

        private void pollQueue() {
            do {
                Object v = queue.poll();
                on.accept(observer, v);
            } while (counter.decrementAndGet() > 0);
        }

    }

}