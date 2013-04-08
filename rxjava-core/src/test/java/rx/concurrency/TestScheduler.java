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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func0;

public class TestScheduler extends AbstractScheduler {
    private final Queue<TimedAction> queue = new PriorityQueue<TimedAction>(11, new CompareActionsByTime());
    
    private static class TimedAction {
        private final long time;
        private final Func0<Subscription> action;

        private TimedAction(long time, Func0<Subscription> action) {
            this.time = time;
            this.action = action;
        }
    }
    
    private static class CompareActionsByTime implements Comparator<TimedAction> {
      @Override
      public int compare(TimedAction action1, TimedAction action2) {
        return Long.valueOf(action1.time).compareTo(Long.valueOf(action2.time));
      }
    }
    
    private long time;
    
    @Override
    public Subscription schedule(Func0<Subscription> action) {
      return schedule(action, 0L, TimeUnit.NANOSECONDS);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
      queue.add(new TimedAction(now() + unit.toNanos(dueTime), action));
      return Subscriptions.empty();
    }
    
    @Override
    public long now() {
        return time;
    }

    public void advanceTimeBy(long dueTime, TimeUnit unit) {
      advanceTimeTo(time + unit.toNanos(dueTime), TimeUnit.NANOSECONDS);
    }
    
    public void advanceTimeTo(long dueTime, TimeUnit unit) {
        long targetTime = unit.toNanos(dueTime);
        triggerActions(targetTime);
    }

    public void triggerActions() {
      triggerActions(time);
    }
    
    private void triggerActions(long targetTimeInNanos) {
        while (! queue.isEmpty()) {
            TimedAction current = queue.peek();
            if (current.time > targetTimeInNanos) {
                break;
            }
            time = current.time;
            queue.remove();
            current.action.call();
        }
    }
}
