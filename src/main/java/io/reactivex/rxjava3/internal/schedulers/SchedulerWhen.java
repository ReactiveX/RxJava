/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.schedulers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.processors.*;

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
 * When the {@link io.reactivex.rxjava3.core.Scheduler.Worker Worker} is unsubscribed the {@link Completable} emits an
 * onComplete and triggers any behavior in the flattening operator. The
 * {@link Observable} and all {@link Completable}s give to the flattening
 * function never onError.
 * <p>
 * Limit the amount concurrency two at a time without creating a new fix size
 * thread pool:
 *
 * <pre>
 * {@code
 * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
 *  // use merge max concurrent to limit the number of concurrent
 *  // callbacks two at a time
 *  return Completable.merge(Observable.merge(workers), 2);
 * });
 * }
 * </pre>
 * <p>
 * This is a slightly different way to limit the concurrency but it has some
 * interesting benefits and drawbacks to the method above. It works by limited
 * the number of concurrent {@link io.reactivex.rxjava3.core.Scheduler.Worker Worker}s rather than individual actions.
 * Generally each {@link Observable} uses its own {@link io.reactivex.rxjava3.core.Scheduler.Worker Worker}. This means
 * that this will essentially limit the number of concurrent subscribes. The
 * danger comes from using operators like
 * {@link Flowable#zip(org.reactivestreams.Publisher, org.reactivestreams.Publisher, io.reactivex.rxjava3.functions.BiFunction)} where
 * subscribing to the first {@link Observable} could deadlock the subscription
 * to the second.
 *
 * <pre>
 * {@code
 * Scheduler limitScheduler = Schedulers.computation().when(workers -> {
 *  // use merge max concurrent to limit the number of concurrent
 *  // Observables two at a time
 *  return Completable.merge(Observable.merge(workers, 2));
 * });
 * }
 * </pre>
 *
 * Slowing down the rate to no more than than 1 a second. This suffers from the
 * same problem as the one above I could find an {@link Observable} operator
 * that limits the rate without dropping the values (aka leaky bucket
 * algorithm).
 *
 * <pre>
 * {@code
 * Scheduler slowScheduler = Schedulers.computation().when(workers -> {
 *  // use concatenate to make each worker happen one at a time.
 *  return Completable.concat(workers.map(actions -> {
 *      // delay the starting of the next worker by 1 second.
 *      return Completable.merge(actions.delaySubscription(1, TimeUnit.SECONDS));
 *  }));
 * });
 * }
 * </pre>
 * <p>History 2.0.1 - experimental
 * @since 2.1
 */
public class SchedulerWhen extends Scheduler implements Disposable {
    private final Scheduler actualScheduler;
    private final FlowableProcessor<Flowable<Completable>> workerProcessor;
    private Disposable disposable;

    public SchedulerWhen(Function<Flowable<Flowable<Completable>>, Completable> combine, Scheduler actualScheduler) {
        this.actualScheduler = actualScheduler;
        // workers are converted into completables and put in this queue.
        this.workerProcessor = UnicastProcessor.<Flowable<Completable>>create().toSerialized();
        // send it to a custom combinator to pick the order and rate at which
        // workers are processed.
        try {
            disposable = combine.apply(workerProcessor).subscribe();
        } catch (Throwable e) {
            throw ExceptionHelper.wrapOrThrow(e);
        }
    }

    @Override
    public void dispose() {
        disposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposable.isDisposed();
    }

    @NonNull
    @Override
    public Worker createWorker() {
        final Worker actualWorker = actualScheduler.createWorker();
        // a queue for the actions submitted while worker is waiting to get to
        // the subscribe to off the workerQueue.
        final FlowableProcessor<ScheduledAction> actionProcessor = UnicastProcessor.<ScheduledAction>create().toSerialized();
        // convert the work of scheduling all the actions into a completable
        Flowable<Completable> actions = actionProcessor.map(new CreateWorkerFunction(actualWorker));

        // a worker that queues the action to the actionQueue subject.
        Worker worker = new QueueWorker(actionProcessor, actualWorker);

        // enqueue the completable that process actions put in reply subject
        workerProcessor.onNext(actions);

        // return the worker that adds actions to the reply subject
        return worker;
    }

    static final Disposable SUBSCRIBED = new SubscribedDisposable();

    static final Disposable DISPOSED = Disposable.disposed();

    @SuppressWarnings("serial")
    abstract static class ScheduledAction extends AtomicReference<Disposable> implements Disposable {
        ScheduledAction() {
            super(SUBSCRIBED);
        }

        void call(Worker actualWorker, CompletableObserver actionCompletable) {
            Disposable oldState = get();
            // either SUBSCRIBED or UNSUBSCRIBED
            if (oldState == DISPOSED) {
                // no need to schedule return
                return;
            }
            if (oldState != SUBSCRIBED) {
                // has already been scheduled return
                // should not be able to get here but handle it anyway by not
                // rescheduling.
                return;
            }

            Disposable newState = callActual(actualWorker, actionCompletable);

            if (!compareAndSet(SUBSCRIBED, newState)) {
                // set would only fail if the new current state is some other
                // subscription from a concurrent call to this method.
                // Unsubscribe from the action just scheduled because it lost
                // the race.
                newState.dispose();
            }
        }

        protected abstract Disposable callActual(Worker actualWorker, CompletableObserver actionCompletable);

        @Override
        public boolean isDisposed() {
            return get().isDisposed();
        }

        @Override
        public void dispose() {
            getAndSet(DISPOSED).dispose();
        }
    }

    @SuppressWarnings("serial")
    static class ImmediateAction extends ScheduledAction {
        private final Runnable action;

        ImmediateAction(Runnable action) {
            this.action = action;
        }

        @Override
        protected Disposable callActual(Worker actualWorker, CompletableObserver actionCompletable) {
            return actualWorker.schedule(new OnCompletedAction(action, actionCompletable));
        }
    }

    @SuppressWarnings("serial")
    static class DelayedAction extends ScheduledAction {
        private final Runnable action;
        private final long delayTime;
        private final TimeUnit unit;

        DelayedAction(Runnable action, long delayTime, TimeUnit unit) {
            this.action = action;
            this.delayTime = delayTime;
            this.unit = unit;
        }

        @Override
        protected Disposable callActual(Worker actualWorker, CompletableObserver actionCompletable) {
            return actualWorker.schedule(new OnCompletedAction(action, actionCompletable), delayTime, unit);
        }
    }

    static class OnCompletedAction implements Runnable {
        final CompletableObserver actionCompletable;
        final Runnable action;

        OnCompletedAction(Runnable action, CompletableObserver actionCompletable) {
            this.action = action;
            this.actionCompletable = actionCompletable;
        }

        @Override
        public void run() {
            try {
                action.run();
            } finally {
                actionCompletable.onComplete();
            }
        }
    }

    static final class CreateWorkerFunction implements Function<ScheduledAction, Completable> {
        final Worker actualWorker;

        CreateWorkerFunction(Worker actualWorker) {
            this.actualWorker = actualWorker;
        }

        @Override
        public Completable apply(final ScheduledAction action) {
            return new WorkerCompletable(action);
        }

        final class WorkerCompletable extends Completable {
            final ScheduledAction action;

            WorkerCompletable(ScheduledAction action) {
                this.action = action;
            }

            @Override
            protected void subscribeActual(CompletableObserver actionCompletable) {
                actionCompletable.onSubscribe(action);
                action.call(actualWorker, actionCompletable);
            }
        }
    }

    static final class QueueWorker extends Worker {
        private final AtomicBoolean unsubscribed;
        private final FlowableProcessor<ScheduledAction> actionProcessor;
        private final Worker actualWorker;

        QueueWorker(FlowableProcessor<ScheduledAction> actionProcessor, Worker actualWorker) {
            this.actionProcessor = actionProcessor;
            this.actualWorker = actualWorker;
            unsubscribed = new AtomicBoolean();
        }

        @Override
        public void dispose() {
            // complete the actionQueue when worker is unsubscribed to make
            // room for the next worker in the workerQueue.
            if (unsubscribed.compareAndSet(false, true)) {
                actionProcessor.onComplete();
                actualWorker.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return unsubscribed.get();
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull final Runnable action, final long delayTime, @NonNull final TimeUnit unit) {
            // send a scheduled action to the actionQueue
            DelayedAction delayedAction = new DelayedAction(action, delayTime, unit);
            actionProcessor.onNext(delayedAction);
            return delayedAction;
        }

        @NonNull
        @Override
        public Disposable schedule(@NonNull final Runnable action) {
            // send a scheduled action to the actionQueue
            ImmediateAction immediateAction = new ImmediateAction(action);
            actionProcessor.onNext(immediateAction);
            return immediateAction;
        }
    }

    static final class SubscribedDisposable implements Disposable {
        @Override
        public void dispose() {
        }

        @Override
        public boolean isDisposed() {
            return false;
        }
    }
}
