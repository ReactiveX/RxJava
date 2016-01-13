/**
 * Copyright 2014 Netflix, Inc.
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
package rx;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class SchedulerWorkerTest {
    
    static final class CustomDriftScheduler extends Scheduler {
        public volatile long drift;
        @Override
        public Worker createWorker() {
            final Worker w = Schedulers.computation().createWorker();
            return new Worker() {

                @Override
                public void unsubscribe() {
                    w.unsubscribe();
                }

                @Override
                public boolean isUnsubscribed() {
                    return w.isUnsubscribed();
                }

                @Override
                public Subscription schedule(Action0 action) {
                    return w.schedule(action);
                }

                @Override
                public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
                    return w.schedule(action, delayTime, unit);
                }
                
                @Override
                public long now() {
                    return super.now() + drift;
                }
            };
        }
        
        @Override
        public long now() {
            return super.now() + drift;
        }
    }
    
    @Test
    public void testCurrentTimeDriftBackwards() throws Exception {
        CustomDriftScheduler s = new CustomDriftScheduler();
        
        Scheduler.Worker w = s.createWorker();
        
        try {
            final List<Long> times = new ArrayList<Long>();
            
            Subscription d = w.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    times.add(System.currentTimeMillis());
                }
            }, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(150);
            
            s.drift = -1000 - TimeUnit.NANOSECONDS.toMillis(Scheduler.CLOCK_DRIFT_TOLERANCE_NANOS);
            
            Thread.sleep(400);
            
            d.unsubscribe();
            
            Thread.sleep(150);
            
            System.out.println("Runs: " + times.size());
            
            for (int i = 0; i < times.size() - 1 ; i++) {
                long diff = times.get(i + 1) - times.get(i);
                System.out.println("Diff #" + i + ": " + diff);
                assertTrue("" + i + ":" + diff, diff < 150 && diff > 50);
            }

            assertTrue("Too few invocations: " + times.size(), times.size() > 2);
            
        } finally {
            w.unsubscribe();
        }
        
    }
    
    @Test
    public void testCurrentTimeDriftForwards() throws Exception {
        CustomDriftScheduler s = new CustomDriftScheduler();
        
        Scheduler.Worker w = s.createWorker();
        
        try {
            final List<Long> times = new ArrayList<Long>();
            
            Subscription d = w.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    times.add(System.currentTimeMillis());
                }
            }, 100, 100, TimeUnit.MILLISECONDS);

            Thread.sleep(150);
            
            s.drift = 1000 + TimeUnit.NANOSECONDS.toMillis(Scheduler.CLOCK_DRIFT_TOLERANCE_NANOS);
            
            Thread.sleep(400);
            
            d.unsubscribe();
            
            Thread.sleep(150);
            
            System.out.println("Runs: " + times.size());
            
            assertTrue(times.size() > 2);
            
            for (int i = 0; i < times.size() - 1 ; i++) {
                long diff = times.get(i + 1) - times.get(i);
                System.out.println("Diff #" + i + ": " + diff);
                assertTrue("Diff out of range: " + diff, diff < 250 && diff > 50);
            }
            
        } finally {
            w.unsubscribe();
        }
        
    }
}
