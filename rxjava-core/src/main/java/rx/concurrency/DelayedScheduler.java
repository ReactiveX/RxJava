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
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

/**
 * Scheduler that delays the underlying scheduler by a fixed time delay.
 */
public class DelayedScheduler extends Scheduler {
    private final Scheduler underlying;
    private final long delay;
    private final TimeUnit unit;
    
    public DelayedScheduler(Scheduler underlying, long delay, TimeUnit unit) {
        this.underlying = underlying;
        this.delay = delay;
        this.unit = unit;
    }
    
    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return underlying.schedule(state, action, delay, unit);
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delay, TimeUnit unit) {
        long newDelay = unit.toNanos(delay) + this.unit.toNanos(this.delay); 
        return underlying.schedule(state, action, newDelay, TimeUnit.NANOSECONDS);
    }
    
    public static class UnitTest {
        @Mock 
        Action0 action;
        
        private TestScheduler scheduler = new TestScheduler();
        
        @Before
        public void before() {
            initMocks(this);
        }
        
        @Test
        public void testNotDelayingAnAction() {
            Scheduler delayed = new DelayedScheduler(scheduler, 0, TimeUnit.SECONDS);
            delayed.schedule(action);
            delayed.schedule(action, 1L, TimeUnit.SECONDS);

            InOrder inOrder = inOrder(action);
            
            scheduler.triggerActions();
            inOrder.verify(action, times(1)).call();
            
            scheduler.advanceTimeTo(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(action, never()).call();

            scheduler.advanceTimeTo(1L, TimeUnit.SECONDS);
            inOrder.verify(action, times(1)).call();

            scheduler.advanceTimeTo(5L, TimeUnit.SECONDS);
            inOrder.verify(action, never()).call();
        }

        @Test
        public void testdelayingAnAction() {
            Scheduler delayed = new DelayedScheduler(scheduler, 500, TimeUnit.MILLISECONDS);
            delayed.schedule(action);
            delayed.schedule(action, 1L, TimeUnit.SECONDS);

            InOrder inOrder = inOrder(action);
            
            scheduler.advanceTimeTo(499L, TimeUnit.MILLISECONDS);
            inOrder.verify(action, never()).call();
            
            scheduler.advanceTimeTo(500L, TimeUnit.MILLISECONDS);
            inOrder.verify(action, times(1)).call();

            scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
            inOrder.verify(action, never()).call();

            scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
            inOrder.verify(action, times(1)).call();
            
            scheduler.advanceTimeTo(5L, TimeUnit.SECONDS);
            inOrder.verify(action, never()).call();
        }
    }
}
