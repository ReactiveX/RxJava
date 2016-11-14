/**
 * Copyright 2016 Netflix, Inc.
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
package rx.internal.schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Completable;
import rx.Completable.OnSubscribe;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.annotations.Experimental;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.internal.operators.BufferUntilSubscriber;
import rx.observers.SerializedObserver;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

/**
 * Allows the use of operators for controlling the timing around when actions
 * scheduled on workers are actually done. This makes it possible to layer
 * additional behavior on this {@link Scheduler}. The only parameter is a
 * function that flattens an {@link Observable} of {@link Observable} of
 * {@link Completable}s into just one {@link Completable}. There must be a chain
 * of operators connecting the returned value to the source {@link Observable}
 * otherwise any work scheduled on the returned {@link Scheduler} will not be
 * executed.
 * <p>
 * When {@link Scheduler#createWorker()} is invoked a {@link Observable} of
 * {@link Completable}s is onNext'd to the combinator to be flattened. If the
 * inner {@link Observable} is not immediately subscribed to an calls to
 * {@link Worker#schedule} are buffered. Once the {@link Observable} is
 * subscribed to actions are then onNext'd as {@link Completable}s.
 * <p>
 * Finally the actions scheduled on the parent {@link Scheduler} when the inner
 * most {@link Completable}s are subscribed to.
 * <p>
 * When the {@link rx.Scheduler.Worker} is unsubscribed the {@link Completable} emits an
 * onComplete and triggers any behavior in the flattening operator. The
 * {@link Observable} and all {@link Completable}s give to the flattening
 * function never onError.
 * <p>
 * Limit the amount concurrency two at a time without creating a new fix size
 * thread pool:
 *
 * <pre>
 * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
 *     // use merge max concurrent to limit the number of concurrent
 *     // callbacks two at a time
 *     return Completable.merge(Observable.merge(workers), 2);
 * });
 * </pre>
 * <p>
 * This is a slightly different way to limit the concurrency but it has some
 * interesting benefits and drawbacks to the method above. It works by limited
 * the number of concurrent {@link rx.Scheduler.Worker}s rather than individual actions.
 * Generally each {@link Observable} uses its own {@link rx.Scheduler.Worker}. This means
 * that this will essentially limit the number of concurrent subscribes. The
 * danger comes from using operators like
 * {@link Observable#zip(Observable, Observable, rx.functions.Func2)} where
 * subscribing to the first {@link Observable} could deadlock the subscription
 * to the second.
 *
 * <pre>
 * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
 *     // use merge max concurrent to limit the number of concurrent
 *     // Observables two at a time
 *     return Completable.merge(Observable.merge(workers, 2));
 * });
 * </pre>
 *
 * Slowing down the rate to no more than than 1 a second. This suffers from the
 * same problem as the one above I could find an {@link Observable} operator
 * that limits the rate without dropping the values (aka leaky bucket
 * algorithm).
 *
 * <pre>
 * Scheduler slowScheduler = Schedulers.computation().when(workers -> {
 *     // use concatenate to make each worker happen one at a time.
 *     return Completable.concat(workers.map(actions -> {
 *         // delay the starting of the next worker by 1 second.
 *         return Completable.merge(actions.delaySubscription(1, TimeUnit.SECONDS));
 *     }));
 * });
 * </pre>
 */
@Experimental
public class SchedulerWhen extends Scheduler implements Subscription {
    private final Scheduler actualScheduler;
    private final Observer<Observable<Completable>> workerObserver;
    private final Subscription subscription;

    public SchedulerWhen(Func1<Observable<Observable<Completable>>, Completable> combine, Scheduler actualScheduler) {
        this.actualScheduler = actualScheduler;
        // workers are converted into completables and put in this queue.
        PublishSubject<Observable<Completable>> workerSubject = PublishSubject.create();
        this.workerObserver = new SerializedObserver<Observable<Completable>>(workerSubject);
        // send it to a custom combinator to pick the order and rate at which
        // workers are processed.
        this.subscription = combine.call(workerSubject.onBackpressureBuffer()).subscribe();
    }

    @Override
    public void unsubscribe() {
        subscription.unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return subscription.isUnsubscribed();
    }

