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
package rx.schedulers;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

public class ExecutorSchedulerTests extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        // this is an implementation of ExecutorScheduler
        return Schedulers.threadPoolForComputation();
    }

    @Test
    public void testThreadSafetyWhenSchedulerIsHoppingBetweenThreads() {

        final int NUM = 1000000;
        final CountDownLatch latch = new CountDownLatch(1);
        HashMap<String, Integer> statefulMap = new HashMap<String, Integer>();
        Schedulers.threadPoolForComputation().schedule(statefulMap,
                new Func2<Scheduler, HashMap<String, Integer>, Subscription>() {

                    @Override
                    public Subscription call(Scheduler innerScheduler, final HashMap<String, Integer> statefulMap) {
                        return innerScheduler.schedule(new Action1<Action0>() {

                            int nonThreadSafeCounter = 0;

                            @Override
                            public void call(Action0 self) {
                                Integer i = statefulMap.get("a");
                                if (i == null) {
                                    i = 1;
                                    statefulMap.put("a", i);
                                    statefulMap.put("b", i);
                                } else {
                                    i++;
                                    statefulMap.put("a", i);
                                    statefulMap.put("b", i);
                                }
                                nonThreadSafeCounter++;
                                statefulMap.put("nonThreadSafeCounter", nonThreadSafeCounter);
                                if (i < NUM) {
                                    self.call();
                                } else {
                                    latch.countDown();
                                }
                            }
                        });
                    }
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Count A: " + statefulMap.get("a"));
        System.out.println("Count B: " + statefulMap.get("b"));
        System.out.println("nonThreadSafeCounter: " + statefulMap.get("nonThreadSafeCounter"));

        assertEquals(NUM, statefulMap.get("a").intValue());
        assertEquals(NUM, statefulMap.get("b").intValue());
        assertEquals(NUM, statefulMap.get("nonThreadSafeCounter").intValue());
    }
}
