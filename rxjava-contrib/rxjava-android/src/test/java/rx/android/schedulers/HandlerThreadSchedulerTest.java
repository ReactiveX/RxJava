/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.android.schedulers;

import android.os.Handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import rx.Scheduler;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.util.functions.Func2;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE)
public class HandlerThreadSchedulerTest {

    @Test
    public void shouldScheduleImmediateActionOnHandlerThread() {
        final Handler handler = mock(Handler.class);
        final Object state = new Object();
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
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