    @Override
    public Worker createWorker() {
        final Worker actualWorker = actualScheduler.createWorker();
        // a queue for the actions submitted while worker is waiting to get to
        // the subscribe to off the workerQueue.
        BufferUntilSubscriber<ScheduledAction> actionSubject = BufferUntilSubscriber.<ScheduledAction> create();
        final Observer<ScheduledAction> actionObserver = new SerializedObserver<ScheduledAction>(actionSubject);
        // convert the work of scheduling all the actions into a completable
        Observable<Completable> actions = actionSubject.map(new Func1<ScheduledAction, Completable>() {
            @Override
            public Completable call(final ScheduledAction action) {
                return Completable.create(new OnSubscribe() {
                    @Override
                    public void call(CompletableSubscriber actionCompletable) {
                        actionCompletable.onSubscribe(action);
                        action.call(actualWorker, actionCompletable);
                    }
                });
            }
        });

        // a worker that queues the action to the actionQueue subject.
        Worker worker = new Worker() {
            private final AtomicBoolean unsubscribed = new AtomicBoolean();

            @Override
            public void unsubscribe() {
                // complete the actionQueue when worker is unsubscribed to make
                // room for the next worker in the workerQueue.
                if (unsubscribed.compareAndSet(false, true)) {
                    actualWorker.unsubscribe();
                    actionObserver.onCompleted();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return unsubscribed.get();
            }

            @Override
            public Subscription schedule(final Action0 action, final long delayTime, final TimeUnit unit) {
                // send a scheduled action to the actionQueue
                DelayedAction delayedAction = new DelayedAction(action, delayTime, unit);
                actionObserver.onNext(delayedAction);
                return delayedAction;
            }

            @Override
            public Subscription schedule(final Action0 action) {
                // send a scheduled action to the actionQueue
                ImmediateAction immediateAction = new ImmediateAction(action);
                actionObserver.onNext(immediateAction);
                return immediateAction;
            }
        };

        // enqueue the completable that process actions put in reply subject
        workerObserver.onNext(actions);

        // return the worker that adds actions to the reply subject
        return worker;
    }

    static final Subscription SUBSCRIBED = new Subscription() {
        @Override
        public void unsubscribe() {
        }

        @Override
        public boolean isUnsubscribed() {
            return false;
        }
    };

    static final Subscription UNSUBSCRIBED = Subscriptions.unsubscribed();

    @SuppressWarnings("serial")
    static abstract class ScheduledAction extends AtomicReference<Subscription>implements Subscription {
        public ScheduledAction() {
            super(SUBSCRIBED);
        }

        private void call(Worker actualWorker, CompletableSubscriber actionCompletable) {
            Subscription oldState = get();
            // either SUBSCRIBED or UNSUBSCRIBED
            if (oldState == UNSUBSCRIBED) {
                // no need to schedule return
                return;
            }
            if (oldState != SUBSCRIBED) {
                // has already been scheduled return
                // should not be able to get here but handle it anyway by not
                // rescheduling.
                return;
            }

            Subscription newState = callActual(actualWorker, actionCompletable);

            if (!compareAndSet(SUBSCRIBED, newState)) {
                // set would only fail if the new current state is some other
                // subscription from a concurrent call to this method.
                // Unsubscribe from the action just scheduled because it lost
                // the race.
                newState.unsubscribe();
            }
        }

        protected abstract Subscription callActual(Worker actualWorker, CompletableSubscriber actionCompletable);

        @Override
        public boolean isUnsubscribed() {
            return get().isUnsubscribed();
        }

        @Override
        public void unsubscribe() {
            Subscription oldState;
            // no matter what the current state is the new state is going to be
            Subscription newState = UNSUBSCRIBED;
            do {
                oldState = get();
                if (oldState == UNSUBSCRIBED) {
                    // the action has already been unsubscribed
                    return;
                }
            } while (!compareAndSet(oldState, newState));

            if (oldState != SUBSCRIBED) {
                // the action was scheduled. stop it.
                oldState.unsubscribe();
            }
        }
    }

    @SuppressWarnings("serial")
    static class ImmediateAction extends ScheduledAction {
        private final Action0 action;

        public ImmediateAction(Action0 action) {
            this.action = action;
        }

        @Override
        protected Subscription callActual(Worker actualWorker, CompletableSubscriber actionCompletable) {
            return actualWorker.schedule(new OnCompletedAction(action, actionCompletable));
        }
    }

    @SuppressWarnings("serial")
    static class DelayedAction extends ScheduledAction {
        private final Action0 action;
        private final long delayTime;
        private final TimeUnit unit;

        public DelayedAction(Action0 action, long delayTime, TimeUnit unit) {
            this.action = action;
            this.delayTime = delayTime;
            this.unit = unit;
        }

        @Override
        protected Subscription callActual(Worker actualWorker, CompletableSubscriber actionCompletable) {
            return actualWorker.schedule(new OnCompletedAction(action, actionCompletable), delayTime, unit);
        }
    }

    static class OnCompletedAction implements Action0 {
        private CompletableSubscriber actionCompletable;
        private Action0 action;

        public OnCompletedAction(Action0 action, CompletableSubscriber actionCompletable) {
            this.action = action;
            this.actionCompletable = actionCompletable;
        }

        @Override
        public void call() {
            try {
                action.call();
            } finally {
                actionCompletable.onCompleted();
            }
        }
    }
}
