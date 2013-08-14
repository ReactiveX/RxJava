package rx.concurrency;

import android.os.Handler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import rx.Scheduler;
import rx.Subscription;
import rx.android.testsupport.AndroidTestRunner;
import rx.operators.AtomicObservableSubscription;
import rx.util.functions.Func2;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;

    public HandlerThreadScheduler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Scheduler _scheduler = this;

        handler.post(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        });
        return subscription;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit) {
        if (delayTime == 0) {
            return schedule(state, action);
        } else {
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
    }

    @RunWith(AndroidTestRunner.class)
    public static final class UnitTest {

        @Test
        public void shouldScheduleActionOnHandlerThread() {
            final Handler handler = mock(Handler.class);
            final Object state = new Object();
            final Func2<Scheduler, Object, Subscription> action = mock(Func2.class);

            Scheduler scheduler = new HandlerThreadScheduler(handler);
            scheduler.schedule(state, action);

            // verify that we post to the given Handler
            ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
            verify(handler).post(runnable.capture());

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

        @Test
        public void scheduleDelayedActionShouldForwardToNormalPostIfDelayIsZero() {
            final Handler handler = mock(Handler.class);
            final Object state = new Object();
            final Func2<Scheduler, Object, Subscription> action = mock(Func2.class);

            Scheduler scheduler = new HandlerThreadScheduler(handler);
            scheduler.schedule(state, action, 0L, TimeUnit.SECONDS);

            // verify that we post to the given Handler
            verify(handler).post(any(Runnable.class));
            verify(handler, never()).postDelayed(any(Runnable.class), anyLong());
        }

    }
}


