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
package rx.concurrency;

import static org.mockito.Mockito.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.badlogic.gdx.Gdx;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

/**
 * Executes work on the Gdx UI thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public final class GdxScheduler extends Scheduler {
    private static final GdxScheduler INSTANCE = new GdxScheduler();

    public static GdxScheduler get() {
        return INSTANCE;
    }

    private GdxScheduler() {
        // hide from public access
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                sub.set(action.call(GdxScheduler.this, state));
            }
        });
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                Subscription subscription = sub.get();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
        });
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, final long delayTime, final TimeUnit unit) {
        final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
        
        final long delayInMillis = unit.toMillis(delayTime);
        if (delayInMillis < 0) {
            throw new IllegalArgumentException("delay may not be negative (in milliseconds): " + delayInMillis);
        }
        
        final Thread sleeper = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(unit.toMillis(delayTime));
                    sub.set(schedule(state, action));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "gdx-scheduler-sleeper");
        sleeper.start();
        
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                sleeper.interrupt();
      
                Subscription subscription = sub.get();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
        });
    }

    @Override
    public <T> Subscription schedulePeriodically(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        final long periodInMillis = unit.toMillis(period);
        if (periodInMillis < 0) {
            throw new IllegalArgumentException("period may not be negative (in milliseconds): " + periodInMillis);
        }
        
        final long initialDelayInMillis = unit.toMillis(initialDelay);
        if (initialDelayInMillis < 0) {
            throw new IllegalArgumentException("initial delay may not be negative (in milliseconds): " + initialDelayInMillis);
        }
        
        final CompositeSubscription subscriptions = new CompositeSubscription();
        
        final ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() { 
                subscriptions.add(schedule(state, action));
            }
        }, initialDelayInMillis, periodInMillis, TimeUnit.MILLISECONDS);
        
        subscriptions.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                handle.cancel(true);
            }
        }));

        return subscriptions;
    }

    public static class UnitTest {
        @Rule
        public ExpectedException exception = ExpectedException.none();

        @Test
        public void testInvalidDelayValues() {
            final GdxScheduler scheduler = GdxScheduler.get();
            final Action0 action = mock(Action0.class);

            exception.expect(IllegalArgumentException.class);
            scheduler.schedulePeriodically(action, -1L, 100L, TimeUnit.SECONDS);

            exception.expect(IllegalArgumentException.class);
            scheduler.schedulePeriodically(action, 100L, -1L, TimeUnit.SECONDS);

            exception.expect(IllegalArgumentException.class);
            scheduler.schedulePeriodically(action, 1L + Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS);

            exception.expect(IllegalArgumentException.class);
            scheduler.schedulePeriodically(action, 100L, 1L + Integer.MAX_VALUE / 1000, TimeUnit.SECONDS);
        }
    }
}
