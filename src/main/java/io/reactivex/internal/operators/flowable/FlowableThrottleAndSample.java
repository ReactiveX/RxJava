package io.reactivex.internal.operators.flowable;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.SerializedSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class FlowableThrottleAndSample<T> extends AbstractFlowableWithUpstream<T, T> {

    private final long windowDuration;
    private final TimeUnit unit;
    private final Scheduler scheduler;

    private final boolean emitLast;

    public FlowableThrottleAndSample(@NonNull final Flowable<T> source, final long windowDuration,
                                     final TimeUnit unit, final Scheduler scheduler, final boolean emitLast) {
        super(source);
        this.windowDuration = windowDuration;
        this.unit = unit;
        this.scheduler = scheduler;
        this.emitLast = emitLast;
    }

    @Override
    protected void subscribeActual(final Subscriber<? super T> s) {
        final SerializedSubscriber<T> serial = new SerializedSubscriber<T>(s);
        if (emitLast) {
            source.subscribe(new ThrottleAndSampleEmitLast<T>(serial, windowDuration, unit, scheduler.createWorker()));
        } else {
            source.subscribe(new ThrottleAndSampleNoLast<T>(serial, windowDuration, unit, scheduler.createWorker()));
        }
    }

    abstract static class ThrottleAndSampleSubscriber<T> extends AtomicReference<T> implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = -7130465637537281443L;

        protected final Subscriber<? super T> actual;
        private final long windowDuration;
        private final TimeUnit unit;
        private final Scheduler.Worker worker;

        final AtomicLong requested = new AtomicLong();

        private final SequentialDisposable timer = new SequentialDisposable(Disposables.disposed());

        private Subscription subscription;

        boolean isDone;

        ThrottleAndSampleSubscriber(final Subscriber<? super T> actual, long windowDuration,
                                    final TimeUnit unit, final Scheduler.Worker worker) {
            this.actual = actual;
            this.windowDuration = windowDuration;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            if (SubscriptionHelper.validate(this.subscription, subscription)) {
                this.subscription = subscription;
                actual.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(final T value) {
            // Desired bahavior
            // source: -1-2-3-45------6-7-8-
            // Output: -1---3---5-----6---8-

            final Disposable throttling = timer.get();
            if (throttling.isDisposed()) {
                // Initial item comes or item comes after windowDuration
                //   - Emmit item, start timer
                emit(value);
                timer.replace(worker.schedule(this, windowDuration, unit));
            } else {
                // Timer is started and item comes (We need to throttle)
                //   - Store item
                set(value);
            }
        }

        @Override
        public void run() {
            // Timer ended
            //   - Check if we have an item to publish
            //      - If we do, publish, start throttle timer
            //      - If we do not, do not restart throttle timer
            final Disposable throttling = timer.get();
            throttling.dispose();
            final T value = getAndSet(null);
            if (value != null) {
                emit(value);
                timer.replace(worker.schedule(this, windowDuration, unit));
            }
        }

        @Override
        public void onError(final Throwable t) {
            if (isDone) {
                RxJavaPlugins.onError(t);
                return;
            }
            isDone = true;
            cancelTimer();
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (isDone) {
                return;
            }
            isDone = true;
            cancelTimer();
            complete();
        }

        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            cancelTimer();
            subscription.cancel();
        }

        private void cancelTimer() {
            DisposableHelper.dispose(timer);
            worker.dispose();
        }

        void emit(final T value) {
            if (value != null) {
                long r = requested.get();
                if (r != 0L) {
                    actual.onNext(value);
                    BackpressureHelper.produced(requested, 1);
                } else {
                    cancel();
                    actual.onError(new MissingBackpressureException("Couldn't emit value due to lack of requests!"));
                }
            }
        }

        abstract void complete();
    }

    static final class ThrottleAndSampleNoLast<T> extends ThrottleAndSampleSubscriber<T> {

        private static final long serialVersionUID = -7139995637537281443L;

        ThrottleAndSampleNoLast(final Subscriber<? super T> actual, final long windowDuration,
                                final TimeUnit unit, final Scheduler.Worker worker) {
            super(actual, windowDuration, unit, worker);
        }

        @Override
        void complete() {
            actual.onComplete();
        }
    }

    static final class ThrottleAndSampleEmitLast<T> extends ThrottleAndSampleSubscriber<T> {

        private static final long serialVersionUID = -7139995637537281443L;

        ThrottleAndSampleEmitLast(final Subscriber<? super T> actual, final long windowDuration,
                                  final TimeUnit unit, final Scheduler.Worker worker) {
            super(actual, windowDuration, unit, worker);
        }

        @Override
        void complete() {
            final T value = getAndSet(null);
            if (value != null) {
                emit(value);
            }
            actual.onComplete();
        }
    }
}
