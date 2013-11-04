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

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

import java.util.concurrent.TimeUnit;

/**
 * Executes work immediately on the current thread.
 */
public final class ImmediateScheduler extends Scheduler {
  private static final ImmediateScheduler INSTANCE = new ImmediateScheduler();

  public static ImmediateScheduler getInstance() {
    return INSTANCE;
  }

  ImmediateScheduler() {
  }

  @Override
  public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
    return action.call(this, state);
  }

  @Override
  public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long dueTime, TimeUnit unit) {
    // since we are executing immediately on this thread we must cause this thread to sleep
    long execTime = now() + unit.toMillis(dueTime);

    return schedule(state, new SleepingAction<T>(action, this, execTime));
  }
}
