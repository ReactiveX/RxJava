/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.schedulers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

public class GenericScheduledExecutorServiceTest {
    @Test
    public void verifyInstanceIsSingleThreaded() throws Exception {
        ScheduledExecutorService exec = GenericScheduledExecutorService.getInstance();
        
        final AtomicInteger state = new AtomicInteger();

        final AtomicInteger found1 = new AtomicInteger();
        final AtomicInteger found2 = new AtomicInteger();
        
        Future<?> f1 = exec.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                found1.set(state.getAndSet(1));
            }
        }, 250, TimeUnit.MILLISECONDS);
        Future<?> f2 = exec.schedule(new Runnable() {
            @Override
            public void run() {
                found2.set(state.getAndSet(2));
            }
        }, 250, TimeUnit.MILLISECONDS);
        
        f1.get();
        f2.get();
        
        Assert.assertEquals(2, state.get());
        Assert.assertEquals(0, found1.get());
        Assert.assertEquals(1, found2.get());
    }
}
