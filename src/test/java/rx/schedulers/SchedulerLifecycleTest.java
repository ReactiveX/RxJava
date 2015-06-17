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

package rx.schedulers;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;

import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

public class SchedulerLifecycleTest {
    @Test
    public void testShutdown() throws InterruptedException {
        Set<Thread> rxThreads = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreads.add(t);
            }
        }
        Schedulers.shutdown();
        System.out.println("testShutdown >> Giving time to threads to stop");
        Thread.sleep(500);
        
        for (Thread t : rxThreads) {
            assertFalse(t.isAlive());
        }
        
        System.out.println("testShutdown >> Restarting schedulers...");
        Schedulers.start();
        
        final CountDownLatch cdl = new CountDownLatch(3);
        
        Action0 countAction = new Action0() {
            @Override
            public void call() {
                cdl.countDown();
            }
        };
        
        CompositeSubscription csub = new CompositeSubscription();
        
        try {
            Worker w1 = Schedulers.computation().createWorker();
            csub.add(w1);
            w1.schedule(countAction);
            
            Worker w2 = Schedulers.io().createWorker();
            csub.add(w2);
            w2.schedule(countAction);
            
            Worker w3 = Schedulers.newThread().createWorker();
            csub.add(w3);
            w3.schedule(countAction);
            
            if (!cdl.await(3, TimeUnit.SECONDS)) {
                fail("countAction was not run by every worker");
            }
        } finally {
            csub.unsubscribe();
        }
    }
    
    @Test
    public void testStartIdempotence() throws InterruptedException {
        Set<Thread> rxThreads = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreads.add(t);
            }
        }
        System.out.println("testStartIdempotence >> trying to start again");
        Schedulers.start();
        System.out.println("testStartIdempotence >> giving some time");
        Thread.sleep(500);
        
        Set<Thread> rxThreads2 = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreads2.add(t);
            }
        }
        
        assertEquals(rxThreads, rxThreads2);
    }
}
