package rx.android.concurrency;

import android.os.Handler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import rx.Scheduler;
import rx.Subscription;
import rx.operators.AtomicObservableSubscription;
import rx.util.functions.Func2;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;

    /**
     * Constructs a {@link HandlerThreadScheduler} using the given {@link Handler}
     * @param handler {@link Handler} to use when scheduling actions
     */
    public HandlerThreadScheduler(Handler handler) {
        this.handler = handler;
    }

    /**
     * Calls {@link HandlerThreadScheduler#schedule(Object, rx.util.functions.Func2, long, java.util.concurrent.TimeUnit)}
     * with a delay of zero milliseconds.
     *
     * See {@link #schedule(Object, rx.util.functions.Func2, long, java.util.concurrent.TimeUnit)}
     */
    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        return schedule(state, action, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * Calls {@link Handler#postDelayed(Runnable, long)} with a runnable that executes the given action.
     * @param state
     *            State to pass into the action.
     * @param action
     *            Action to schedule.
     * @param delayTime
     *            Time the action is to be delayed before executing.
     * @param unit
     *            Time unit of the delay time.
     * @return A Subscription from which one can unsubscribe from.
     */
    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Scheduler _scheduler = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        }, unit.toMillis(delayTime));
        return subscription;
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest=Config.NONE)
    public static final class UnitTest {

        @Test
        public void shouldScheduleImmediateActionOnHandlerThread() {
            final Handler handler = mock(Handler.class);
            final Object state = new Object();
            final Func2<Scheduler, Object, Subscription> action = mock(Func2.class);

            Scheduler scheduler = new HandlerThreadScheduler(handler);
            scheduler.schedule(state, action);

            // verify that we post to the given Handler
            ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
            verify(handler).postDelayed(runnable.capture(), eq(0L));

            // verify that the given handler delegates to our action
            runnable.getValue().run();
            verify(action).call(scheduler, state);
        }

        @Test
        public void shouldScheduleDelayedActionOnHandlerThread() {
            final Handler handler = mock(Handler.class);
            final Object state = new Object();
            final Func2<Scheduler, Object, Subscription> action = mock(Func2.class);

            Scheduler scheduler = new HandlerThreadScheduler(handler);
            scheduler.schedule(state, action, 1L, TimeUnit.SECONDS);

            // verify that we post to the given Handler
            ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
            verify(handler).postDelayed(runnable.capture(), eq(1000L));

            // verify that the given handler delegates to our action
            runnable.getValue().run();
            verify(action).call(scheduler, state);
        }
    }
}


