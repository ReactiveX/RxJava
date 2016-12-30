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

import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.subscriptions.SequentialSubscription;

/**
 * Utility method for scheduling tasks periodically (at a fixed rate) by using Worker.schedule(Action0, long, TimeUnit).
 */
public final class SchedulePeriodicHelper {

    /** Utility class. */
    private SchedulePeriodicHelper() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * The tolerance for a clock drift in nanoseconds where the periodic scheduler will rebase.
     * <p>
     * The associated system parameter, {@code rx.scheduler.drift-tolerance}, expects its value in minutes.
     */
    public static final long CLOCK_DRIFT_TOLERANCE_NANOS;
    static {
        CLOCK_DRIFT_TOLERANCE_NANOS = TimeUnit.MINUTES.toNanos(
                Long.getLong("rx.scheduler.drift-tolerance", 15));
    }

    /**
     * Return the current time in nanoseconds.
     */
    public interface NowNanoSupplier {
        long nowNanos();
    }

    public static Subscription schedulePeriodically(
            final Worker worker,
            final Action0 action,
            long initialDelay, long period, TimeUnit unit,
            final NowNanoSupplier nowNanoSupplier) {
        final long periodInNanos = unit.toNanos(period);
        final long firstNowNanos = nowNanoSupplier != null ? nowNanoSupplier.nowNanos() : TimeUnit.MILLISECONDS.toNanos(worker.now());
        final long firstStartInNanos = firstNowNanos + unit.toNanos(initialDelay);

        final SequentialSubscription first = new SequentialSubscription();
        final SequentialSubscription mas = new SequentialSubscription(first);

        final Action0 recursiveAction = new Action0() {
            long count;
            long lastNowNanos = firstNowNanos;
            long startInNanos = firstStartInNanos;
            @Override
            public void call() {
                action.call();

                if (!mas.isUnsubscribed()) {

                    long nextTick;

                    long nowNanos = nowNanoSupplier != null ? nowNanoSupplier.nowNanos() : TimeUnit.MILLISECONDS.toNanos(worker.now());
                    // If the clock moved in a direction quite a bit, rebase the repetition period
                    if (nowNanos + CLOCK_DRIFT_TOLERANCE_NANOS < lastNowNanos
                            || nowNanos >= lastNowNanos + periodInNanos + CLOCK_DRIFT_TOLERANCE_NANOS) {
                        nextTick = nowNanos + periodInNanos;
                        /*
                         * Shift the start point back by the drift as if the whole thing
                         * started count periods ago.
                         */
                        startInNanos = nextTick - (periodInNanos * (++count));
                    } else {
                        nextTick = startInNanos + (++count * periodInNanos);
                    }
                    lastNowNanos = nowNanos;

                    long delay = nextTick - nowNanos;
                    mas.replace(worker.schedule(this, delay, TimeUnit.NANOSECONDS));
                }
            }
        };
        first.replace(worker.schedule(recursiveAction, initialDelay, unit));
        return mas;
    }

}
